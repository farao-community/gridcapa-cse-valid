/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.dichotomy.LimitingElementService;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionReport;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CseValidHandler {
    private final DichotomyRunner dichotomyRunner;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final MinioAdapter minioAdapter;
    private final NetPositionService netPositionService;
    private final LimitingElementService limitingElementService;
    private final Logger businessLogger;

    public CseValidHandler(DichotomyRunner dichotomyRunner, FileImporter fileImporter, FileExporter fileExporter,
                           MinioAdapter minioAdapter, NetPositionService netPositionService,
                           LimitingElementService limitingElementService, Logger businessLogger) {
        this.dichotomyRunner = dichotomyRunner;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
        this.netPositionService = netPositionService;
        this.limitingElementService = limitingElementService;
        this.businessLogger = businessLogger;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        Instant computationStartInstant = Instant.now();
        TcDocumentType tcDocumentType = importTtcAdjustmentFile(cseValidRequest.getTtcAdjustment());
        TcDocumentTypeWriter tcDocumentTypeWriter = new TcDocumentTypeWriter(cseValidRequest);
        if (tcDocumentType != null) {
            TTimestamp timestampData = getTimestampData(cseValidRequest, tcDocumentType);
            if (timestampData != null) {
                computeTimestamp(timestampData, cseValidRequest, tcDocumentTypeWriter);
            } else {
                String ttcAdjTimestamp = formatTimestamp(cseValidRequest.getTime());
                String refCalcTimestamp = formatTimestamp(cseValidRequest.getTimestamp());
                businessLogger.warn("No timestamp available in ttc adjustment for time {} and reference calculation time {}", ttcAdjTimestamp, refCalcTimestamp);
                tcDocumentTypeWriter.fillNoTtcAdjustmentError(cseValidRequest);
            }
        } else {
            tcDocumentTypeWriter.fillNoTtcAdjustmentError(cseValidRequest);
        }
        String ttcValidationUrl = fileExporter.saveTtcValidation(tcDocumentTypeWriter, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
        Instant computationEndInstant = Instant.now();
        return new CseValidResponse(cseValidRequest.getId(), ttcValidationUrl, computationStartInstant, computationEndInstant);
    }

    private TcDocumentType importTtcAdjustmentFile(CseValidFileResource ttcAdjustmentFile) {
        return ttcAdjustmentFile != null ? fileImporter.importTtcAdjustment(ttcAdjustmentFile.getUrl()) : null;
    }

    private TTimestamp getTimestampData(CseValidRequest cseValidRequest, TcDocumentType tcDocumentType) {
        if (tcDocumentType.getAdjustmentResults().get(0) != null) {
            return tcDocumentType.getAdjustmentResults().get(0).getTimestamp().stream()
                    .filter(t -> t.getReferenceCalculationTime().getV().equals(formatTimestamp(cseValidRequest.getTimestamp()))
                            && t.getTime().getV().equals(formatTimestamp(cseValidRequest.getTime())))
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }

    private String formatTimestamp(OffsetDateTime offsetDateTime) {
        // format to "yyy-MM-ddThh:mmZ"
        return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toString();
    }

    void computeTimestamp(TTimestamp timestamp, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        if (missingMniiMnieMiecInTimestamp(timestamp)) {
            tcDocumentTypeWriter.fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
        } else if (multipleMniiMnieMiecInTimestamp(timestamp)) {
            tcDocumentTypeWriter.fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
        } else if (isMniiInTimestamp(timestamp)) {
            computeTimestampForFullImport(timestamp, cseValidRequest, tcDocumentTypeWriter);
        } else if (isMiecInTimestamp(timestamp)) {
            tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestamp, timestamp.getMIEC().getV()); // Temporary filler for export corner, should be replaced
        } else if (isMnieInTimestamp(timestamp)) {
            tcDocumentTypeWriter.fillTimestampFullExportSuccess(timestamp, timestamp.getMNIE().getV());
        } else {
            throw new CseValidInvalidDataException(String.format("Unhandled data for timestamp %s and reference calculation time %s", timestamp.getTime().getV(), timestamp.getReferenceCalculationTime().getV()));
        }
    }

    private void computeTimestampForFullImport(TTimestamp timestamp, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        if (irrelevantValuesInTimestampForFullImport(timestamp)) {
            tcDocumentTypeWriter.fillTimestampFullImportSuccess(timestamp, timestamp.getMNII().getV());
        } else if (missingDataInTimestampForFullImport(timestamp)) {
            tcDocumentTypeWriter.fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
        } else if (actualNtcAboveTarget(timestamp)) {
            BigDecimal mniiValue = timestamp.getMiBNII().getV().subtract(timestamp.getANTCFinal().getV());
            tcDocumentTypeWriter.fillTimestampFullImportSuccess(timestamp, mniiValue);
        } else {
            final boolean isCgmFileAvailable = checkFileAvailability(cseValidRequest.getProcessType(), "CGMs", cseValidRequest::getCgm);
            final boolean isCracFileAvailable = checkFileAvailability(cseValidRequest.getProcessType(), "CRACs", cseValidRequest::getCrac);
            final boolean isGlskFileAvailable = checkFileAvailability(cseValidRequest.getProcessType(), "GLSKs", cseValidRequest::getGlsk);
            final boolean areAllFilesAvailable = isCgmFileAvailable && isCracFileAvailable && isGlskFileAvailable;

            if (!areAllFilesAvailable) {
                businessLogger.error("Missing some input files for timestamp '{}'", timestamp.getTime().getV());
                String redFlagError = errorMessageForMissingFiles(isCgmFileAvailable, isCracFileAvailable, isGlskFileAvailable);
                tcDocumentTypeWriter.fillTimestampError(timestamp, redFlagError);
            } else {
                runDichotomyForFullImport(timestamp, cseValidRequest, tcDocumentTypeWriter);
            }
        }
    }

    private static boolean isMniiInTimestamp(TTimestamp timestamp) {
        return timestamp.getMNII() != null && timestamp.getMNII().getV() != null;
    }

    private static boolean isMnieInTimestamp(TTimestamp timestamp) {
        return timestamp.getMNIE() != null && timestamp.getMNIE().getV() != null;
    }

    private static boolean isMiecInTimestamp(TTimestamp timestamp) {
        return timestamp.getMIEC() != null && timestamp.getMIEC().getV() != null;
    }

    private static boolean missingMniiMnieMiecInTimestamp(TTimestamp timestamp) {
        return !isMniiInTimestamp(timestamp) && !isMnieInTimestamp(timestamp) && !isMiecInTimestamp(timestamp);
    }

    private static boolean multipleMniiMnieMiecInTimestamp(TTimestamp timestamp) {
        // simultaneous presence of at least two values among MNII (full import), MIEC (export-corner) and MNIE (full export)
        return (isMniiInTimestamp(timestamp) && isMnieInTimestamp(timestamp))
                || (isMniiInTimestamp(timestamp) && isMiecInTimestamp(timestamp))
                || (isMnieInTimestamp(timestamp) && isMiecInTimestamp(timestamp));
    }

    private static boolean isMibniiDefined(TTimestamp timestamp) {
        return timestamp.getMiBNII() != null && timestamp.getMiBNII().getV() != null;
    }

    private static int getMibnii(TTimestamp timestamp) {
        return timestamp.getMiBNII().getV().intValue();
    }

    private static boolean isAntcfinalDefined(TTimestamp timestamp) {
        return timestamp.getANTCFinal() != null && timestamp.getANTCFinal().getV() != null;
    }

    private static int getAntcfinal(TTimestamp timestamp) {
        return timestamp.getANTCFinal().getV().intValue();
    }

    private static boolean irrelevantValuesInTimestampForFullImport(TTimestamp timestamp) {
        // MNII is present but both values MiBNII and ANTCFinal are absent or both values are equal to zero
        final boolean mibniiAndAntcfinalAbsent = timestamp.getMiBNII() == null && timestamp.getANTCFinal() == null;
        final boolean mibniiIsZero = isMibniiDefined(timestamp) && getMibnii(timestamp) == 0;
        final boolean antcfinalIsZero = isAntcfinalDefined(timestamp) && getAntcfinal(timestamp) == 0;

        return mibniiAndAntcfinalAbsent || (mibniiIsZero && antcfinalIsZero);
    }

    private static boolean missingDataInTimestampForFullImport(TTimestamp timestamp) {
        // MNII is present but one of the required data (MiBNII or ANTCFinal) is missing
        return !isMibniiDefined(timestamp) || !isAntcfinalDefined(timestamp);
    }

    private boolean actualNtcAboveTarget(TTimestamp timestamp) {
        final int actualNtc = getMibnii(timestamp) - getAntcfinal(timestamp);
        final int targetNtc = timestamp.getMNII().getV().intValue();
        if (actualNtc >= targetNtc) {
            businessLogger.info("Timestamp '{}' NTC has not been augmented by adjustment process, no computation needed.", timestamp.getTime().getV());
            return true;
        }
        businessLogger.info("Timestamp '{}' augmented NTC must be validated.", timestamp.getTime().getV());
        return false;
    }

    private boolean checkFileAvailability(ProcessType processType, String filetype, Supplier<CseValidFileResource> fileSupplier) {
        return fileSupplier.get() != null && minioAdapter.fileExists(buildMinioPath(processType, filetype, fileSupplier.get().getFilename()));
    }

    private static String buildMinioPath(ProcessType processType, String filetype, String filename) {
        return processType.name() + "/" + filetype + "/" + filename;
    }

    private static String errorMessageForMissingFiles(boolean isCgmFileAvailable, boolean isCracFileAvailable, boolean isGlskFileAvailable) {
        StringJoiner stringJoiner = new StringJoiner(", ", "Process fail during TSO validation phase: Missing ", ".");

        if (!isCgmFileAvailable) {
            stringJoiner.add("CGM file");
        }
        if (!isCracFileAvailable) {
            stringJoiner.add("CRAC file");
        }
        if (!isGlskFileAvailable) {
            stringJoiner.add("GLSK file");
        }

        return stringJoiner.toString();
    }

    private void runDichotomyForFullImport(TTimestamp timestamp, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runDichotomy(cseValidRequest, timestamp);
        if (dichotomyResult != null && dichotomyResult.hasValidStep()) {
            TLimitingElement tLimitingElement = this.limitingElementService.getLimitingElement(dichotomyResult.getHighestValidStep());
            BigDecimal mibniiValue = timestamp.getMiBNII().getV().subtract(timestamp.getANTCFinal().getV());
            BigDecimal mniiValue = computeMnii(dichotomyResult).map(Math::round).map(BigDecimal::valueOf).orElse(mibniiValue);
            tcDocumentTypeWriter.fillTimestampWithDichotomyResponse(timestamp, mibniiValue, mniiValue, tLimitingElement);
        } else {
            tcDocumentTypeWriter.fillDichotomyError(timestamp);
        }
    }

    private Optional<Double> computeMnii(DichotomyResult<RaoResponse> dichotomyResult) {
        if (dichotomyResult.getHighestValidStep() == null) {
            return Optional.empty();
        }
        String finalNetworkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getNetworkWithPraFileUrl();
        NetPositionReport netPositionReport = netPositionService.generateNetPositionReport(finalNetworkWithPraUrl);
        Map<String, Double> italianBordersExchange = netPositionReport.getAreasReport().get("IT").getBordersExchanges();
        double italianCseNetPosition = italianBordersExchange.get("FR") +
                italianBordersExchange.get("CH") +
                italianBordersExchange.get("AT") +
                italianBordersExchange.get("SI");
        return Optional.of(-italianCseNetPosition);
    }
}

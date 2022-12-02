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
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.apache.commons.lang3.NotImplementedException;
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
import java.util.stream.Collectors;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_SHIFTING_FACTORS;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_CALCULATION_DIRECTIONS;

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
                TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestampData);
                computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
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

    void computeTimestamp(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        if (timestampWrapper.hasNoneOfMniiMnieMiec()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_DATA);
        } else if (timestampWrapper.hasMultipleMniiMnieMiec()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_CONTRADICTORY_DATA);
        } else if (timestampWrapper.hasMnii()) {
            computeTimestampForFullImport(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        } else if (timestampWrapper.hasMiec()) {
            computeTimestampForExportCorner(timestampWrapper, tcDocumentTypeWriter);
        } else if (timestampWrapper.hasMnie()) {
            tcDocumentTypeWriter.fillTimestampFullExportSuccess(timestampWrapper.getTimestamp(), timestampWrapper.getMnieValue());
        } else {
            throw new CseValidInvalidDataException(String.format("Unhandled data for timestamp %s and reference calculation time %s", timestampWrapper.getTimeValue(), timestampWrapper.getReferenceCalculationTimeValue()));
        }
    }

    private void computeTimestampForFullImport(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        if (irrelevantValuesInTimestampForFullImport(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampFullImportSuccess(timestampWrapper.getTimestamp(), timestampWrapper.getMniiValue());
        } else if (missingDataInTimestampForFullImport(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_DATA);
        } else if (actualNtcAboveTargetForFullImport(timestampWrapper)) {
            BigDecimal mniiValue = timestampWrapper.getMibniiValue().subtract(timestampWrapper.getAntcfinalValue());
            tcDocumentTypeWriter.fillTimestampFullImportSuccess(timestampWrapper.getTimestamp(), mniiValue);
        } else {
            final boolean isCgmFileAvailable = checkFileAvailability(cseValidRequest.getProcessType(), "CGMs", cseValidRequest::getCgm);
            final boolean isCracFileAvailable = checkFileAvailability(cseValidRequest.getProcessType(), "CRACs", cseValidRequest::getImportCrac);
            final boolean isGlskFileAvailable = checkFileAvailability(cseValidRequest.getProcessType(), "GLSKs", cseValidRequest::getGlsk);
            final boolean areAllFilesAvailable = isCgmFileAvailable && isCracFileAvailable && isGlskFileAvailable;

            if (!areAllFilesAvailable) {
                businessLogger.error("Missing some input files for timestamp '{}'", timestampWrapper.getTimeValue());
                String redFlagError = errorMessageForMissingFiles(isCgmFileAvailable, isCracFileAvailable, isGlskFileAvailable);
                tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), redFlagError);
            } else {
                runDichotomyForFullImport(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
            }
        }
    }

    private void computeTimestampForExportCorner(TTimestampWrapper timestampWrapper, TcDocumentTypeWriter tcDocumentTypeWriter) {
        TTimestamp timestamp = timestampWrapper.getTimestamp();
        if (irrelevantValuesInTimestampForExportCorner(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestamp, timestampWrapper.getMiecValue());
        } else if (missingDataInTimestampForExportCorner(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_DATA);
        } else if (timestampWrapper.hasShiftingFactors()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_SHIFTING_FACTORS);
        } else if (timestampWrapper.hasCalculationDirections()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_CALCULATION_DIRECTIONS);
        } else if (actualNtcAboveTargetForExportCorner(timestampWrapper)) {
            BigDecimal miecValue = timestampWrapper.getMibiecValue().subtract(timestampWrapper.getAntcfinalValue());
            tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestampWrapper.getTimestamp(), miecValue);
        } else {
            int italianImportAfterCep70Adjustment = timestampWrapper.getMiecIntValue();
            int maxItalianSecureImport = timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue();
            timestampWrapper.getTimeValue();
            timestampWrapper.getReferenceCalculationTimeValue();
            timestamp.getLimitingElement();
            timestamp.getTTCLimitedBy();
            timestamp.getCGMfile();
            timestamp.getGSKfile();
            timestamp.getCRACfile();
            timestamp.getBASECASEfile();
            timestamp.getCalculationDirections().getCalculationDirection().stream()
                    .map(TCalculationDirection::getInArea)
                    .collect(Collectors.toList());
            timestamp.getShiftingFactors().getShiftingFactor().stream()
                    .filter(f -> f.getCountry().getV().equals("FR"))
                    .map(TFactor::getFactor)
                    .findFirst().orElseThrow();

            // TODO Case 005 : To be completed
            throw new NotImplementedException();
        }
    }

    private static boolean irrelevantValuesInTimestampForFullImport(TTimestampWrapper timestampWrapper) {
        // MNII is present but both values MiBNII and ANTCFinal are absent or both values are equal to zero
        final boolean mibniiAndAntcfinalAbsent = timestampWrapper.getMibnii() == null && timestampWrapper.getAntcfinal() == null;
        final boolean mibniiIsZero = timestampWrapper.hasMibnii() && timestampWrapper.getMibniiIntValue() == 0;
        final boolean antcfinalIsZero = timestampWrapper.hasAntcfinal() && timestampWrapper.getAntcfinalIntValue() == 0;

        return mibniiAndAntcfinalAbsent || (mibniiIsZero && antcfinalIsZero);
    }

    private static boolean irrelevantValuesInTimestampForExportCorner(TTimestampWrapper timestampWrapper) {
        // MIEC is present but both values MiBIEC and ANTCFinal are absent or both values are equal to zero
        final boolean mibiecAndAntcfinalAbsent = timestampWrapper.getMibiec() == null && timestampWrapper.getAntcfinal() == null;
        final boolean mibiecIsZero = timestampWrapper.hasMibiec() && timestampWrapper.getMibiecIntValue() == 0;
        final boolean antcfinalIsZero = timestampWrapper.hasAntcfinal() && timestampWrapper.getAntcfinalIntValue() == 0;

        return mibiecAndAntcfinalAbsent || (mibiecIsZero && antcfinalIsZero);
    }

    private static boolean missingDataInTimestampForFullImport(TTimestampWrapper timestampWrapper) {
        // MNII is present but one of the required data (MiBNII or ANTCFinal) is missing
        return !timestampWrapper.hasMibnii() || !timestampWrapper.hasAntcfinal();
    }

    private static boolean missingDataInTimestampForExportCorner(TTimestampWrapper timestampWrapper) {
        // MIEC is present but one of the required data (MiBIEC or ANTCFinal) is missing
        return !timestampWrapper.hasMibiec() || !timestampWrapper.hasAntcfinal();
    }

    private boolean actualNtcAboveTargetForFullImport(TTimestampWrapper timestampWrapper) {
        final int actualNtc = timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue();
        final int targetNtc = timestampWrapper.getMniiIntValue();
        return actualNtcAboveTargetNtc(timestampWrapper, actualNtc, targetNtc);
    }

    private boolean actualNtcAboveTargetForExportCorner(TTimestampWrapper timestampWrapper) {
        final int actualNtc = timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue();
        final int targetNtc = timestampWrapper.getMiecIntValue();
        return actualNtcAboveTargetNtc(timestampWrapper, actualNtc, targetNtc);
    }

    private boolean actualNtcAboveTargetNtc(TTimestampWrapper timestampWrapper, int actualNtc, int targetNtc) {
        if (actualNtc >= targetNtc) {
            businessLogger.info("Timestamp '{}' NTC has not been augmented by adjustment process, no computation needed.", timestampWrapper.getTimestamp().getTime().getV());
            return true;
        }
        businessLogger.info("Timestamp '{}' augmented NTC must be validated.", timestampWrapper.getTimestamp().getTime().getV());
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

    private void runDichotomyForFullImport(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runDichotomy(cseValidRequest, timestampWrapper.getTimestamp());
        if (dichotomyResult != null && dichotomyResult.hasValidStep()) {
            TLimitingElement tLimitingElement = this.limitingElementService.getLimitingElement(dichotomyResult.getHighestValidStep());
            BigDecimal mibniiValue = timestampWrapper.getMibniiValue().subtract(timestampWrapper.getAntcfinalValue());
            BigDecimal mniiValue = computeMnii(dichotomyResult).map(Math::round).map(BigDecimal::valueOf).orElse(mibniiValue);
            tcDocumentTypeWriter.fillTimestampWithDichotomyResponse(timestampWrapper.getTimestamp(), mibniiValue, mniiValue, tLimitingElement);
        } else {
            tcDocumentTypeWriter.fillDichotomyError(timestampWrapper.getTimestamp());
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

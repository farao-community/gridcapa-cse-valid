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
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.StringJoiner;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CseValidHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidHandler.class);
    private final DichotomyRunner dichotomyRunner;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final MinioAdapter minioAdapter;
    private TcDocumentTypeWriter tcDocumentTypeWriter;
    private TimestampStatus timestampStatus;
    private boolean isCracFileAvailable = false;
    private boolean isCgmFileAvailable = false;
    private boolean isGlskFileAvailable = false;
    private final NetPositionService netPositionService;
    private final LimitingElementService limitingElementService;

    public CseValidHandler(DichotomyRunner dichotomyRunner, FileImporter fileImporter, FileExporter fileExporter, MinioAdapter minioAdapter, NetPositionService netPositionService, LimitingElementService limitingElementService) {
        this.dichotomyRunner = dichotomyRunner;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
        this.netPositionService = netPositionService;
        this.limitingElementService = limitingElementService;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        Instant computationStartInstant = Instant.now();
        TcDocumentType tcDocumentType = importTtcAdjustmentFile(cseValidRequest.getTtcAdjustment());
        tcDocumentTypeWriter = new TcDocumentTypeWriter(cseValidRequest, netPositionService);
        if (tcDocumentType != null) {
            TTimestamp timestampData = getTimestampData(cseValidRequest, tcDocumentType);
            if (timestampData != null) {
                timestampStatus = getTimestampStatus(timestampData, cseValidRequest);
                computeTimestamp(cseValidRequest, timestampData);
            } else {
                LOGGER.warn("No timestamp available in ttc adjustment for time {} and reference calculation time {}", formatTimestamp(cseValidRequest.getTime()), formatTimestamp(cseValidRequest.getTimestamp()));
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

    private void computeTimestamp(CseValidRequest cseValidRequest, TTimestamp timestamp) {
        switch (timestampStatus) {
            case MISSING_DATAS:
                tcDocumentTypeWriter.fillTimestampWithMissingInputFiles(timestamp, "Process fail during TSO validation phase: Missing datas.");
                break;
            case NO_COMPUTATION_NEEDED:
                tcDocumentTypeWriter.fillTimestampNoComputationNeeded(timestamp);
                break;
            case NO_VERIFICATION_NEEDED:
                tcDocumentTypeWriter.fillTimestampNoVerificationNeeded(timestamp);
                break;
            case MISSING_INPUT_FILES:
                String redFlagError = redFlagReasonError(isCgmFileAvailable, isCracFileAvailable, isGlskFileAvailable);
                tcDocumentTypeWriter.fillTimestampWithMissingInputFiles(timestamp, redFlagError);
                break;
            case COMPUTATION_NEEDED:
                DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runDichotomy(cseValidRequest, timestamp);
                if (dichotomyResult != null && dichotomyResult.hasValidStep()) {
                    TLimitingElement tLimitingElement = this.limitingElementService.getLimitingElement(dichotomyResult.getHighestValidStep());
                    tcDocumentTypeWriter.fillTimestampWithDichotomyResponse(timestamp, dichotomyResult, tLimitingElement);
                } else {
                    tcDocumentTypeWriter.fillDichotomyError(timestamp);
                }
                break;
            default:
                throw new CseValidInvalidDataException("Timestamp Status not supported");
        }
    }

    private TimestampStatus getTimestampStatus(TTimestamp timestamp, CseValidRequest cseValidRequest) {
        if (valuesAreNotRevelant(timestamp)) {
            return TimestampStatus.NO_VERIFICATION_NEEDED;
        } else if (datasAbsentInTimestamp(timestamp)) {
            return TimestampStatus.MISSING_DATAS;
        } else if (actualMaxImportAugmented(timestamp)) {
            return TimestampStatus.NO_COMPUTATION_NEEDED;
        } else if (!areFilesPresent(timestamp, cseValidRequest)) {
            return TimestampStatus.MISSING_INPUT_FILES;
        } else {
            return TimestampStatus.COMPUTATION_NEEDED;
        }
    }

    private boolean datasAbsentInTimestamp(TTimestamp timestamp) {
        return (timestamp.getMNII() == null || timestamp.getMNII().getV() == null) ||
                ((timestamp.getMiBNII() == null || timestamp.getANTCFinal() == null) || (timestamp.getMiBNII().getV() == null || timestamp.getANTCFinal().getV() == null));
    }

    private boolean actualMaxImportAugmented(TTimestamp timestamp) {
        int mibniiMinusAntc = timestamp.getMiBNII().getV().intValue() - timestamp.getANTCFinal().getV().intValue();
        int mnii = timestamp.getMNII().getV().intValue();
        if (mibniiMinusAntc >= mnii) {
            LOGGER.info("Timestamp '{}' NTC has not been augmented by adjustment process, no computation needed.", timestamp.getTime().getV());
            return true;
        }
        LOGGER.info("Timestamp '{}' augmented NTC must be validated.", timestamp.getTime().getV());
        return false;
    }

    private boolean valuesAreNotRevelant(TTimestamp ts) {
        // case both values are absent or both values are equal to zero
        return (ts.getMiBNII() == null && ts.getANTCFinal() == null) ||
                ((ts.getMiBNII() != null && ts.getMiBNII().getV() != null && ts.getMiBNII().getV().intValue() == 0)
                        && (ts.getANTCFinal() != null && ts.getANTCFinal().getV() != null && ts.getANTCFinal().getV().intValue() == 0));
    }

    private boolean areFilesPresent(TTimestamp timestamp, CseValidRequest cseValidRequest) {
        isCgmFileAvailable = cseValidRequest.getCgm() != null && minioAdapter.fileExists(buildMinioPath(cseValidRequest.getProcessType(), "CGMs", cseValidRequest.getCgm().getFilename()));
        isCracFileAvailable = cseValidRequest.getCrac() != null && minioAdapter.fileExists(buildMinioPath(cseValidRequest.getProcessType(), "CRACs", cseValidRequest.getCrac().getFilename()));
        isGlskFileAvailable = cseValidRequest.getGlsk() != null && minioAdapter.fileExists(buildMinioPath(cseValidRequest.getProcessType(), "GLSKs", cseValidRequest.getGlsk().getFilename()));

        if (!isCgmFileAvailable || !isCracFileAvailable || !isGlskFileAvailable) {
            LOGGER.error("Missing some input files for timestamp '{}'", timestamp.getTime().getV());
            return false;
        }
        return true;
    }

    private String buildMinioPath(ProcessType processType, String filetype, String filename) {
        return processType.name() + "/" + filetype + "/" + filename;
    }

    private String redFlagReasonError(boolean isCgmFileAvailable, boolean isCracFileAvailable, boolean isGlskFileAvailable) {
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

}

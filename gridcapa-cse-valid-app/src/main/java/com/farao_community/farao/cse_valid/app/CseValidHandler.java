/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private boolean isCracFileAvailable = true; // todo check by default true or false ?
    private boolean isCgmFileAvailable = true;
    private boolean isGlskFileAvailable = true;
    private final NetPositionService netPositionService;

    public CseValidHandler(DichotomyRunner dichotomyRunner, FileImporter fileImporter, FileExporter fileExporter, MinioAdapter minioAdapter, NetPositionService netPositionService) {
        this.dichotomyRunner = dichotomyRunner;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
        this.netPositionService = netPositionService;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        Instant computationStartInstant = Instant.now();
        TcDocumentType tcDocumentType = fileImporter.importTtcAdjustment(cseValidRequest.getTtcAdjustment().getUrl());
        tcDocumentTypeWriter = new TcDocumentTypeWriter(cseValidRequest, netPositionService);
        if (tcDocumentType != null) {
            TTimestamp timestampData = getTimestampData(cseValidRequest, tcDocumentType);
            if (timestampData != null) {
                timestampStatus = getTimestampStatus(timestampData, cseValidRequest.getProcessType());
                computeTimestamp(cseValidRequest, timestampData);
            } else {
                // todo fill with no ttc adjustment error
            }
        } else {
        }
        String ttcValidationUrl = fileExporter.saveTtcValidation(tcDocumentTypeWriter, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
        Instant computationEndInstant = Instant.now();
        return new CseValidResponse(cseValidRequest.getId(), ttcValidationUrl, computationStartInstant, computationEndInstant);
    }

    private TTimestamp getTimestampData(CseValidRequest cseValidRequest, TcDocumentType tcDocumentType) {
        String requestTs = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(cseValidRequest.getTimestamp().withNano(0));
        return tcDocumentType.getAdjustmentResults().get(0).getTimestamp().stream()
                .filter(t -> formatTimestampTime(t.getTime()).equals(requestTs))
                .findFirst()
                .orElse(null);
    }

    private String formatTimestampTime(TTime time) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.parse(time.getV(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'")));
    }

    private void computeTimestamp(CseValidRequest cseValidRequest, TTimestamp tTimestamp) {
        switch (timestampStatus) {
            case MISSING_DATAS:
                tcDocumentTypeWriter.fillTimestampWithMissingInputFiles(tTimestamp, "Process fail during TSO validation phase: Missing datas.");
                break;
            case NO_COMPUTATION_NEEDED:
                tcDocumentTypeWriter.fillTimestampNoComputationNeeded(tTimestamp);
                break;
            case MISSING_INPUT_FILES:
                String redFlagError = redFlagReasonError(isCgmFileAvailable, isCracFileAvailable, isGlskFileAvailable);
                tcDocumentTypeWriter.fillTimestampWithMissingInputFiles(tTimestamp, redFlagError);
                break;
            case COMPUTATION_NEEDED:
                DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runDichotomy(cseValidRequest, tTimestamp);
                String finalCgmUrl;
                if (dichotomyResult.hasValidStep()) { //todo no need to fiter here??
                    //String finalCgmPath = fileExporter.getFinalNetworkFilePath(cseValidRequest.getTimestamp(), cseValidRequest.getProcessType()); todo if necessary to add cgm to the output directory
                    Network finalNetwork = fileImporter.importNetwork(dichotomyResult.getHighestValidStep().getValidationData().getNetworkWithPraFileUrl());
                    //finalCgmUrl = fileExporter.exportAndUploadNetwork(finalNetwork, "UCTE", GridcapaFileGroup.OUTPUT, finalCgmPath, processConfiguration.getFinalCgm(), cseRequest.getTargetProcessDateTime(), cseRequest.getProcessType());
                    tcDocumentTypeWriter.fillTimestampWithDichotomyResponse(tTimestamp, dichotomyResult);
                } else {
                    //tcDocumentTypeWriter.fillWithError(tTimestamp); todo case failure dichotomy
                }
                break;
            default:
                throw new CseValidInvalidDataException("Timestamp Status not supported");
        }
    }

    private TimestampStatus getTimestampStatus(TTimestamp timestamp, ProcessType processType) {
        if (datasAbsentInTimestamp(timestamp)) {
            return TimestampStatus.MISSING_DATAS;
        } else if (actualMaxImportAugmented(timestamp)) {
            return TimestampStatus.NO_COMPUTATION_NEEDED;
        } else if (!areFilesPresent(timestamp, processType)) {
            return TimestampStatus.MISSING_INPUT_FILES;
        } else {
            return TimestampStatus.COMPUTATION_NEEDED;
        }
    }

    private boolean datasAbsentInTimestamp(TTimestamp timestamp) {
        if (timestamp.getMNII() == null || timestamp.getMiBNII() == null || timestamp.getANTCFinal() == null
                || (timestamp.getMiBNII().getV().intValue() == 0 && timestamp.getANTCFinal().getV().intValue() == 0)
                || timestamp.getMNII().getV() == null || timestamp.getMiBNII().getV() == null || timestamp.getANTCFinal().getV() == null) {
            LOGGER.info("Missing datas in TTC Adjustment");
            return true;
        }
        return false;
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

    private boolean areFilesPresent(TTimestamp timestamp, ProcessType processType) { // todo how to configure this in case of automatic run?
        isCgmFileAvailable = timestamp.getCGMfile() != null && minioAdapter.fileExists(processType + "/CGMs/" + timestamp.getCGMfile().getV());
        //boolean isCracFileAvailable = timestamp.getCRACfile() != null && minioAdapter.fileExists(processType + "/CRACs/" + timestamp.getCRACfile().getV());
        isCracFileAvailable = true; // todo the crac used should be the crac of the request "_FR"
        isGlskFileAvailable = timestamp.getGSKfile() != null && minioAdapter.fileExists(processType + "/GLSKs/" + timestamp.getGSKfile().getV());

        if (!isCgmFileAvailable || !isCracFileAvailable || !isGlskFileAvailable) {
            LOGGER.error("Missing some input files for timestamp '{}'", timestamp.getTime().getV());

            return false;
        }
        return true;
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

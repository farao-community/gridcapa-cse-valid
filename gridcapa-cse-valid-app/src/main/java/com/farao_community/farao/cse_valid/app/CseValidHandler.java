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
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CseValidHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidHandler.class);
    private final DichotomyRunner dichotomyRunner;
    private final FileImporter fileImporter;
    private final FileImporter fileExporter;
    private final MinioAdapter minioAdapter;
    private CseValidRequest cseValidRequest;
    private TcDocumentType tcDocumentType;
    private TcDocumentTypeWriter tcDocumentTypeWriter;

    public CseValidHandler(DichotomyRunner dichotomyRunner, FileImporter fileImporter, FileImporter fileExporter, MinioAdapter minioAdapter) {
        this.dichotomyRunner = dichotomyRunner;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        this.cseValidRequest = cseValidRequest;
        this.tcDocumentType = importTTcAdjustmentFile(cseValidRequest);
        if (tcDocumentType != null) {
            computeEveryTimestamp(tcDocumentType, cseValidRequest.getProcessType()); //todo need to check the timestamp.get Time and the request timestamp?
        } else {
            // todo fill with no ttc adjustment error
        }
        return new CseValidResponse(cseValidRequest.getId());
    }

    public TcDocumentType importTTcAdjustmentFile(CseValidRequest cseValidRequest) {
        String url = fileImporter.buildTtcFileUrl(cseValidRequest);
        InputStream minioObject = minioAdapter.getFile(url);
        return fileImporter.importTtcAdjustment(minioObject);
    }

    private void computeEveryTimestamp(TcDocumentType tcDocumentType, ProcessType processType) {
        this.tcDocumentTypeWriter = new TcDocumentTypeWriter();
        for (TTimestamp timestamp : tcDocumentType.getAdjustmentResults().get(0).getTimestamp()) { //todo filtre le timettamp correspondant a la request
            //todo check les fichiers dans ttc adjustment sont les meme sque la request sinon quoi??
            TimestampStatus timestampStatus = isComputationNeeded(timestamp, processType);
            switch (timestampStatus) {
                case MISSING_DATAS:
                case NO_COMPUTATION_NEEDED:
                case MISSING_INPUT_FILES:
                    tcDocumentTypeWriter.writeOneTimestamp(timestamp, timestampStatus);
                    break;
                case COMPUTATION_NEEDED:
                    DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runDichotomy(cseValidRequest, timestamp);
                    String finalCgmUrl;
                    if (dichotomyResult.hasValidStep()) {
                        /*String finalCgmPath = fileExporter.getFinalNetworkFilePath(cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
                        // todo complete
                        */
                    } else {
                        //todo do something
                    }
                    break;
                default:
                    throw new CseValidInvalidDataException("Timestamp Status not supported");
            }
        }
    }

    private TimestampStatus isComputationNeeded(TTimestamp timestamp, ProcessType processType) {
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
                || (timestamp.getMiBNII().getV().intValue() == 0 && timestamp.getANTCFinal().getV().intValue() == 0)) {
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

    private boolean areFilesPresent(TTimestamp timestamp, ProcessType processType) {
        boolean isCgmFileAvailable = timestamp.getCGMfile() != null && minioAdapter.fileExists(processType + "/CGMs/" + timestamp.getCGMfile().getV());
        //boolean isCracFileAvailable = timestamp.getCRACfile() != null && minioAdapter.fileExists(processType + "/CRACs/" + timestamp.getCRACfile().getV());
        boolean isCracFileAvailable = true; //todo the crac used should be the crac of the request "_FR"
        boolean isGlskFileAvailable = timestamp.getGSKfile() != null && minioAdapter.fileExists(processType + "/GLSKs/" + timestamp.getGSKfile().getV());

        if (!isCgmFileAvailable || !isCracFileAvailable || !isGlskFileAvailable) {
            LOGGER.error("Missing some input files for timestamp '{}'", timestamp.getTime().getV());
            return false;
        }
        return true;
    }

    public TcDocumentType getTcDocumentType() {
        return tcDocumentType;
    }
}

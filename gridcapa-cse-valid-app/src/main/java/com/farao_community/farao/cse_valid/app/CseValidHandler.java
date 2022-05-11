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
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.TTimestamp;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.TcDocumentType;
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
    private final MinioAdapter minioAdapter;
    private TcDocumentType tcDocumentType;
    private TcDocumentTypeWriter tcDocumentTypeWriter;

    public CseValidHandler(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        this.tcDocumentType = importTTcAdjustmentFile(cseValidRequest);
        if (tcDocumentType != null) {
            computeEveryTimestamp(tcDocumentType);
        } else {
            // fill with no ttc adjustment error
        }
        return new CseValidResponse(cseValidRequest.getId());
    }

    private TcDocumentType importTTcAdjustmentFile(CseValidRequest cseValidRequest) {
        String url = buildTtcAdjustmentFileUrl(cseValidRequest);
        InputStream minioObject = getInputStreamOfMinioObject(url);
        return FileImporter.importTtcAdjustment(minioObject);
    }

    private String buildTtcAdjustmentFileUrl(CseValidRequest cseValidRequest) {
        return minioAdapter.getProperties().getBasePath() +
                cseValidRequest.getProcessType().toString() +
                "/TTC_ADJUSTMENT/" +
                cseValidRequest.getTtcAdjustment().getFilename();
    }

    private InputStream getInputStreamOfMinioObject(String url) {
        try {
            return minioAdapter.getFile(url);
        } catch (Exception e) {
            LOGGER.error("Can not import TTC Adjustment file : {}", url);
        }
        return null;
    }

    private void computeEveryTimestamp(TcDocumentType tcDocumentType) {
        this.tcDocumentTypeWriter = new TcDocumentTypeWriter();
        for (TTimestamp timestamp : tcDocumentType.getAdjustmentResults().get(0).getTimestamp()) {
            TimestampStatus timestampStatus = isComputationNeeded(timestamp);
            switch (timestampStatus) {
                case MISSING_DATAS:
                case NO_COMPUTATION_NEEDED:
                case MISSING_INPUT_FILES:
                    tcDocumentTypeWriter.writeOneTimestamp(timestamp, timestampStatus);
                    break;
                case COMPUTATION_NEEDED:
                    test();
                    break;
                default:
                    throw new CseValidInvalidDataException("Timestamp Status not supported");
            }
        }
    }

    private void test() {
    }

    private TimestampStatus isComputationNeeded(TTimestamp timestamp) {
        if (datasAbsentInTimestamp(timestamp)) {
            return TimestampStatus.MISSING_DATAS;
        } else if (actualMaxImportAugmented(timestamp)) {
            return TimestampStatus.NO_COMPUTATION_NEEDED;
        } else if (areFilesPresent(timestamp)) {
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

    private boolean areFilesPresent(TTimestamp timestamp) {
        boolean isCgmFileAvailable = timestamp.getCGMfile() != null && minioAdapter.fileExists("CGMs/" + timestamp.getCGMfile().getV());
        boolean isCracFileAvailable = timestamp.getCRACfile() != null && minioAdapter.fileExists("CRACs/" + timestamp.getCRACfile().getV());
        boolean isGlskFileAvailable = timestamp.getGSKfile() != null && minioAdapter.fileExists("GLSKs/" + timestamp.getGSKfile().getV());

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

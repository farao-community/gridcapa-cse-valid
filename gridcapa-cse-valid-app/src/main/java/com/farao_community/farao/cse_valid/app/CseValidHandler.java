/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.app.configuration.MinioAdapter;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.TcDocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CseValidHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidHandler.class);
    private final MinioAdapter minioAdapter;

    public CseValidHandler(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        TcDocumentType tcDocumentType = importTTcAdjustmentFile(cseValidRequest);
        return new CseValidResponse(cseValidRequest.getId());
    }

    private TcDocumentType importTTcAdjustmentFile(CseValidRequest cseValidRequest) {
        String url = buildTtcAdjustmentFileUrl(cseValidRequest);
        Optional<InputStream> minioObject = getOptionalMinioObject(url);
        return minioObject.map(FileImporter::importTtcAdjustment).orElse(null);
    }

    private String buildTtcAdjustmentFileUrl(CseValidRequest cseValidRequest) {
        return minioAdapter.getBasePath() +
                cseValidRequest.getProcessType().toString() +
                "/TTC_ADJUSTMENT/" +
                cseValidRequest.getTtcAdjustment().getFilename();
    }

    private Optional<InputStream> getOptionalMinioObject(String url) {
        try {
            return minioAdapter.getMinioObject(url);
        } catch (Exception e) {
            LOGGER.error("Can not import TTC Adjustment file : {}", url);
        }
        return Optional.empty();
    }
}

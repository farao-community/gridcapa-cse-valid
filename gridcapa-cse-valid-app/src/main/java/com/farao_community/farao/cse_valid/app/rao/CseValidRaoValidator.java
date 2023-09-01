/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.rao;

import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
public class CseValidRaoValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidRaoValidator.class);

    private final FileImporter fileImporter;
    private final RaoRunnerClient raoRunnerClient;

    public CseValidRaoValidator(FileImporter fileImporter, RaoRunnerClient raoRunnerClient) {
        this.fileImporter = fileImporter;
        this.raoRunnerClient = raoRunnerClient;
    }

    public RaoResponse runRao(String requestId, String networkFiledUrl, String jsonCracUrl, String raoParametersURL, String resultsDestination) {
        RaoRequest raoRequest = new RaoRequest(requestId, networkFiledUrl, jsonCracUrl, raoParametersURL, resultsDestination);

        LOGGER.info("RAO request sent: {}", raoRequest);
        RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
        LOGGER.info("RAO response received: {}", raoResponse);

        return raoResponse;
    }

    public boolean isSecure(RaoResponse raoResponse) {
        String raoResultUrl = raoResponse.getRaoResultFileUrl();
        Crac crac = fileImporter.importCracFromJson(raoResponse.getCracFileUrl());
        RaoResult raoResult = fileImporter.importRaoResult(raoResultUrl, crac);

        return raoResult.getFunctionalCost(OptimizationState.AFTER_CRA) <= 0.0;
    }
}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.rao;

import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
public class CseValidRaoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidRaoRunner.class);

    private final FileImporter fileImporter;
    private final RaoRunnerClient raoRunnerClient;

    public CseValidRaoRunner(FileImporter fileImporter, RaoRunnerClient raoRunnerClient) {
        this.fileImporter = fileImporter;
        this.raoRunnerClient = raoRunnerClient;
    }

    public AbstractRaoResponse runRao(String requestId, String runId, String networkFiledUrl, String jsonCracUrl, String raoParametersURL, String resultsDestination) {
        RaoRequest raoRequest = new RaoRequest.RaoRequestBuilder()
                .withId(requestId)
                .withRunId(runId)
                .withNetworkFileUrl(networkFiledUrl)
                .withCracFileUrl(jsonCracUrl)
                .withRaoParametersFileUrl(raoParametersURL)
                .withResultsDestination(resultsDestination)
                .build();

        LOGGER.info("RAO request sent: {}", raoRequest);
        AbstractRaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
        LOGGER.info("RAO response received: {}", raoResponse);

        return raoResponse;
    }

    public boolean isSecure(AbstractRaoResponse raoResponse, Network network) {
        if (raoResponse.isRaoFailed()) {
            return false;
        }
        RaoSuccessResponse successResponse = (RaoSuccessResponse) raoResponse;
        String raoResultUrl = successResponse.getRaoResultFileUrl();
        Crac crac = fileImporter.importCracFromJson(successResponse.getCracFileUrl(), network);
        RaoResult raoResult = fileImporter.importRaoResult(raoResultUrl, crac);

        return raoResult.isSecure();
    }
}

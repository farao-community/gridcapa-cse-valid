/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.powsybl.iidm.network.Network;

import java.io.IOException;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class RaoValidator implements NetworkValidator<RaoResponse> {

    private final String requestId;
    private final String networkUrl;
    private final String cracUrl;
    private final RaoRunnerClient raoRunnerClient;
    private final FileImporter fileImporter;

    public RaoValidator(CseValidRequest cseValidRequest, RaoRunnerClient raoRunnerClient, FileImporter fileImporter) {
        this.requestId = cseValidRequest.getId();
        this.networkUrl = cseValidRequest.getCgm().getUrl();
        this.cracUrl = cseValidRequest.getCrac().getUrl();
        this.raoRunnerClient = raoRunnerClient;
        this.fileImporter = fileImporter;
    }

    @Override
    public DichotomyStepResult<RaoResponse> validateNetwork(Network network) throws ValidationException {
        RaoRequest raoRequest = buildRaoRequest();
        try {
            RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), fileImporter.importCracFromJson(raoResponse.getCracFileUrl()));
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, raoResponse);
        } catch (RuntimeException | IOException e) {
            throw new ValidationException("RAO run failed. Nested exception: " + e.getMessage());
        }
    }

    private RaoRequest buildRaoRequest() {
        return new RaoRequest(requestId, networkUrl, cracUrl, getRaoParameterURL());
    }

    private String getRaoParameterURL() {
        RaoParameters raoParameters = getRaoParameters();
        return fileImporter.saveRaoParametersAndGetUrl(raoParameters);
    }

    private RaoParameters getRaoParameters() {
        RaoParameters raoParameters = RaoParameters.load();
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        return raoParameters;
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.rao;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.dichotomy.RaoValidator;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Component;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
public class CseValidRaoValidator {

    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final RaoRunnerClient raoRunnerClient;

    public CseValidRaoValidator(FileImporter fileImporter, FileExporter fileExporter, RaoRunnerClient raoRunnerClient) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.raoRunnerClient = raoRunnerClient;
    }

    public String getJsonCracUrl(CseValidRequest cseValidRequest, Network network, String cracUrl) {
        CseCrac cseCrac = fileImporter.importCseCrac(cracUrl);
        Crac crac = fileImporter.importCrac(cseCrac, cseValidRequest.getTimestamp(), network);
        return fileExporter.saveCracInJsonFormat(crac, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
    }

    public NetworkValidator<RaoResponse> getNetworkValidator(CseValidRequest cseValidRequest, String jsonCracUrl) {
        String raoParametersURL = fileExporter.saveRaoParameters(cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
        return new RaoValidator(
                cseValidRequest.getProcessType(),
                cseValidRequest.getId(),
                cseValidRequest.getTimestamp(),
                jsonCracUrl,
                raoParametersURL,
                raoRunnerClient,
                fileImporter,
                fileExporter);
    }

    public boolean isNetworkSecure(Network network, CseValidRequest cseValidRequest, String cracUrl) {
        String jsonCracUrl = getJsonCracUrl(cseValidRequest, network, cracUrl);
        NetworkValidator<RaoResponse> networkValidator = getNetworkValidator(cseValidRequest, jsonCracUrl);
        try {
            DichotomyStepResult<RaoResponse> result = networkValidator.validateNetwork(network);
            return result.isValid();
        } catch (ValidationException e) {
            return false;
        }
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.rao;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
public class CseValidRaoValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidRaoValidator.class);

    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final RaoRunnerClient raoRunnerClient;
    private int variantCounter = 0;

    public CseValidRaoValidator(FileImporter fileImporter, FileExporter fileExporter, RaoRunnerClient raoRunnerClient) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.raoRunnerClient = raoRunnerClient;
    }

    public boolean isNetworkSecure(Network network, CseValidRequest cseValidRequest, String cracUrl) {
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();
        String requestId = cseValidRequest.getId();

        String jsonCracUrl = getJsonCracUrl(cseValidRequest, network, cracUrl);
        String raoParametersURL = fileExporter.saveRaoParameters(processTargetDateTime, processType);
        String scaledNetworkDirPath = generateScaledNetworkDirPath(network, processTargetDateTime, processType);
        String scaledNetworkName = network.getNameOrId() + ".xiidm";
        String networkFiledUrl = fileExporter.saveNetworkInArtifact(network, scaledNetworkDirPath + scaledNetworkName, "", processTargetDateTime, processType);

        RaoRequest raoRequest = new RaoRequest(requestId, networkFiledUrl, jsonCracUrl, raoParametersURL, "CSE/VALID/");
        LOGGER.info("RAO request sent: {}", raoRequest);
        RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
        LOGGER.info("RAO response received: {}", raoResponse);

        String raoResultUrl = raoResponse.getRaoResultFileUrl();
        Crac crac = fileImporter.importCracFromJson(raoResponse.getCracFileUrl());
        RaoResult raoResult = fileImporter.importRaoResult(raoResultUrl, crac);

        return raoResult.getFunctionalCost(OptimizationState.AFTER_CRA) <= 0.0;
    }

    private String getJsonCracUrl(CseValidRequest cseValidRequest, Network network, String cracUrl) {
        CseCrac cseCrac = fileImporter.importCseCrac(cracUrl);
        Crac crac = fileImporter.importCrac(cseCrac, cseValidRequest.getTimestamp(), network);
        return fileExporter.saveCracInJsonFormat(crac, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
    }

    private String generateScaledNetworkDirPath(Network network, OffsetDateTime processTargetDateTime, ProcessType processType) {
        String basePath = fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS);
        String variantName = network.getVariantManager().getWorkingVariantId();
        return String.format("%s/%s-%s/", basePath, ++variantCounter, variantName);
    }
}

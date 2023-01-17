/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class RaoValidator implements NetworkValidator<RaoResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoValidator.class);

    private final ProcessType processType;
    private final String requestId;
    private final OffsetDateTime processTargetDateTime;
    private final String jsonCracUrl;
    private final String raoParametersUrl;
    private final RaoRunnerClient raoRunnerClient;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private int variantCounter = 0;

    public RaoValidator(ProcessType processType, String requestId, OffsetDateTime processTargetDateTime, String jsonCracUrl, String raoParametersUrl, RaoRunnerClient raoRunnerClient, FileImporter fileImporter, FileExporter fileExporter) {
        this.processType = processType;
        this.requestId = requestId;
        this.processTargetDateTime = processTargetDateTime;
        this.jsonCracUrl = jsonCracUrl;
        this.raoParametersUrl = raoParametersUrl;
        this.raoRunnerClient = raoRunnerClient;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
    }

    @Override
    public DichotomyStepResult<RaoResponse> validateNetwork(Network network, DichotomyStepResult<RaoResponse> dichotomyStepResult) throws ValidationException {
        String scaledNetworkDirPath = generateScaledNetworkDirPath(network);
        String scaledNetworkName = network.getNameOrId() + ".xiidm";
        String networkPresignedUrl = fileExporter.saveNetworkInArtifact(network, scaledNetworkDirPath + scaledNetworkName, "", processTargetDateTime, processType);
        RaoRequest raoRequest = buildRaoRequest(networkPresignedUrl, "CSE/VALID/" + scaledNetworkDirPath);
        try {
            LOGGER.info("RAO request sent: {}", raoRequest);
            RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("RAO response received: {}", raoResponse);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), fileImporter.importCracFromJson(raoResponse.getCracFileUrl()));
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, raoResponse);
        } catch (RuntimeException e) {
            throw new ValidationException("RAO run failed: " + e.getMessage());
        }
    }

    private RaoRequest buildRaoRequest(String networkPresignedUrl, String scaledNetworkDirPath) {
        return new RaoRequest(requestId, networkPresignedUrl, jsonCracUrl, raoParametersUrl, scaledNetworkDirPath);
    }

    private String generateScaledNetworkDirPath(Network network) {
        String basePath = fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS);
        String variantName = network.getVariantManager().getWorkingVariantId();
        return String.format("%s/%s-%s/", basePath, ++variantCounter, variantName);
    }
}

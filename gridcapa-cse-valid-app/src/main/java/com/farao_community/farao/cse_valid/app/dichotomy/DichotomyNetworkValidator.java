/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
public class DichotomyNetworkValidator implements NetworkValidator<RaoSuccessResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DichotomyNetworkValidator.class);

    private final ProcessType processType;
    private final String requestId;
    private final String runId;
    private final OffsetDateTime processTargetDateTime;
    private final String jsonCracUrl;
    private final String raoParametersUrl;
    private final RaoRunnerClient raoRunnerClient;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private int variantCounter = 0;

    public DichotomyNetworkValidator(CseValidRequest cseValidRequest,
                                     String jsonCracUrl,
                                     String raoParametersUrl,
                                     RaoRunnerClient raoRunnerClient,
                                     FileImporter fileImporter,
                                     FileExporter fileExporter) {
        this.processType = cseValidRequest.getProcessType();
        this.requestId = cseValidRequest.getId();
        this.runId = cseValidRequest.getCurrentRunId();
        this.processTargetDateTime = cseValidRequest.getTimestamp();
        this.jsonCracUrl = jsonCracUrl;
        this.raoParametersUrl = raoParametersUrl;
        this.raoRunnerClient = raoRunnerClient;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
    }

    @Override
    public DichotomyStepResult<RaoSuccessResponse> validateNetwork(Network network, DichotomyStepResult<RaoSuccessResponse> dichotomyStepResult) throws ValidationException {
        String scaledNetworkDirPath = generateScaledNetworkDirPath(network);
        String scaledNetworkName = network.getNameOrId() + ".xiidm";
        String networkPresignedUrl = fileExporter.saveNetworkInArtifact(network, scaledNetworkDirPath + scaledNetworkName, "", processTargetDateTime, processType);
        RaoRequest raoRequest = buildRaoRequest(networkPresignedUrl, "CSE/VALID/" + scaledNetworkDirPath);
        try {
            LOGGER.info("RAO request sent: {}", raoRequest);
            AbstractRaoResponse abstractRaoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("RAO response received: {}", abstractRaoResponse);
            if (abstractRaoResponse.isRaoFailed()) {
                RaoFailureResponse failureResponse = (RaoFailureResponse) abstractRaoResponse;
                throw new ValidationException(failureResponse.getErrorMessage());
            }

            RaoSuccessResponse raoResponse = (RaoSuccessResponse) abstractRaoResponse;
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), fileImporter.importCracFromJson(raoResponse.getCracFileUrl(), network));
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, raoResponse);
        } catch (RuntimeException e) {
            throw new ValidationException("RAO run failed", e);
        }
    }

    private RaoRequest buildRaoRequest(String networkPresignedUrl, String scaledNetworkDirPath) {
        return new RaoRequest.RaoRequestBuilder()
                .withId(requestId)
                .withRunId(runId)
                .withNetworkFileUrl(networkPresignedUrl)
                .withCracFileUrl(jsonCracUrl)
                .withRaoParametersFileUrl(raoParametersUrl)
                .withResultsDestination(scaledNetworkDirPath)
                .build();
    }

    private String generateScaledNetworkDirPath(Network network) {
        String basePath = fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS);
        String variantName = network.getVariantManager().getWorkingVariantId();
        return String.format("%s/%s-%s/", basePath, ++variantCounter, variantName);
    }
}

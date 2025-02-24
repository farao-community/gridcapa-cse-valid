/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.service;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.cse_valid.app.rao.CseValidRaoRunner;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Service
public class ComputationService {

    private final FileExporter fileExporter;
    private final CseValidRaoRunner cseValidRaoRunner;

    public ComputationService(FileExporter fileExporter, CseValidRaoRunner cseValidRaoRunner) {
        this.fileExporter = fileExporter;
        this.cseValidRaoRunner = cseValidRaoRunner;
    }

    protected void shiftNetwork(double shiftValue, Network network, NetworkShifter networkShifter) {
        try {
            networkShifter.shiftNetwork(shiftValue, network);
        } catch (GlskLimitationException | ShiftingException e) {
            throw new CseValidShiftFailureException("Initial shift failed to value " + shiftValue, e);
        }
    }

    protected AbstractRaoResponse runRao(CseValidRequest cseValidRequest, Network network, String jsonCracUrl, String raoParametersURL) {
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();
        String requestId = cseValidRequest.getId();
        String runId = cseValidRequest.getCurrentRunId();
        String scaledNetworkDirPath = generateScaledNetworkDirPath(network, processTargetDateTime, processType);
        String scaledNetworkName = network.getNameOrId() + ".xiidm";
        String networkFilePath = scaledNetworkDirPath + scaledNetworkName;
        String networkFiledUrl = fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType);
        String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;

        return cseValidRaoRunner.runRao(requestId, runId, networkFiledUrl, jsonCracUrl, raoParametersURL, resultsDestination);
    }

    private String generateScaledNetworkDirPath(Network network, OffsetDateTime processTargetDateTime, ProcessType processType) {
        String basePath = fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS);
        String variantName = network.getVariantManager().getWorkingVariantId();
        return basePath + variantName;
    }
}

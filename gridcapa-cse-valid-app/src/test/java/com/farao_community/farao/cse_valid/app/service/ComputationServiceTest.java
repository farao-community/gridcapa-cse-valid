/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.service;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.cse_valid.app.rao.CseValidRaoValidator;
import com.farao_community.farao.cse_valid.app.utils.CseValidRequestTestData;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class ComputationServiceTest {

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private CseValidRaoValidator cseValidRaoValidator;

    @Autowired
    private ComputationService computationService;

    @Test
    void shiftNetwork() throws GlskLimitationException, ShiftingException {
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        double shiftValue = 100.0;

        computationService.shiftNetwork(shiftValue, network, networkShifter);

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void shiftNetworkShouldThrowGlskLimitationException() throws GlskLimitationException, ShiftingException {
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        double shiftValue = 100.0;

        doThrow(GlskLimitationException.class).when(networkShifter).shiftNetwork(shiftValue, network);

        assertThrows(CseValidShiftFailureException.class, () -> {
            computationService.shiftNetwork(shiftValue, network, networkShifter);
        }, "CseValidShiftFailureException error was expected");

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void shiftNetworkShouldThrowShiftingException() throws GlskLimitationException, ShiftingException {
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        double shiftValue = 100.0;

        doThrow(ShiftingException.class).when(networkShifter).shiftNetwork(shiftValue, network);

        assertThrows(CseValidShiftFailureException.class, () -> {
            computationService.shiftNetwork(shiftValue, network, networkShifter);
        }, "CseValidShiftFailureException error was expected");

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void runRao() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();
        String requestId = cseValidRequest.getId();

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParametersURL = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFiledUrl = "CSE/Valid/network.utc";
        String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;

        Network network = mock(Network.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        VariantManager variantManager = mock(VariantManager.class);

        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFiledUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidRaoValidator.runRao(requestId, networkFiledUrl, jsonCracUrl, raoParametersURL, resultsDestination)).thenReturn(raoResponse);

        RaoResponse response = computationService.runRao(cseValidRequest, network, jsonCracUrl, raoParametersURL);

        assertEquals(raoResponse, response);
    }
}

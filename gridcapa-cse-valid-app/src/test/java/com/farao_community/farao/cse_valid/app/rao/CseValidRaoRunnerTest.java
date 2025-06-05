/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.rao;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.utils.CseValidRequestTestData;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class CseValidRaoRunnerTest {

    @MockitoBean
    private FileImporter fileImporter;

    @MockitoBean
    private RaoRunnerClient raoRunnerClient;

    @Autowired
    private CseValidRaoRunner cseValidRaoRunner;

    private static final String NETWORK_FILE_URL = "CSE/Valid/network.utc";
    private static final String JSON_CRAC_URL = "/CSE/VALID/crac.utc";
    private static final String RAO_PARAMETER_URL = "/CSE/VALID/raoParameter.utc";
    private static final String RAO_RESULT_FILE_URL = "raoResultUrl";
    private static final String RESULTS_DESTINATION = "CSE/VALID/IDCC/2023/01/09/12_30/ARTIFACTS/1234";

    /* ------------------- runRao ------------------- */

    @Test
    void runRao() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String requestId = cseValidRequest.getId();
        String runId = cseValidRequest.getCurrentRunId();
        AbstractRaoResponse raoResponse = mock(AbstractRaoResponse.class);

        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);

        AbstractRaoResponse result = cseValidRaoRunner.runRao(requestId, runId, NETWORK_FILE_URL, JSON_CRAC_URL, RAO_PARAMETER_URL, RESULTS_DESTINATION);

        assertEquals(raoResponse, result);

    }

    /* ------------------- isSecure ------------------- */

    @Test
    void isSecureShouldReturnTrue() {
        Crac crac = mock(Crac.class);
        RaoSuccessResponse raoResponse = mock(RaoSuccessResponse.class);
        RaoResult raoResult = mock(RaoResult.class);
        Network network = mock(Network.class);

        when(fileImporter.importCracFromJson(JSON_CRAC_URL, network)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.isSecure()).thenReturn(true);

        boolean isSecure = cseValidRaoRunner.isSecure(raoResponse, network);

        assertTrue(isSecure);
    }

    @Test
    void isSecureShouldReturnFalse() {
        Crac crac = mock(Crac.class);
        RaoSuccessResponse raoResponse = mock(RaoSuccessResponse.class);
        RaoResult raoResult = mock(RaoResult.class);
        Network network = mock(Network.class);

        when(fileImporter.importCracFromJson(JSON_CRAC_URL, network)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.isSecure()).thenReturn(false);

        boolean isSecure = cseValidRaoRunner.isSecure(raoResponse, network);

        assertFalse(isSecure);
    }

    @Test
    void isSecureShouldReturnFalseInCaseOfRaoFailure() {
        RaoFailureResponse raoResponse = new RaoFailureResponse.Builder().build();
        Network network = mock(Network.class);

        boolean isSecure = cseValidRaoRunner.isSecure(raoResponse, network);

        assertFalse(isSecure);
    }
}

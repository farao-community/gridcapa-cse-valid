/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.cse_valid.app.util.CseValidRequestTestData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class CseValidRaoValidatorTest {

    @MockBean
    private FileImporter fileImporter;

    @MockBean
    private RaoRunnerClient raoRunnerClient;

    @SpyBean
    private CseValidRaoValidator cseValidRaoValidator;

    private static final String NETWORK_FILE_URL = "CSE/Valid/network.utc";
    private static final String JSON_CRAC_URL = "/CSE/VALID/crac.utc";
    private static final String RAO_PARAMETER_URL = "/CSE/VALID/raoParameter.utc";
    private static final String RAO_RESULT_FILE_URL = "raoResultUrl";
    private static final String RESULTS_DESTINATION = "CSE/VALID/IDCC/2023/01/09/12_30/ARTIFACTS/1234";

    /* ------------------- runRao ------------------- */

    @Test
    void runRao() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);

        RaoResponse raoResponse = mock(RaoResponse.class);

        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);

        RaoResponse result = cseValidRaoValidator.runRao(cseValidRequest, NETWORK_FILE_URL, JSON_CRAC_URL, RAO_PARAMETER_URL, RESULTS_DESTINATION);

        assertEquals(raoResponse, result);

    }

    /* ------------------- isSecure ------------------- */

    @Test
    void isSecureShouldReturnTrue() {
        Crac crac = mock(Crac.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        RaoResult raoResult = mock(RaoResult.class);

        when(fileImporter.importCracFromJson(JSON_CRAC_URL)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(-1.);

        boolean isSecure = cseValidRaoValidator.isSecure(raoResponse);

        assertTrue(isSecure);
    }

    @Test
    void isSecureShouldReturnFalse() {
        Crac crac = mock(Crac.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        RaoResult raoResult = mock(RaoResult.class);

        when(fileImporter.importCracFromJson(JSON_CRAC_URL)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(1.);

        boolean isSecure = cseValidRaoValidator.isSecure(raoResponse);

        assertFalse(isSecure);
    }
}

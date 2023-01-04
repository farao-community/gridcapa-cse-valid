/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.util.CseValidRequestTestData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class CseValidRaoValidatorTest {

    @MockBean
    private FileImporter fileImporter;

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private RaoRunnerClient raoRunnerClient;

    @SpyBean
    private CseValidRaoValidator cseValidRaoValidator;

    private static final String JSON_CRAC_URL = "/CSE/VALID/crac.utc";
    private static final String RAO_PARAMETER_URL = "/CSE/VALID/raoParameter.utc";

    /* ------------------- getJsonCracUrl ------------------- */

    @Test
    void getJsonCracUrlWithImportCornerCrac() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getImportCrac().getUrl();

        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);

        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, cseValidRequest.getTimestamp(), network)).thenReturn(crac);
        when(fileExporter.saveCracInJsonFormat(crac, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType())).thenReturn(JSON_CRAC_URL);

        String jsonCracUrl = cseValidRaoValidator.getJsonCracUrl(cseValidRequest, network, cracUrl);

        assertEquals(jsonCracUrl, JSON_CRAC_URL);
    }

    @Test
    void getJsonCracUrlWithExportCornerCrac() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getExportCrac().getUrl();

        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);

        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, cseValidRequest.getTimestamp(), network)).thenReturn(crac);
        when(fileExporter.saveCracInJsonFormat(crac, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType())).thenReturn(JSON_CRAC_URL);

        String jsonCracUrl = cseValidRaoValidator.getJsonCracUrl(cseValidRequest, network, cracUrl);

        assertEquals(jsonCracUrl, JSON_CRAC_URL);
    }

    /* ------------------- getNetworkValidator ------------------- */

    @Test
    void getNetworkValidatorWithImportCornerCrac() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);

        when(fileExporter.saveRaoParameters(cseValidRequest.getTimestamp(), cseValidRequest.getProcessType())).thenReturn(RAO_PARAMETER_URL);

        NetworkValidator<RaoResponse> networkValidator = cseValidRaoValidator.getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        assertNotNull(networkValidator);
    }

    @Test
    void getNetworkValidatorWithExportCornerCrac() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);

        when(fileExporter.saveRaoParameters(cseValidRequest.getTimestamp(), cseValidRequest.getProcessType())).thenReturn(RAO_PARAMETER_URL);

        NetworkValidator<RaoResponse> networkValidator = cseValidRaoValidator.getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        assertNotNull(networkValidator);
    }

    /* ------------------- isNetworkSecure ------------------- */

    @Test
    void isNetworkSecureWithExportCornerCracShouldReturnTrue() throws ValidationException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getExportCrac().getUrl();

        Network network = mock(Network.class);
        NetworkValidator<RaoResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyStepResult<RaoResponse> dichotomyStepResult = mock(DichotomyStepResult.class);

        doReturn(JSON_CRAC_URL).when(cseValidRaoValidator).getJsonCracUrl(cseValidRequest, network, cracUrl);
        doReturn(networkValidator).when(cseValidRaoValidator).getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        when(networkValidator.validateNetwork(network)).thenReturn(dichotomyStepResult);
        when(dichotomyStepResult.isValid()).thenReturn(true);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertTrue(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithExportCornerCracShouldReturnFalse() throws ValidationException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getExportCrac().getUrl();

        Network network = mock(Network.class);
        NetworkValidator<RaoResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyStepResult<RaoResponse> dichotomyStepResult = mock(DichotomyStepResult.class);

        doReturn(JSON_CRAC_URL).when(cseValidRaoValidator).getJsonCracUrl(cseValidRequest, network, cracUrl);
        doReturn(networkValidator).when(cseValidRaoValidator).getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        when(networkValidator.validateNetwork(network)).thenReturn(dichotomyStepResult);
        when(dichotomyStepResult.isValid()).thenReturn(false);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertFalse(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithExportCornerCracShouldReturnFalseBecauseExceptionIsThrown() throws ValidationException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getExportCrac().getUrl();

        Network network = mock(Network.class);
        NetworkValidator<RaoResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyStepResult<RaoResponse> dichotomyStepResult = mock(DichotomyStepResult.class);

        doReturn(JSON_CRAC_URL).when(cseValidRaoValidator).getJsonCracUrl(cseValidRequest, network, cracUrl);
        doReturn(networkValidator).when(cseValidRaoValidator).getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        when(networkValidator.validateNetwork(network)).thenThrow(ValidationException.class);
        when(dichotomyStepResult.isValid()).thenReturn(false);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertFalse(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithImportCornerCracShouldReturnTrue() throws ValidationException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getImportCrac().getUrl();

        Network network = mock(Network.class);
        NetworkValidator<RaoResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyStepResult<RaoResponse> dichotomyStepResult = mock(DichotomyStepResult.class);

        doReturn(JSON_CRAC_URL).when(cseValidRaoValidator).getJsonCracUrl(cseValidRequest, network, cracUrl);
        doReturn(networkValidator).when(cseValidRaoValidator).getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        when(networkValidator.validateNetwork(network)).thenReturn(dichotomyStepResult);
        when(dichotomyStepResult.isValid()).thenReturn(true);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertTrue(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithImportCornerCracShouldReturnFalse() throws ValidationException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getImportCrac().getUrl();

        Network network = mock(Network.class);
        NetworkValidator<RaoResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyStepResult<RaoResponse> dichotomyStepResult = mock(DichotomyStepResult.class);

        doReturn(JSON_CRAC_URL).when(cseValidRaoValidator).getJsonCracUrl(cseValidRequest, network, cracUrl);
        doReturn(networkValidator).when(cseValidRaoValidator).getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        when(networkValidator.validateNetwork(network)).thenReturn(dichotomyStepResult);
        when(dichotomyStepResult.isValid()).thenReturn(false);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertFalse(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithImportCornerCracShouldReturnFalseBecauseExceptionIsThrown() throws ValidationException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cracUrl = cseValidRequest.getImportCrac().getUrl();

        Network network = mock(Network.class);
        NetworkValidator<RaoResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyStepResult<RaoResponse> dichotomyStepResult = mock(DichotomyStepResult.class);

        doReturn(JSON_CRAC_URL).when(cseValidRaoValidator).getJsonCracUrl(cseValidRequest, network, cracUrl);
        doReturn(networkValidator).when(cseValidRaoValidator).getNetworkValidator(cseValidRequest, JSON_CRAC_URL);
        when(networkValidator.validateNetwork(network)).thenThrow(ValidationException.class);
        when(dichotomyStepResult.isValid()).thenReturn(false);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertFalse(isNetworkSecure);
    }
}

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
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.OffsetDateTime;

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
    private FileExporter fileExporter;

    @MockBean
    private RaoRunnerClient raoRunnerClient;

    @SpyBean
    private CseValidRaoValidator cseValidRaoValidator;

    private static final String JSON_CRAC_URL = "/CSE/VALID/crac.utc";
    private static final String RAO_PARAMETER_URL = "/CSE/VALID/raoParameter.utc";
    private static final String BASE_PATH = "IDCC/2023/01/09/12_30/ARTIFACTS/";
    private static final String VARIANT_NAME = "1234";
    private static final String NETWORK_NAME_OR_ID = "test";
    private static final String SCALED_NETWORK_DIR_PATH = BASE_PATH + "/" + 0 + "-" + VARIANT_NAME + "/";
    private static final String NETWORK_FILE_PATH = SCALED_NETWORK_DIR_PATH + NETWORK_NAME_OR_ID + ".xiidm";
    private static final String NETWORK_FILE_URL = "CSE/Valid/network.utc";
    private static final String RAO_RESULT_FILE_URL = "raoResultUrl";

    /* ------------------- isNetworkSecure ------------------- */

    @Test
    void isNetworkSecureWithExportCornerCracShouldReturnTrue() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime offsetDateTime = cseValidRequest.getTimestamp();
        String cracUrl = cseValidRequest.getExportCrac().getUrl();

        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        RaoResult raoResult = mock(RaoResult.class);

        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, offsetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(JSON_CRAC_URL)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(fileExporter.saveCracInJsonFormat(crac, offsetDateTime, processType)).thenReturn(JSON_CRAC_URL);
        when(fileExporter.saveRaoParameters(offsetDateTime, processType)).thenReturn(RAO_PARAMETER_URL);
        when(fileExporter.makeDestinationMinioPath(offsetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(BASE_PATH);
        when(fileExporter.saveNetworkInArtifact(network, NETWORK_FILE_PATH, "", offsetDateTime, processType)).thenReturn(NETWORK_FILE_URL);

        when(network.getNameOrId()).thenReturn(NETWORK_NAME_OR_ID);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(VARIANT_NAME);

        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(-1.);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertTrue(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithExportCornerCracShouldReturnFalse() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime offsetDateTime = cseValidRequest.getTimestamp();
        String cracUrl = cseValidRequest.getExportCrac().getUrl();

        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        RaoResult raoResult = mock(RaoResult.class);

        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, offsetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(JSON_CRAC_URL)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(fileExporter.saveCracInJsonFormat(crac, offsetDateTime, processType)).thenReturn(JSON_CRAC_URL);
        when(fileExporter.saveRaoParameters(offsetDateTime, processType)).thenReturn(RAO_PARAMETER_URL);
        when(fileExporter.makeDestinationMinioPath(offsetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(BASE_PATH);
        when(fileExporter.saveNetworkInArtifact(network, NETWORK_FILE_PATH, "", offsetDateTime, processType)).thenReturn(NETWORK_FILE_URL);

        when(network.getNameOrId()).thenReturn(NETWORK_NAME_OR_ID);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(VARIANT_NAME);

        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(1.);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertFalse(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithImportCornerCracShouldReturnTrue() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime offsetDateTime = cseValidRequest.getTimestamp();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();

        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        RaoResult raoResult = mock(RaoResult.class);

        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, offsetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(JSON_CRAC_URL)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(fileExporter.saveCracInJsonFormat(crac, offsetDateTime, processType)).thenReturn(JSON_CRAC_URL);
        when(fileExporter.saveRaoParameters(offsetDateTime, processType)).thenReturn(RAO_PARAMETER_URL);
        when(fileExporter.makeDestinationMinioPath(offsetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(BASE_PATH);
        when(fileExporter.saveNetworkInArtifact(network, NETWORK_FILE_PATH, "", offsetDateTime, processType)).thenReturn(NETWORK_FILE_URL);

        when(network.getNameOrId()).thenReturn(NETWORK_NAME_OR_ID);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(VARIANT_NAME);

        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(-1.);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertTrue(isNetworkSecure);
    }

    @Test
    void isNetworkSecureWithImportCornerCracShouldReturnFalse() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime offsetDateTime = cseValidRequest.getTimestamp();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();

        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        RaoResult raoResult = mock(RaoResult.class);

        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, offsetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(JSON_CRAC_URL)).thenReturn(crac);
        when(fileImporter.importRaoResult(RAO_RESULT_FILE_URL, crac)).thenReturn(raoResult);

        when(fileExporter.saveCracInJsonFormat(crac, offsetDateTime, processType)).thenReturn(JSON_CRAC_URL);
        when(fileExporter.saveRaoParameters(offsetDateTime, processType)).thenReturn(RAO_PARAMETER_URL);
        when(fileExporter.makeDestinationMinioPath(offsetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(BASE_PATH);
        when(fileExporter.saveNetworkInArtifact(network, NETWORK_FILE_PATH, "", offsetDateTime, processType)).thenReturn(NETWORK_FILE_URL);

        when(network.getNameOrId()).thenReturn(NETWORK_NAME_OR_ID);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(VARIANT_NAME);

        when(raoRunnerClient.runRao(any())).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn(RAO_RESULT_FILE_URL);
        when(raoResponse.getCracFileUrl()).thenReturn(JSON_CRAC_URL);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(1.);

        boolean isNetworkSecure = cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cracUrl);

        assertFalse(isNetworkSecure);
    }
}

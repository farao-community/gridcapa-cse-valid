package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.utils.CseValidRequestTestData;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DichotomyNetworkValidatorTest {

    @Test
    void validateNetwork() {
        RaoRunnerClient raoRunnerClient = Mockito.mock(RaoRunnerClient.class);
        FileImporter fileImporter = Mockito.mock(FileImporter.class);
        FileExporter fileExporter = Mockito.mock(FileExporter.class);
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.D2CC);

        String networkPresignedUrl = "networkPresignedUrl";
        String scaledNetworkDirPath = "scaledNetworkDirPath";
        String cracUrl = "http://cracUrl";
        String raoPramasUrl = "raoPramasUrl";

        DichotomyNetworkValidator dichotomyNetworkValidator = new DichotomyNetworkValidator(cseValidRequest, cracUrl, raoPramasUrl, raoRunnerClient, fileImporter, fileExporter);
        RaoRequest raoRequest = ReflectionTestUtils.invokeMethod(dichotomyNetworkValidator, "buildRaoRequest", networkPresignedUrl, scaledNetworkDirPath);
        assertNotNull(raoRequest);
        assertEquals(cseValidRequest.getId(), raoRequest.getId());
        assertEquals(cseValidRequest.getCurrentRunId(), raoRequest.getRunId());
        assertEquals(cracUrl, raoRequest.getCracFileUrl());
        assertEquals(raoPramasUrl, raoRequest.getRaoParametersFileUrl());
    }
}

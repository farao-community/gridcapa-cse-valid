package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class DichotomyNetworkValidatorTest {

    private static final String CRAC_URL = "http://cracUrl";
    private static final String RAO_PARAMS_URL = "raoParamsUrl";

    private final RaoRunnerClient raoRunnerClient = Mockito.mock(RaoRunnerClient.class);
    private final FileImporter fileImporter = Mockito.mock(FileImporter.class);
    private final FileExporter fileExporter = Mockito.mock(FileExporter.class);
    private final CseValidRequest cseValidRequest = Mockito.mock(CseValidRequest.class);
    private final Network network = Mockito.mock(Network.class);
    private final DichotomyStepResult<RaoSuccessResponse> dichotomyStepResult = Mockito.mock(DichotomyStepResult.class);

    private DichotomyNetworkValidator validator;

    @BeforeEach
    void init() {
        // Network
        final VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("workingVariantId");
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(network.getNameOrId()).thenReturn("nameOrId");

        // File Exporter
        Mockito.when(fileExporter.makeDestinationMinioPath(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("/BASE/PATH/");
        Mockito.when(fileExporter.saveNetworkInArtifact(Mockito.eq(network), Mockito.anyString(), Mockito.eq(""), Mockito.any(), Mockito.any()))
                .thenReturn("networkPresignedUrl");

        // CSE Valid Request
        Mockito.when(cseValidRequest.getId()).thenReturn("theId");
        Mockito.when(cseValidRequest.getCurrentRunId()).thenReturn("theRunId");
        validator = new DichotomyNetworkValidator(cseValidRequest, CRAC_URL, RAO_PARAMS_URL, raoRunnerClient, fileImporter, fileExporter);
    }

    @Test
    void buildRaoRequest() {
        final String networkPresignedUrl = "networkPresignedUrl";
        final String scaledNetworkDirPath = "scaledNetworkDirPath";

        final RaoRequest raoRequest = ReflectionTestUtils.invokeMethod(validator, "buildRaoRequest", networkPresignedUrl, scaledNetworkDirPath);

        Assertions.assertThat(raoRequest)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", cseValidRequest.getId())
                .hasFieldOrPropertyWithValue("runId", cseValidRequest.getCurrentRunId())
                .hasFieldOrPropertyWithValue("cracFileUrl", CRAC_URL)
                .hasFieldOrPropertyWithValue("raoParametersFileUrl", RAO_PARAMS_URL);
    }

    @Test
    void validateNetworkWithRaoFailure() {
        final String message = "Test exception";
        Mockito.when(raoRunnerClient.runRao(Mockito.any(RaoRequest.class)))
                .thenReturn(new RaoFailureResponse.Builder().withErrorMessage(message).build());

        Assertions.assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> validator.validateNetwork(network, dichotomyStepResult))
                .withMessage(message);
    }

    @Test
    void validateNetworkWithRaoException() {
        Mockito.when(raoRunnerClient.runRao(Mockito.any(RaoRequest.class)))
                .thenThrow(new RuntimeException("Test exception"));

        Assertions.assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> validator.validateNetwork(network, dichotomyStepResult))
                .withMessage("RAO run failed");
    }

    @Test
    void validateNetwork() throws ValidationException {
        final RaoSuccessResponse successResponse = new RaoSuccessResponse.Builder()
                .withCracFileUrl("cracFileUrl")
                .withRaoResultFileUrl("raoResultFileUrl")
                .build();
        final Crac crac = Mockito.mock(Crac.class);
        final RaoResult raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(raoRunnerClient.runRao(Mockito.any(RaoRequest.class))).thenReturn(successResponse);
        Mockito.when(fileImporter.importCracFromJson(Mockito.anyString(), Mockito.eq(network))).thenReturn(crac);
        Mockito.when(fileImporter.importRaoResult(Mockito.anyString(), Mockito.eq(crac))).thenReturn(raoResult);
        Mockito.when(raoResult.isSecure(Mockito.any())).thenReturn(true);

        final DichotomyStepResult<RaoSuccessResponse> result = validator.validateNetwork(network, dichotomyStepResult);

        Assertions.assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("raoResult", raoResult)
                .hasFieldOrPropertyWithValue("secure", true)
                .hasFieldOrPropertyWithValue("validationData", successResponse);
    }
}

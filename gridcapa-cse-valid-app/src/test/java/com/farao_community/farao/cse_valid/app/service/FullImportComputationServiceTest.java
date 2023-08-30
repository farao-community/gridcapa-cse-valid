/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.service;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.CseValidNetworkShifterProvider;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.TTimestampWrapper;
import com.farao_community.farao.cse_valid.app.TcDocumentTypeWriter;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.cse_valid.app.helper.LimitingElementHelper;
import com.farao_community.farao.cse_valid.app.helper.NetPositionHelper;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.rao.CseValidRaoValidator;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.utils.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.utils.TimestampTestData;
import com.farao_community.farao.cse_valid.app.validator.CseValidRequestValidator;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */

@SpringBootTest
class FullImportComputationServiceTest {

    @MockBean
    private DichotomyRunner dichotomyRunner;

    @MockBean
    private FileImporter fileImporter;

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private Logger businessLogger;

    @MockBean
    private CseValidNetworkShifterProvider cseValidNetworkShifterProvider;

    @MockBean
    private CseValidRaoValidator cseValidRaoValidator;

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Autowired
    private EicCodesMapper eicCodesMapper;

    @Autowired
    private FullImportComputationService fullImportComputationService;

    /* ------------------- computeTimestamp ------------------- */

    @Test
    void computeTimestampMibniiAndAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMnii();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMibniiAndAntcfinalBothZero() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMibniiAndAntcfinalBothZero();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMibniiAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndAntcfinal();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibnii();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampActualNtcAboveTarget() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcAboveTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).info(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.TEN);
    }

    @Test
    void computeTimestampFilesNotExisting() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);
        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, CRAC file, GLSK file.";
        CseValidRequestValidatorException e = new CseValidRequestValidatorException(errorMessage);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        try (MockedStatic<CseValidRequestValidator> cseValidRequestValidatorMockedStatic = Mockito.mockStatic(CseValidRequestValidator.class)) {
            cseValidRequestValidatorMockedStatic.when(() -> CseValidRequestValidator.checkAllFilesExist(cseValidRequest, false))
                    .thenThrow(e);
            fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, e.getMessage());
    }

    @Test
    void computeTimestampShouldThrowCseValidShiftFailureExceptionBecauseGlskLimitationExceptionWasThrownWhenShifting() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();

        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        double italianImport = 5.0;
        double shiftValue = (timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue()) - italianImport;

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        doThrow(GlskLimitationException.class).when(networkShifter).shiftNetwork(shiftValue, network);

        assertThrows(CseValidShiftFailureException.class, () -> {
            try (MockedStatic<NetPositionHelper> netPositionHelperMockedStatic = Mockito.mockStatic(NetPositionHelper.class)) {
                netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeItalianImport(network))
                        .thenReturn(italianImport);
                fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
            }
        }, "CseValidShiftFailureException error was expected");

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void computeTimestampShouldThrowCseValidShiftFailureExceptionBecauseShiftingExceptionWasThrownWhenShifting() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();

        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        double italianImport = 5.0;
        double shiftValue = (timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue()) - italianImport;

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        doThrow(ShiftingException.class).when(networkShifter).shiftNetwork(shiftValue, network);

        assertThrows(CseValidShiftFailureException.class, () -> {
            try (MockedStatic<NetPositionHelper> netPositionHelperMockedStatic = Mockito.mockStatic(NetPositionHelper.class)) {
                netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeItalianImport(network))
                        .thenReturn(italianImport);
                fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
            }
        }, "CseValidShiftFailureException error was expected");

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void computeTimestampShouldNotRunDichotomyBecauseNetworkShiftedIsUnsecure() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParametersUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;
        double italianImport = 5.0;
        double shiftValue = (timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue()) - italianImport;
        BigDecimal mibnii = timestampWrapper.getMibniiValue().subtract(timestampWrapper.getAntcfinalValue());

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);
        Crac crac = mock(Crac.class);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCracCreationContext(cracUrl, processTargetDateTime, network)).thenReturn(cracCreationContext);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParametersUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(false);

        try (MockedStatic<NetPositionHelper> netPositionHelperMockedStatic = Mockito.mockStatic(NetPositionHelper.class)) {
            netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeItalianImport(network))
                    .thenReturn(italianImport);
            fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, mibnii);
    }

    @Test
    void computeTimestampRunDichotomyError() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParametersUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;
        double italianImport = 5.0;
        double shiftValue = (timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue()) - italianImport;

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);
        Crac crac = mock(Crac.class);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCracCreationContext(cracUrl, processTargetDateTime, network)).thenReturn(cracCreationContext);
        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParametersUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(true);

        when(dichotomyRunner.runDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl, network, false)).thenReturn(null);

        try (MockedStatic<NetPositionHelper> netPositionHelperMockedStatic = Mockito.mockStatic(NetPositionHelper.class)) {
            netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeItalianImport(network))
                    .thenReturn(italianImport);
            fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(tcDocumentTypeWriter, times(1)).fillDichotomyError(timestamp);
    }

    @Test
    void computeTimestampRunDichotomySuccessHighestValidStepNotNull() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParametersUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;
        String raoResultFileUrl = "CSE/VALID/raoResult.utc";
        double italianImport = 5.0;
        double shiftValue = (timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue()) - italianImport;
        BigDecimal mibnii = timestampWrapper.getMibniiValue().subtract(timestampWrapper.getAntcfinalValue());
        TLimitingElement limitingElement = new TLimitingElement();
        double fullImportValue = 10.0;

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);
        Crac crac = mock(Crac.class);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);
        RaoResult raoResult = mock(RaoResult.class);
        DichotomyStepResult<RaoResponse> highestValidStep = mock(DichotomyStepResult.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCracCreationContext(cracUrl, processTargetDateTime, network)).thenReturn(cracCreationContext);
        when(fileImporter.importNetwork(networkFileUrl)).thenReturn(network);
        when(fileImporter.importRaoResult(raoResultFileUrl, crac)).thenReturn(raoResult);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParametersUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(true);

        when(cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(true);

        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getValidationData()).thenReturn(raoResponse);
        when(fileImporter.importRaoResult(raoResultFileUrl, crac)).thenReturn(raoResult);
        when(dichotomyRunner.runDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl, network, false)).thenReturn(dichotomyResult);

        when(raoResponse.getNetworkWithPraFileUrl()).thenReturn(networkFileUrl);
        when(raoResponse.getRaoResultFileUrl()).thenReturn(raoResultFileUrl);

        try (
                MockedStatic<LimitingElementHelper> limitingElementServiceMockedStatic = Mockito.mockStatic(LimitingElementHelper.class);
                MockedStatic<NetPositionHelper> netPositionMockedStatic = Mockito.mockStatic(NetPositionHelper.class)
        ) {
            limitingElementServiceMockedStatic.when(() -> LimitingElementHelper.getLimitingElement(raoResult, cracCreationContext, network))
                    .thenReturn(limitingElement);
            netPositionMockedStatic.when(() -> NetPositionHelper.computeItalianImport(network))
                    .thenReturn(italianImport, fullImportValue);
            fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampWithFullImportDichotomyResponse(timestamp, mibnii, fullImportValue, limitingElement);
    }
}

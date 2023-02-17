/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.service;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.CseValidNetworkShifter;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.TTimestampWrapper;
import com.farao_community.farao.cse_valid.app.TcDocumentTypeWriter;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
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
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_CALCULATION_DIRECTIONS;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_SHIFTING_FACTORS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */

@SpringBootTest
class ExportCornerComputationServiceTest {

    @MockBean
    private DichotomyRunner dichotomyRunner;

    @MockBean
    private FileImporter fileImporter;

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private Logger businessLogger;

    @MockBean
    private CseValidNetworkShifter cseValidNetworkShifter;

    @MockBean
    private CseValidRaoValidator cseValidRaoValidator;

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Autowired
    private EicCodesMapper eicCodesMapper;

    @Autowired
    private ExportCornerComputationService exportCornerComputationService;

    /* ------------------- computeTimestamp ------------------- */

    @Test
    void computeTimestampMibiecAndAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMibiecAndAntcfinalBothZero() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMibiecAndAntcfinalBothZero();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMibiecAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMiecAndAntcfinal();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMiecAndMibiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampShiftingFactorsMissing() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithoutShiftingFactors();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_SHIFTING_FACTORS);
    }

    @Test
    void computeTimestampCalculationDirectionsMissing() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithoutCalculationDirections();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_CALCULATION_DIRECTIONS);
    }

    @Test
    void computeTimestampActualNtcAboveTarget() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMiecAndMibiecAndAntcfinalAndActualNtcAboveTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.TEN);
    }

    @Test
    void computeTimestampWhithoutFranceInAreaOrOutAreaShouldThrowAnException() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithoutFranceInAreaOrOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        Assertions.assertThatExceptionOfType(CseValidInvalidDataException.class)
                .isThrownBy(() -> exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter));
    }

    @Test
    void computeTimestampWithFranceInAreaAndFilesNotExisting() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, CRAC file, GLSK file, CRAC Transit.";
        CseValidRequestValidatorException e = new CseValidRequestValidatorException(errorMessage);

        try (MockedStatic<CseValidRequestValidator> cseValidRequestValidatorMockedStatic = Mockito.mockStatic(CseValidRequestValidator.class)) {
            cseValidRequestValidatorMockedStatic.when(() -> CseValidRequestValidator.checkAllFilesExist(cseValidRequest, true))
                    .thenThrow(e);
            exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, e.getMessage());
    }

    @Test
    void computeTimestampWithFranceOutAreaAndFilesNotExisting() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, CRAC file, GLSK file.";
        CseValidRequestValidatorException e = new CseValidRequestValidatorException(errorMessage);

        try (MockedStatic<CseValidRequestValidator> cseValidRequestValidatorMockedStatic = Mockito.mockStatic(CseValidRequestValidator.class)) {
            cseValidRequestValidatorMockedStatic.when(() -> CseValidRequestValidator.checkAllFilesExist(cseValidRequest, false))
                    .thenThrow(e);
            exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, e.getMessage());
    }

    @Test
    void computeTimestampWithFranceInAreaShouldNotRunDichotomyBecauseNetworkShiftedIsSecure() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getExportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);
        Crac crac = mock(Crac.class);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCracCreationContext(cracUrl, processTargetDateTime, network)).thenReturn(cracCreationContext);
        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(true);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl, processType);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl, resultsDestination);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, timestamp.getMIEC().getV());
    }

    @Test
    void computeTimestampWithFranceOutAreaShouldNotRunDichotomyBecauseNetworkShiftedIsSecure() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);
        Crac crac = mock(Crac.class);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCracCreationContext(cracUrl, processTargetDateTime, network)).thenReturn(cracCreationContext);
        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(true);

        exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl, processType);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl, resultsDestination);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, timestamp.getMIEC().getV());
    }

    @Test
    void computeTimestampWithFranceInAreaShouldRunDichotomy() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getExportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TLimitingElement limitingElement = new TLimitingElement();

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
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());
        double exportCornerValue = 10.0;

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);
        Crac crac = mock(Crac.class);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
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

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(false);
        when(raoResponse.getNetworkWithPraFileUrl()).thenReturn(networkFileUrl);
        when(raoResponse.getRaoResultFileUrl()).thenReturn(raoResultFileUrl);

        when(dichotomyRunner.runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl, network)).thenReturn(dichotomyResult);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getValidationData()).thenReturn(raoResponse);
        try (
                MockedStatic<LimitingElementHelper> limitingElementServiceMockedStatic = Mockito.mockStatic(LimitingElementHelper.class);
                MockedStatic<NetPositionHelper> netPositionMockedStatic = Mockito.mockStatic(NetPositionHelper.class)
        ) {
            limitingElementServiceMockedStatic.when(() -> LimitingElementHelper.getLimitingElement(raoResult, cracCreationContext, network))
                    .thenReturn(limitingElement);
            netPositionMockedStatic.when(() -> NetPositionHelper.computeFranceImportFromItaly(network))
                    .thenReturn(exportCornerValue);
            exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl, processType);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(dichotomyRunner, times(1)).runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl, network);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampWithExportCornerDichotomyResponse(timestamp, limitingElement, exportCornerValue, true);
    }

    @Test
    void computeTimestampWithFranceOutAreaShouldRunDichotomy() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TLimitingElement limitingElement = new TLimitingElement();

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
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());
        double exportCornerValue = 10.0;

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);
        Crac crac = mock(Crac.class);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
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

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(false);
        when(raoResponse.getNetworkWithPraFileUrl()).thenReturn(networkFileUrl);
        when(raoResponse.getRaoResultFileUrl()).thenReturn(raoResultFileUrl);

        when(dichotomyRunner.runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl, network)).thenReturn(dichotomyResult);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getValidationData()).thenReturn(raoResponse);
        try (
                MockedStatic<LimitingElementHelper> limitingElementServiceMockedStatic = Mockito.mockStatic(LimitingElementHelper.class);
                MockedStatic<NetPositionHelper> netPositionMockedStatic = Mockito.mockStatic(NetPositionHelper.class)
        ) {
            limitingElementServiceMockedStatic.when(() -> LimitingElementHelper.getLimitingElement(raoResult, cracCreationContext, network))
                    .thenReturn(limitingElement);
            netPositionMockedStatic.when(() -> NetPositionHelper.computeFranceImportFromItaly(network))
                    .thenReturn(exportCornerValue * -1);
            exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        }

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl, processType);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl, resultsDestination);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(dichotomyRunner, times(1)).runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl, network);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampWithExportCornerDichotomyResponse(timestamp, limitingElement, exportCornerValue, false);
    }
}

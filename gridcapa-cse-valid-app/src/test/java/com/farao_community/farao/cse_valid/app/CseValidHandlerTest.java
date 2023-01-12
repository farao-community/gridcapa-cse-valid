/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.dichotomy.LimitingElementService;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
import com.farao_community.farao.cse_valid.app.net_position.AreaReport;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionReport;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
import com.farao_community.farao.cse_valid.app.rao.CseValidRaoValidator;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.util.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.util.TimeStampTestData;
import com.farao_community.farao.cse_valid.app.validator.CseValidRequestValidator;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_CALCULATION_DIRECTIONS;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_SHIFTING_FACTORS;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@SpringBootTest
class CseValidHandlerTest {

    @MockBean
    private MinioAdapter minioAdapter;

    @MockBean
    private DichotomyRunner dichotomyRunner;

    @MockBean
    private FileImporter fileImporter;

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private NetPositionService netPositionService;

    @MockBean
    private LimitingElementService limitingElementService;

    @MockBean
    private Logger businessLogger;

    @MockBean
    private CseValidRequestValidator cseValidRequestValidator;

    @MockBean
    private CseValidNetworkShifter cseValidNetworkShifter;

    @MockBean
    private CseValidRaoValidator cseValidRaoValidator;

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Autowired
    private CseValidHandler cseValidHandler;

    @Test
    void existingTtcAdjustmentFileWithoutCalcul() {
        launchRequest("TTC_Adjustment_20200813_2D4_CSE1_no_calcul.xml");
    }

    @Test
    void existingTtcAdjustmentFileMissingDatas() {
        launchRequest("TTC_Adjustment_20200813_2D4_CSE1_missing_datas.xml");
    }

    @Test
    void existingTtcAdjustmentFileMissingInputFiles() {
        launchRequest("TTC_Adjustment_20200813_2D4_CSE1_missing_input_files.xml");
    }

    @Test
    void existingTtcAdjustmentFileComputationNeededNoResponse() {
        launchRequest("TTC_Adjustment_20200813_2D4_CSE1_missing_input_files.xml");
    }

    @Test
    void valuesAreNotRelevant() {
        launchRequest("TTC_Adjustment_20200813_2D4_CSE1_values_not_relevant.xml");
    }

    @Test
    void nonExistingTtcAdjustmentFile() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String resultFileUrl = "/output.xml";

        when(fileImporter.importTtcAdjustment(cseValidRequest.getTtcAdjustment().getUrl())).thenReturn(null);
        when(fileExporter.saveTtcValidation(any(), eq(cseValidRequest.getTimestamp()), eq(cseValidRequest.getProcessType()))).thenReturn(resultFileUrl);

        CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);

        assertEquals(cseValidRequest.getId(), cseValidResponse.getId());
        assertEquals(resultFileUrl, cseValidResponse.getResultFileUrl());
    }

    private void launchRequest(String ttcAdjustmentFilename) {
        CseValidRequest cseValidRequest = new CseValidRequest("id",
                ProcessType.D2CC,
                OffsetDateTime.of(2020, 8, 12, 22, 30, 0, 0, ZoneOffset.UTC),
                createFileResource(ttcAdjustmentFilename),
                new CseValidFileResource("importCrac.xml", "file://importCrac.xml"),
                new CseValidFileResource("exportCrac.xml", "file://exportCrac.xml"),
                new CseValidFileResource("cgm.xml", "file://cgm.xml"),
                new CseValidFileResource("glsk.xml", "file://glsk.xml"),
                OffsetDateTime.of(2020, 8, 12, 22, 30, 0, 0, ZoneOffset.UTC));
        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(dichotomyRunner.runImportCornerDichotomy(any(), any(), any(), any())).thenReturn(null);
        CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
        assertEquals("id", cseValidResponse.getId());
    }

    private CseValidFileResource createFileResource(String filename) {
        return new CseValidFileResource(filename, Objects.requireNonNull(getClass().getResource("/" + filename)).toExternalForm());
    }

    /* --------------- IMPORT CORNER --------------- */

    @Test
    void computeTimestampDataMissingMniiMnieMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = new TTimestamp();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMnie() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMnie();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMnieMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMnieMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampMnie() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMnie();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullExportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMniiMibniiAndAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMnii();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMniiMibniiAndAntcfinalBothZero() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMibniiAndAntcfinalBothZero();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMniiMibniiAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndAntcfinal();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMniiAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibnii();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMniiActualNtcAboveTarget() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcAboveTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).info(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.TEN);
    }

    @Test
    void computeTimestampMniiFilesNotExisting() throws CseValidRequestValidatorException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, CRAC file, GLSK file.";
        CseValidRequestValidatorException e = new CseValidRequestValidatorException(errorMessage);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        doThrow(e).when(cseValidRequestValidator).checkAllFilesExist(cseValidRequest, false);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(eq(timestamp), eq(e.getMessage()));
    }

    @Test
    void computeTimestampMniiRunDichotomyError() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, processTargetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(jsonCracUrl)).thenReturn(crac);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);

        when(dichotomyRunner.runImportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl)).thenReturn(null);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillDichotomyError(timestamp);
    }

    @Test
    void computeTimestampMniiRunDichotomySuccessHighestValidStepNull() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        TLimitingElement limitingElement = new TLimitingElement();

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, processTargetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(jsonCracUrl)).thenReturn(crac);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);

        when(dichotomyRunner.runImportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl)).thenReturn(dichotomyResult);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(null);
        when(limitingElementService.getLimitingElement(null)).thenReturn(limitingElement);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampWithDichotomyResponse(timestamp, BigDecimal.ONE, BigDecimal.ONE, limitingElement);
    }

    @Test
    void computeTimestampMniiRunDichotomySuccessHighestValidStepNotNull() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        TLimitingElement limitingElement = new TLimitingElement();

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        NetPositionReport netPositionReport = mock(NetPositionReport.class);
        DichotomyStepResult<RaoResponse> highestValidStep = mock(DichotomyStepResult.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, processTargetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(jsonCracUrl)).thenReturn(crac);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);

        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(dichotomyRunner.runImportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl)).thenReturn(dichotomyResult);

        when(limitingElementService.getLimitingElement(highestValidStep)).thenReturn(limitingElement);
        when(highestValidStep.getValidationData()).thenReturn(raoResponse);
        when(raoResponse.getNetworkWithPraFileUrl()).thenReturn("finalNetworkWithPra");
        when(netPositionService.generateNetPositionReport("finalNetworkWithPra")).thenReturn(netPositionReport);
        Map<String, Double> borderExchanges = Map.of("FR", 1.0, "CH", 2.0, "AT", 4.0, "SI", 8.0);
        when(netPositionReport.getAreasReport()).thenReturn(Map.of("IT", new AreaReport("id", 42.0, borderExchanges)));

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampWithDichotomyResponse(timestamp, BigDecimal.ONE, BigDecimal.valueOf(-15), limitingElement);
    }

    /* --------------- EXPORT CORNER --------------- */

    @Test
    void computeTimestampMiecMibiecAndAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMiecMibiecAndAntcfinalBothZero() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMibiecAndAntcfinalBothZero();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMiecMibiecAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiecAndAntcfinal();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMiecAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiecAndMibiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMiecShiftingFactorsMissing() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithoutShiftingFactors();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_SHIFTING_FACTORS);
    }

    @Test
    void computeTimestampMiecCalculationDirectionsMissing() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithoutCalculationDirections();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_CALCULATION_DIRECTIONS);
    }

    @Test
    void computeTimestampMiecActualNtcAboveTarget() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiecAndMibiecAndAntcfinalAndActualNtcAboveTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.TEN);
    }

    @Test
    void computeTimestampMiecWhithoutFranceInAreaOrOutAreaShouldThrowAnException() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithoutFranceInAreaOrOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        assertThatExceptionOfType(CseValidInvalidDataException.class)
                .isThrownBy(() -> cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter));
    }

    @Test
    void computeTimestampMiecWithFranceInAreaAndFilesNotExisting() throws CseValidRequestValidatorException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, CRAC file, GLSK file, CRAC Transit.";
        CseValidRequestValidatorException e = new CseValidRequestValidatorException(errorMessage);
        doThrow(e).when(cseValidRequestValidator).checkAllFilesExist(cseValidRequest, true);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(eq(timestamp), eq(e.getMessage()));
    }

    @Test
    void computeTimestampMiecWithFranceOutAreaAndFilesNotExisting() throws CseValidRequestValidatorException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, CRAC file, GLSK file.";
        CseValidRequestValidatorException e = new CseValidRequestValidatorException(errorMessage);
        doThrow(e).when(cseValidRequestValidator).checkAllFilesExist(cseValidRequest, false);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(eq(timestamp), eq(e.getMessage()));
    }

    @Test
    void computeTimestampMiecWithFranceInAreaShouldRunDichotomyForExportCorner() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getExportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParametersUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, processTargetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(jsonCracUrl)).thenReturn(crac);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParametersUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(false);

        when(dichotomyRunner.runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl)).thenReturn(dichotomyResult);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParametersUrl);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(dichotomyRunner, times(1)).runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersUrl);
    }

    @Test
    void computeTimestampMiecWithFranceInAreaShouldNotRunDichotomyForExportCornerBecauseNetworkShiftedIsSecure() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getExportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, processTargetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(jsonCracUrl)).thenReturn(crac);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(true);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, timestamp.getMIEC().getV());
    }

    @Test
    void computeTimestampMiecWithFranceOutAreaShouldRunDichotomyForExportCorner() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, processTargetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(jsonCracUrl)).thenReturn(crac);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(false);

        when(dichotomyRunner.runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl)).thenReturn(dichotomyResult);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(dichotomyRunner, times(1)).runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl);
    }

    @Test
    void computeTimestampMiecWithFranceOutAreaShouldNotRunDichotomyForExportCornerBecauseNetworkShiftedIsSecure() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cracUrl = cseValidRequest.getImportCrac().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();
        OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();

        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        String basePath = "IDCC/2023/01/09/12_30/ARTIFACTS/";
        String variantName = "1234";
        String networkNameOrId = "test";
        String scaledNetworkDirPath = basePath + variantName;
        String networkFilePath = scaledNetworkDirPath + networkNameOrId + ".xiidm";
        String networkFileUrl = "CSE/Valid/network.utc";
        double shiftValue = timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        Network network = mock(Network.class);
        CseCrac cseCrac = mock(CseCrac.class);
        Crac crac = mock(Crac.class);
        VariantManager variantManager = mock(VariantManager.class);
        RaoResponse raoResponse = mock(RaoResponse.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(network);
        when(fileImporter.importCseCrac(cracUrl)).thenReturn(cseCrac);
        when(fileImporter.importCrac(cseCrac, processTargetDateTime, network)).thenReturn(crac);
        when(fileImporter.importCracFromJson(jsonCracUrl)).thenReturn(crac);

        when(fileExporter.saveCracInJsonFormat(crac, processTargetDateTime, processType)).thenReturn(jsonCracUrl);
        when(fileExporter.saveRaoParameters(processTargetDateTime, processType)).thenReturn(raoParameterUrl);
        when(fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS)).thenReturn(basePath);
        when(fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType)).thenReturn(networkFileUrl);

        when(network.getNameOrId()).thenReturn(networkNameOrId);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(variantManager.getWorkingVariantId()).thenReturn(variantName);

        when(cseValidRaoValidator.runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl)).thenReturn(raoResponse);
        when(cseValidRaoValidator.isSecure(raoResponse)).thenReturn(true);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        verify(cseValidRaoValidator, times(1)).runRao(cseValidRequest, networkFileUrl, jsonCracUrl, raoParameterUrl);
        verify(cseValidRaoValidator, times(1)).isSecure(raoResponse);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, timestamp.getMIEC().getV());
    }
}

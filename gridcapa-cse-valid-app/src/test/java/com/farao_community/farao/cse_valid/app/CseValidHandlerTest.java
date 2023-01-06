/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.util.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.util.TCalculationDirectionTestData;
import com.farao_community.farao.cse_valid.app.util.TimeStampTestData;
import com.farao_community.farao.cse_valid.app.validator.CseValidRequestValidator;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterProperties;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_CALCULATION_DIRECTIONS;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_SHIFTING_FACTORS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        CseValidRequest cseValidRequest = new CseValidRequest("id",
                ProcessType.D2CC,
                OffsetDateTime.now(),
                new CseValidFileResource("ttc.xml", "file://ttc.xml"),
                new CseValidFileResource("importCrac.xml", "file://importCrac.xml"),
                new CseValidFileResource("exportCrac.xml", "file://exportCrac.xml"),
                new CseValidFileResource("cgm.xml", "file://cgm.xml"),
                new CseValidFileResource("glsk.xml", "file://glsk.xml"),
                OffsetDateTime.now());
        when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "basepath", "url", "accesskey", "secretkey"));
        when(minioAdapter.getFile(any())).thenReturn(getClass().getResourceAsStream("/doesNotExist.xml"));
        when(minioAdapter.generatePreSignedUrl(any())).thenReturn("/output.xml");
        CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
        assertNotNull(cseValidResponse);
        assertEquals("id", cseValidResponse.getId());
        assertEquals("/output.xml", cseValidResponse.getResultFileUrl());
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
        when(dichotomyRunner.runImportCornerDichotomy(any(), any())).thenReturn(null);
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
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMnie() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMnie();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMnieMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMnieMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampMnie() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMnie();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullExportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMniiMibniiAndAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMnii();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMniiMibniiAndAntcfinalBothZero() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMibniiAndAntcfinalBothZero();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMniiMibniiAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndAntcfinal();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMniiAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibnii();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMniiActualNtcAboveTarget() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcAboveTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).info(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullImportSuccess(timestamp, BigDecimal.TEN);
    }

    @Test
    void computeTimestampMniiFilesNotExisting() throws CseValidRequestValidatorException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, CRAC file, GLSK file.";
        CseValidRequestValidatorException e = new CseValidRequestValidatorException(errorMessage);
        doThrow(e).when(cseValidRequestValidator).checkAllFilesExist(cseValidRequest, null);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(eq(timestamp), eq(e.getMessage()));
    }

    @Test
    void computeTimestampMniiRunDichotomyError() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(dichotomyRunner.runImportCornerDichotomy(any(), any())).thenReturn(null);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillDichotomyError(timestamp);
    }

    @Test
    void computeTimestampMniiRunDichotomySuccessHighestValidStepNull() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);
        TLimitingElement limitingElement = new TLimitingElement();

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(null);
        when(limitingElementService.getLimitingElement(null)).thenReturn(limitingElement);
        when(dichotomyRunner.runImportCornerDichotomy(any(), any())).thenReturn(dichotomyResult);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampWithDichotomyResponse(timestamp, BigDecimal.ONE, BigDecimal.ONE, limitingElement);
    }

    @Test
    void computeTimestampMniiRunDichotomySuccessHighestValidStepNotNull() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);
        DichotomyStepResult<RaoResponse> highestValidStep = mock(DichotomyStepResult.class);
        RaoResponse raoResponse = mock(RaoResponse.class);
        TLimitingElement limitingElement = new TLimitingElement();
        NetPositionReport netPositionReport = mock(NetPositionReport.class);

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(dichotomyRunner.runImportCornerDichotomy(any(), any())).thenReturn(dichotomyResult);
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
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMiecMibiecAndAntcfinalBothZero() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMibiecAndAntcfinalBothZero();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.ZERO);
    }

    @Test
    void computeTimestampMiecMibiecAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiecAndAntcfinal();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMiecAntcfinalAbsent() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiecAndMibiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMiecShiftingFactorsMissing() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithoutShiftingFactors();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_SHIFTING_FACTORS);
    }

    @Test
    void computeTimestampMiecCalculationDirectionsMissing() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithoutCalculationDirections();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_CALCULATION_DIRECTIONS);
    }

    @Test
    void computeTimestampMiecActualNtcAboveTarget() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMiecAndMibiecAndAntcfinalAndActualNtcAboveTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, BigDecimal.TEN);
    }

    @Test
    void computeTimestampMiecWhithoutFranceInAreaOrOutAreaShouldThrowAnException() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithoutFranceInAreaOrOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        Assertions.assertThatExceptionOfType(CseValidInvalidDataException.class)
                .isThrownBy(() -> cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter));
    }

    @Test
    void computeTimestampMiecWithFranceInAreaAndFilesNotExisting() throws CseValidRequestValidatorException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

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
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

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
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);
        Network network = mock(Network.class);

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(cseValidNetworkShifter.shiftNetwork(timestamp, cseValidRequest)).thenReturn(network);
        when(cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl())).thenReturn(false);
        when(dichotomyRunner.runExportCornerDichotomy(cseValidRequest, timestampWrapper.getTimestamp(), true)).thenReturn(dichotomyResult);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(timestamp, cseValidRequest);
        verify(cseValidRaoValidator, times(1)).isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl());
        verify(dichotomyRunner, times(1)).runExportCornerDichotomy(cseValidRequest, timestampWrapper.getTimestamp(), true);
    }

    @Test
    void computeTimestampMiecWithFranceInAreaShouldNotRunDichotomyForExportCornerBecauseNetworkSHiftedIsSecure() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        Network network = mock(Network.class);

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(cseValidNetworkShifter.shiftNetwork(timestamp, cseValidRequest)).thenReturn(network);
        when(cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl())).thenReturn(true);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(timestamp, cseValidRequest);
        verify(cseValidRaoValidator, times(1)).isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl());
        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, timestamp.getMIEC().getV());
    }

    @Test
    void computeTimestampMiecWithFranceOutAreaShouldRunDichotomyForExportCorner() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);
        Network network = mock(Network.class);

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(cseValidNetworkShifter.shiftNetwork(timestamp, cseValidRequest)).thenReturn(network);
        when(cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl())).thenReturn(false);
        when(dichotomyRunner.runExportCornerDichotomy(cseValidRequest, timestampWrapper.getTimestamp(), false)).thenReturn(dichotomyResult);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(timestamp, cseValidRequest);
        verify(cseValidRaoValidator, times(1)).isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl());
        verify(dichotomyRunner, times(1)).runExportCornerDichotomy(cseValidRequest, timestampWrapper.getTimestamp(), false);
    }

    @Test
    void computeTimestampMiecWithFranceOutAreaShouldNotRunDichotomyForExportCornerBecauseNetworkSHiftedIsSecure() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        Network network = mock(Network.class);

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(cseValidNetworkShifter.shiftNetwork(timestamp, cseValidRequest)).thenReturn(network);
        when(cseValidRaoValidator.isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl())).thenReturn(true);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(cseValidNetworkShifter, times(1)).shiftNetwork(timestamp, cseValidRequest);
        verify(cseValidRaoValidator, times(1)).isNetworkSecure(network, cseValidRequest, cseValidRequest.getExportCrac().getUrl());
        verify(tcDocumentTypeWriter, times(1)).fillTimestampExportCornerSuccess(timestamp, timestamp.getMIEC().getV());
    }

    @Test
    void isCountryInAreaShouldReturnTrue() {
        String franceEic = "10YFR-RTE------C";
        List<TCalculationDirection> tCalculationDirectionList = TCalculationDirectionTestData.getTCalculationDirectionListWithFranceInArea();

        boolean isFranceInArea = CseValidHandler.isCountryInArea(franceEic, tCalculationDirectionList);

        assertTrue(isFranceInArea);
    }

    void isCountryInAreaShouldReturnFalse() {
        String franceEic = "10YFR-RTE------C";
        List<TCalculationDirection> tCalculationDirectionList = TCalculationDirectionTestData.getTCalculationDirectionListWithFranceOutArea();

        boolean isFranceInArea = CseValidHandler.isCountryInArea(franceEic, tCalculationDirectionList);

        assertFalse(isFranceInArea);
    }

    @Test
    void isCountryOutAreaShouldReturnTrue() {
        String franceEic = "10YFR-RTE------C";
        List<TCalculationDirection> tCalculationDirectionList = TCalculationDirectionTestData.getTCalculationDirectionListWithFranceOutArea();

        boolean isFranceOutArea = CseValidHandler.isCountryOutArea(franceEic, tCalculationDirectionList);

        assertTrue(isFranceOutArea);
    }

    @Test
    void isCountryOutAreaShouldReturnFalse() {
        String franceEic = "10YFR-RTE------C";
        List<TCalculationDirection> tCalculationDirectionList = TCalculationDirectionTestData.getTCalculationDirectionListWithFranceInArea();

        boolean isFranceOutArea = CseValidHandler.isCountryOutArea(franceEic, tCalculationDirectionList);

        assertFalse(isFranceOutArea);
    }
}

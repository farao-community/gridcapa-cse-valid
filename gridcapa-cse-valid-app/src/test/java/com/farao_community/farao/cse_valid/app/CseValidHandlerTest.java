/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.dichotomy.LimitingElementService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterProperties;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class CseValidHandlerTest {

    @Autowired
    CseValidHandler cseValidHandler;

    @MockBean
    Logger businessLogger;

    @MockBean
    DichotomyRunner dichotomyRunner;

    @MockBean
    MinioAdapter minioAdapter;

    @MockBean
    LimitingElementService limitingElementService;

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
                new CseValidFileResource("crac.xml", "file://crac.xml"),
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
                new CseValidFileResource("crac.xml", "file://crac.xml"),
                new CseValidFileResource("cgm.xml", "file://cgm.xml"),
                new CseValidFileResource("glsk.xml", "file://glsk.xml"),
                OffsetDateTime.of(2020, 8, 12, 22, 30, 0, 0, ZoneOffset.UTC));
        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(dichotomyRunner.runDichotomy(any(), any())).thenReturn(null);
        CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
        assertEquals("id", cseValidResponse.getId());
    }

    private CseValidFileResource createFileResource(String filename) {
        return new CseValidFileResource(filename, Objects.requireNonNull(getClass().getResource("/" + filename)).toExternalForm());
    }

    @Test
    void computeTimestampDataMissingMniiMnieMiec() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMnie() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);
        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.TEN);
        timestamp.setMNIE(mnieValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMnieMiec() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.ONE);
        timestamp.setMNIE(mnieValue);
        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMiec() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);
        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampMiec() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        // don't forget to change this when real implementation of export-corner handling will be available
        verify(tcDocumentTypeWriter, times(1)).fillTimestampForExportCorner(timestamp);
    }

    @Test
    void computeTimestampMnie() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.ZERO);
        timestamp.setMNIE(mnieValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        // don't forget to change this when real implementation of export-corner handling will be available
        verify(tcDocumentTypeWriter, times(1)).fillTimestampNoComputationNeededForFullExport(timestamp);
    }

    @Test
    void computeTimestampMniiMibniiAndAntcfinalAbsent() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampNoVerificationNeededForFullImport(timestamp);
    }

    @Test
    void computeTimestampMniiMibniiAndAntcfinalBothZero() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);
        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ZERO);
        timestamp.setMiBNII(mibniiValue);
        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampNoVerificationNeededForFullImport(timestamp);
    }

    @Test
    void computeTimestampMniiMibniiAbsent() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);
        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMniiAntcfinalAbsent() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);
        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ZERO);
        timestamp.setMiBNII(mibniiValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampMniiActualNtcAboveTarget() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);
        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.TEN);
        timestamp.setMiBNII(mibniiValue);
        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).info(anyString(), eq("time"));
        verify(tcDocumentTypeWriter, times(1)).fillTimestampNoComputationNeededForFullImport(timestamp);
    }

    @Test
    void computeTimestampMniiFilesNotAvailable() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.TEN);
        timestamp.setMNII(mniiValue);
        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ONE);
        timestamp.setMiBNII(mibniiValue);
        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(businessLogger, times(1)).error(anyString(), eq("time"));
        ArgumentCaptor<String> redFlagErrorCaptor = ArgumentCaptor.forClass(String.class);
        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(eq(timestamp), redFlagErrorCaptor.capture());
        Assertions.assertThat(redFlagErrorCaptor.getValue()).contains("CGM file", "CRAC file", "GLSK file");
    }

    @Test
    void computeTimestampMniiRunDichotomyError() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        when(cseValidRequest.getCgm()).thenReturn(new CseValidFileResource("cgm.xml", "url/to/cgm.xml"));
        when(cseValidRequest.getCrac()).thenReturn(new CseValidFileResource("crac.xml", "url/to/crac.xml"));
        when(cseValidRequest.getGlsk()).thenReturn(new CseValidFileResource("glsk.xml", "url/to/glsk.xml"));
        when(cseValidRequest.getProcessType()).thenReturn(ProcessType.IDCC);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.TEN);
        timestamp.setMNII(mniiValue);
        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ONE);
        timestamp.setMiBNII(mibniiValue);
        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(dichotomyRunner.runDichotomy(any(), any())).thenReturn(null);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillDichotomyError(timestamp);
    }

    @Test
    void computeTimestampMniiRunDichotomySuccess() {
        CseValidRequest cseValidRequest = mock(CseValidRequest.class);
        when(cseValidRequest.getCgm()).thenReturn(new CseValidFileResource("cgm.xml", "url/to/cgm.xml"));
        when(cseValidRequest.getCrac()).thenReturn(new CseValidFileResource("crac.xml", "url/to/crac.xml"));
        when(cseValidRequest.getGlsk()).thenReturn(new CseValidFileResource("glsk.xml", "url/to/glsk.xml"));
        when(cseValidRequest.getProcessType()).thenReturn(ProcessType.IDCC);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);
        TTimestamp timestamp = new TTimestamp();
        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);
        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.TEN);
        timestamp.setMNII(mniiValue);
        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ONE);
        timestamp.setMiBNII(mibniiValue);
        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);
        DichotomyResult<RaoResponse> dichotomyResult = mock(DichotomyResult.class);
        DichotomyStepResult<RaoResponse> highestValidStep = mock(DichotomyStepResult.class);
        TLimitingElement limitingElement = new TLimitingElement();

        when(minioAdapter.fileExists(any())).thenReturn(true);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(limitingElementService.getLimitingElement(highestValidStep)).thenReturn(limitingElement);
        when(dichotomyRunner.runDichotomy(any(), any())).thenReturn(dichotomyResult);

        cseValidHandler.computeTimestamp(timestamp, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampWithDichotomyResponse(timestamp, dichotomyResult, limitingElement);
    }
}

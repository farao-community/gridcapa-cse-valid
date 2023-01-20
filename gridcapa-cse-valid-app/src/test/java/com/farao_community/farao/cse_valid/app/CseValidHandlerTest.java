/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.service.ComputeExportCornerService;
import com.farao_community.farao.cse_valid.app.service.ComputeFullImportService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.utils.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.utils.TimestampTestData;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private FileImporter fileImporter;

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private Logger businessLogger;

    @MockBean
    private ComputeFullImportService computeFullImportService;

    @MockBean
    private ComputeExportCornerService computeExportCornerService;

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Autowired
    private EicCodesMapper eicCodesMapper;

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
        CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
        assertEquals("id", cseValidResponse.getId());
    }

    private CseValidFileResource createFileResource(String filename) {
        return new CseValidFileResource(filename, Objects.requireNonNull(getClass().getResource("/" + filename)).toExternalForm());
    }

    /* ------------------- computeTimestamp ------------------- */

    @Test
    void computeTimestampDataMissingMniiMnieMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = new TTimestamp();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMnie() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMnie();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMnieMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMnieMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampContradictoryDataMniiMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampError(timestamp, ERROR_MSG_CONTRADICTORY_DATA);
    }

    @Test
    void computeTimestampMnii() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMnii();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(computeFullImportService, times(1)).computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
    }

    @Test
    void computeTimestampMiec() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMiec();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(computeExportCornerService, times(1)).computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
    }

    @Test
    void computeTimestampMnie() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        TTimestamp timestamp = TimestampTestData.getTimestampWithMnie();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        TcDocumentTypeWriter tcDocumentTypeWriter = mock(TcDocumentTypeWriter.class);

        cseValidHandler.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);

        verify(tcDocumentTypeWriter, times(1)).fillTimestampFullExportSuccess(timestamp, BigDecimal.ZERO);
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTTCLimitedBy;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import xsd.etso_core_cmpts.QuantityType;
import xsd.etso_core_cmpts.TextType;
import xsd.etso_core_cmpts.TimeIntervalType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_GENERIC;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_TTC_ADJ_FILE;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class TcDocumentTypeWriterTest {
    private TcDocumentTypeWriter tcDocumentTypeWriter;
    private TcDocumentType tcDocumentType;

    private void initTcDocumentTypeWriter(CseValidRequest processRequest) {
        tcDocumentTypeWriter = new TcDocumentTypeWriter(processRequest);
        tcDocumentType = Mockito.mock(TcDocumentType.class);
        ReflectionTestUtils.setField(tcDocumentTypeWriter, "tcDocumentType", this.tcDocumentType);
    }

    private static CseValidRequest initCseValidRequest() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        OffsetDateTime time = OffsetDateTime.now();
        return new CseValidRequest(null, ProcessType.D2CC, timestamp, null, null, null, null, null, time);
    }

    @Test
    void fillNoTtcAdjustmentError() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillNoTtcAdjustmentError(cseValidRequest);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0).getSTATUS().getV()).isEqualTo(BigInteger.ZERO);
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0).getRedFlagReason().getV()).isEqualTo(ERROR_MSG_MISSING_TTC_ADJ_FILE);
    }

    @Test
    void fillTimestampError() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampError(initialTs, "test");

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(resultTs.getSTATUS().getV()).isEqualTo(BigInteger.ZERO);
        assertions.assertThat(resultTs.getRedFlagReason().getV()).isEqualTo("test");
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertAll();
    }

    @Test
    void fillTimestampFullImportSuccess() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType mnii = new QuantityType();
        mnii.setV(BigDecimal.TEN);
        initialTs.setMNII(mnii);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampFullImportSuccess(initialTs, BigDecimal.TEN);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getMNII().getV()).isEqualTo(BigDecimal.TEN);
        assertions.assertAll();
    }

    @Test
    void fillDichotomyError() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillDichotomyError(initialTs);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(resultTs.getSTATUS().getV()).isEqualTo(BigInteger.ONE);
        assertions.assertThat(resultTs.getRedFlagReason().getV()).isEqualTo(ERROR_MSG_GENERIC);
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertAll();
    }

    @Test
    void fillTimestampFullExportSuccess() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType mnie = new QuantityType();
        mnie.setV(BigDecimal.ONE);
        initialTs.setMNIE(mnie);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampFullExportSuccess(initialTs, BigDecimal.ONE);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getMNIE().getV()).isEqualTo(BigDecimal.ONE);
        assertions.assertAll();
    }

    @Test
    void fillTimestampExportCornerSuccess() {
        // Temporary test : should be updated when real handling of export-corner will be available
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType miec = new QuantityType();
        miec.setV(BigDecimal.ONE);
        initialTs.setMIEC(miec);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampExportCornerSuccess(initialTs, BigDecimal.ONE);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getMIEC().getV()).isEqualTo(BigDecimal.ONE);
        assertions.assertAll();
    }

    @Test
    void fillTimestampWithFullImportDichotomyResponse() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType mibnii = new QuantityType();
        mibnii.setV(BigDecimal.ONE);
        initialTs.setMiBNII(mibnii);
        QuantityType mnii = new QuantityType();
        mnii.setV(BigDecimal.TEN);
        initialTs.setMNII(mnii);
        TLimitingElement tLimitingElement = new TLimitingElement();
        initialTs.setLimitingElement(tLimitingElement);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampWithFullImportDichotomyResponse(initialTs, BigDecimal.ONE, BigDecimal.TEN, tLimitingElement);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getLimitingElement()).isEqualTo(tLimitingElement);
        assertions.assertThat(resultTs.getMiBNII().getV()).isEqualTo(BigDecimal.ONE);
        assertions.assertThat(resultTs.getMNII().getV()).isEqualTo(BigDecimal.TEN);
        assertions.assertAll();
    }

    @Test
    void fillTimestampWithExportCornerDichotomyResponseWithFranceInArea() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        TLimitingElement tLimitingElement = new TLimitingElement();
        initialTs.setLimitingElement(tLimitingElement);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampWithExportCornerDichotomyResponse(initialTs, tLimitingElement, BigDecimal.TEN, true);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getLimitingElement()).isEqualTo(tLimitingElement);
        assertions.assertThat(resultTs.getNTCvalues().getNTCvalueExport().get(0).getNTC().getV()).isEqualTo(BigDecimal.TEN);
        assertions.assertThat(resultTs.getNTCvalues().getNTCvalueExport().get(0).getCountry().getV()).isEqualTo("FR");
        assertions.assertThat(resultTs.getNTCvalues().getNTCvalueImport()).isEmpty();
        assertions.assertAll();
    }

    @Test
    void fillTimestampWithExportCornerDichotomyResponseWithFranceOutArea() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        initTcDocumentTypeWriter(cseValidRequest);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        TLimitingElement tLimitingElement = new TLimitingElement();
        initialTs.setLimitingElement(tLimitingElement);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampWithExportCornerDichotomyResponse(initialTs, tLimitingElement, BigDecimal.TEN, false);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getLimitingElement()).isEqualTo(tLimitingElement);
        assertions.assertThat(resultTs.getNTCvalues().getNTCvalueImport().get(0).getNTC().getV()).isEqualTo(BigDecimal.TEN);
        assertions.assertThat(resultTs.getNTCvalues().getNTCvalueImport().get(0).getCountry().getV()).isEqualTo("FR");
        assertions.assertThat(resultTs.getNTCvalues().getNTCvalueExport().isEmpty());
        assertions.assertAll();
    }

    private static void initTimeDataInTimestamp(TTimestamp initialTs) {
        TimeIntervalType timeInterval = new TimeIntervalType();
        timeInterval.setV("timeInterval");
        initialTs.setTimeInterval(timeInterval);
        TTime timeTs = new TTime();
        timeTs.setV("time");
        initialTs.setTime(timeTs);
        TTime referenceCalculationTime = new TTime();
        referenceCalculationTime.setV("referenceCalculationTime");
        initialTs.setReferenceCalculationTime(referenceCalculationTime);
    }

    private static void initSuccessDataInTimestamp(TTimestamp initialTs) {
        TTTCLimitedBy ttcLimitedBy = new TTTCLimitedBy();
        ttcLimitedBy.setV("ttcLimitedBy");
        initialTs.setTTCLimitedBy(ttcLimitedBy);
        TextType cracFile = new TextType();
        cracFile.setV("cracFile");
        initialTs.setCRACfile(cracFile);
        TextType cgmFile = new TextType();
        cgmFile.setV("cgmFile");
        initialTs.setCGMfile(cgmFile);
        TextType gskFile = new TextType();
        gskFile.setV("gskFile");
        initialTs.setGSKfile(gskFile);
        TLimitingElement limitingElement = new TLimitingElement();
        initialTs.setLimitingElement(limitingElement);
    }

    private static void assertTimeDataValidInResultTimestamp(TTimestamp resultTs, SoftAssertions assertions) {
        assertions.assertThat(resultTs.getTimeInterval().getV()).isEqualTo("timeInterval");
        assertions.assertThat(resultTs.getTime().getV()).isEqualTo("time");
        assertions.assertThat(resultTs.getReferenceCalculationTime().getV()).isEqualTo("referenceCalculationTime");
    }

    private static void assertSuccessDataValidInResultTimestamp(TTimestamp resultTs, SoftAssertions assertions) {
        assertions.assertThat(resultTs.getSTATUS().getV()).isEqualTo(BigInteger.TWO);
        assertions.assertThat(resultTs.getTTCLimitedBy().getV()).isEqualTo("ttcLimitedBy");
        assertions.assertThat(resultTs.getCRACfile().getV()).isEqualTo("cracFile");
        assertions.assertThat(resultTs.getCGMfile().getV()).isEqualTo("cgmFile");
        assertions.assertThat(resultTs.getGSKfile().getV()).isEqualTo("gskFile");
        assertions.assertThat(resultTs.getLimitingElement()).isNotNull();
    }
}

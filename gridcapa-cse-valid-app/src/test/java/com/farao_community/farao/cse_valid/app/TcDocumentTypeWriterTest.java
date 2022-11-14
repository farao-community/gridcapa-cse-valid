/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
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

@SpringBootTest
class TcDocumentTypeWriterTest {
    private TcDocumentTypeWriter tcDocumentTypeWriter;
    private TcDocumentType tcDocumentType;

    private void initTcDocumentTypeWriter(CseValidRequest processRequest, NetPositionService netPositionService) {
        tcDocumentTypeWriter = new TcDocumentTypeWriter(processRequest, netPositionService);
        tcDocumentType = Mockito.mock(TcDocumentType.class);
        ReflectionTestUtils.setField(tcDocumentTypeWriter, "tcDocumentType", this.tcDocumentType);
    }

    private static CseValidRequest initCseValidRequest() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        OffsetDateTime time = OffsetDateTime.now();
        return new CseValidRequest(null, ProcessType.D2CC, timestamp, null, null, null, null, time);
    }

    @Test
    void fillNoTtcAdjustmentError() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        NetPositionService netPositionService = new NetPositionService(null);
        initTcDocumentTypeWriter(cseValidRequest, netPositionService);
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
        NetPositionService netPositionService = new NetPositionService(null);
        initTcDocumentTypeWriter(cseValidRequest, netPositionService);
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
    void fillTimestampNoVerificationNeededForFullImport() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        NetPositionService netPositionService = new NetPositionService(null);
        initTcDocumentTypeWriter(cseValidRequest, netPositionService);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType mnii = new QuantityType();
        mnii.setV(BigDecimal.TEN);
        initialTs.setMNII(mnii);
        TextType basecaseFile = new TextType();
        basecaseFile.setV("basecaseFile");
        initialTs.setBASECASEfile(basecaseFile);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampNoVerificationNeededForFullImport(initialTs);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getBASECASEfile().getV()).isEqualTo("basecaseFile");
        assertions.assertThat(resultTs.getMNII().getV()).isEqualTo(BigDecimal.TEN);
        assertions.assertAll();
    }

    @Test
    void fillTimestampNoComputationNeededForFullImport() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        NetPositionService netPositionService = new NetPositionService(null);
        initTcDocumentTypeWriter(cseValidRequest, netPositionService);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType mnii = new QuantityType();
        mnii.setV(BigDecimal.ZERO);
        initialTs.setMNII(mnii);
        QuantityType mibnii = new QuantityType();
        mibnii.setV(BigDecimal.TEN);
        initialTs.setMiBNII(mibnii);
        QuantityType antcfinal = new QuantityType();
        antcfinal.setV(BigDecimal.ONE);
        initialTs.setANTCFinal(antcfinal);
        TextType basecaseFile = new TextType();
        basecaseFile.setV("basecaseFile");
        initialTs.setBASECASEfile(basecaseFile);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampNoComputationNeededForFullImport(initialTs);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getBASECASEfile().getV()).isEqualTo("basecaseFile");
        assertions.assertThat(resultTs.getMNII().getV()).isEqualTo(BigDecimal.valueOf(9));
        assertions.assertAll();
    }

    @Test
    void fillDichotomyError() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        NetPositionService netPositionService = new NetPositionService(null);
        initTcDocumentTypeWriter(cseValidRequest, netPositionService);
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
    void fillTimestampNoComputationNeededForFullExport() {
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        NetPositionService netPositionService = new NetPositionService(null);
        initTcDocumentTypeWriter(cseValidRequest, netPositionService);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType mnie = new QuantityType();
        mnie.setV(BigDecimal.ONE);
        initialTs.setMNIE(mnie);
        TextType basecaseFile = new TextType();
        basecaseFile.setV("basecaseFile");
        initialTs.setBASECASEfile(basecaseFile);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampNoComputationNeededForFullExport(initialTs);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getBASECASEfile().getV()).isEqualTo("basecaseFile");
        assertions.assertThat(resultTs.getMNIE().getV()).isEqualTo(BigDecimal.ONE);
        assertions.assertAll();
    }

    @Test
    void fillTimestampForExportCorner() {
        // Temporary test : should be updated when real handling of export-corner will be available
        // CseValidRequest
        CseValidRequest cseValidRequest = initCseValidRequest();
        NetPositionService netPositionService = new NetPositionService(null);
        initTcDocumentTypeWriter(cseValidRequest, netPositionService);
        // TTimestamp
        TTimestamp initialTs = new TTimestamp();
        initTimeDataInTimestamp(initialTs);
        initSuccessDataInTimestamp(initialTs);
        QuantityType miec = new QuantityType();
        miec.setV(BigDecimal.ONE);
        initialTs.setMIEC(miec);
        TextType basecaseFile = new TextType();
        basecaseFile.setV("basecaseFile");
        initialTs.setBASECASEfile(basecaseFile);
        // Mock
        Mockito.when(tcDocumentType.getValidationResults()).thenReturn(new ArrayList<>());

        tcDocumentTypeWriter.fillTimestampForExportCorner(initialTs);

        Assertions.assertThat(this.tcDocumentType.getValidationResults()).isNotEmpty();
        Assertions.assertThat(this.tcDocumentType.getValidationResults().get(0).getTimestamp()).isNotEmpty();
        TTimestamp resultTs = this.tcDocumentType.getValidationResults().get(0).getTimestamp().get(0);
        SoftAssertions assertions = new SoftAssertions();
        assertTimeDataValidInResultTimestamp(resultTs, assertions);
        assertSuccessDataValidInResultTimestamp(resultTs, assertions);
        assertions.assertThat(resultTs.getBASECASEfile().getV()).isEqualTo("basecaseFile");
        assertions.assertThat(resultTs.getMIEC().getV()).isEqualTo(BigDecimal.ONE);
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

package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirections;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;

class TTimestampWrapperTest {
    private TTimestamp timestamp;
    private TTimestampWrapper timestampWrapper;

    @BeforeEach
    void initTimestampWrapper() {
        timestamp = new TTimestamp();
        timestamp.setTime(new TTime());
        timestamp.getTime().setV("timeValue");
        timestamp.setReferenceCalculationTime(new TTime());
        timestamp.getReferenceCalculationTime().setV("referenceCalculationTimeValue");
        timestampWrapper = new TTimestampWrapper(timestamp);
    }

    private void initTimestampFullImport() {
        timestamp.setMNII(new QuantityType());
        timestamp.getMNII().setV(BigDecimal.TEN);
        timestamp.setMiBNII(new QuantityType());
        timestamp.getMiBNII().setV(BigDecimal.ONE);
        timestamp.setANTCFinal(new QuantityType());
        timestamp.getANTCFinal().setV(BigDecimal.valueOf(2));
    }

    private void initTimestampFullExport() {
        timestamp.setMNIE(new QuantityType());
        timestamp.getMNIE().setV(BigDecimal.TEN);
        timestamp.setANTCFinal(new QuantityType());
        timestamp.getANTCFinal().setV(BigDecimal.valueOf(2));
    }

    private void initTimestampExportCorner() {
        timestamp.setMIEC(new QuantityType());
        timestamp.getMIEC().setV(BigDecimal.TEN);
        timestamp.setMiBIEC(new QuantityType());
        timestamp.getMiBIEC().setV(BigDecimal.ONE);
        timestamp.setANTCFinal(new QuantityType());
        timestamp.getANTCFinal().setV(BigDecimal.valueOf(2));
    }

    @Test
    void getTimestamp() {
        Assertions.assertThat(timestampWrapper.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void hasMniiTrue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.hasMnii()).isTrue();
    }

    @Test
    void hasMniiFalse() {
        Assertions.assertThat(timestampWrapper.hasMnii()).isFalse();

        timestamp.setMNII(new QuantityType());
        Assertions.assertThat(timestampWrapper.hasMnii()).isFalse();
    }

    @Test
    void hasMnieTrue() {
        initTimestampFullExport();
        Assertions.assertThat(timestampWrapper.hasMnie()).isTrue();
    }

    @Test
    void hasMnieFalse() {
        Assertions.assertThat(timestampWrapper.hasMnie()).isFalse();

        timestamp.setMNIE(new QuantityType());
        Assertions.assertThat(timestampWrapper.hasMnie()).isFalse();
    }

    @Test
    void hasMiecTrue() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.hasMiec()).isTrue();
    }

    @Test
    void hasMiecFalse() {
        Assertions.assertThat(timestampWrapper.hasMiec()).isFalse();

        timestamp.setMIEC(new QuantityType());
        Assertions.assertThat(timestampWrapper.hasMiec()).isFalse();
    }

    @Test
    void hasMibniiTrue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.hasMibnii()).isTrue();
    }

    @Test
    void hasMibniiFalse() {
        Assertions.assertThat(timestampWrapper.hasMibnii()).isFalse();
    }

    @Test
    void hasMibiecTrue() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.hasMibiec()).isTrue();
    }

    @Test
    void hasMibiecFalse() {
        Assertions.assertThat(timestampWrapper.hasMibiec()).isFalse();
    }

    @Test
    void hasAntcfinalTrue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.hasAntcfinal()).isTrue();
    }

    @Test
    void hasAntcfinalFalse() {
        Assertions.assertThat(timestampWrapper.hasAntcfinal()).isFalse();
    }

    @Test
    void hasShiftingFactorsTrue() {
        timestamp.setShiftingFactors(new TShiftingFactors());
        timestamp.getShiftingFactors().getShiftingFactor().add(new TFactor());
        Assertions.assertThat(timestampWrapper.hasShiftingFactors()).isTrue();
    }

    @Test
    void hasShiftingFactorsFalse() {
        Assertions.assertThat(timestampWrapper.hasShiftingFactors()).isFalse();

        timestamp.setShiftingFactors(new TShiftingFactors());
        Assertions.assertThat(timestampWrapper.hasShiftingFactors()).isFalse();
    }

    @Test
    void hasCalculationDirectionsTrue() {
        timestamp.setCalculationDirections(new TCalculationDirections());
        timestamp.getCalculationDirections().getCalculationDirection().add(new TCalculationDirection());
        Assertions.assertThat(timestampWrapper.hasCalculationDirections()).isTrue();
    }

    @Test
    void hasCalculationDirectionsFalse() {
        Assertions.assertThat(timestampWrapper.hasCalculationDirections()).isFalse();

        timestamp.setCalculationDirections(new TCalculationDirections());
        Assertions.assertThat(timestampWrapper.hasCalculationDirections()).isFalse();
    }

    @Test
    void hasNoneOfMniiMnieMiecTrue() {
        Assertions.assertThat(timestampWrapper.hasNoneOfMniiMnieMiec()).isTrue();
    }

    @Test
    void hasNoneOfMniiMnieMiecFalseFullImport() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.hasNoneOfMniiMnieMiec()).isFalse();
    }

    @Test
    void hasNoneOfMniiMnieMiecFalseExportCorner() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.hasNoneOfMniiMnieMiec()).isFalse();
    }

    @Test
    void hasNoneOfMniiMnieMiecFalseFullExport() {
        initTimestampFullExport();
        Assertions.assertThat(timestampWrapper.hasNoneOfMniiMnieMiec()).isFalse();
    }

    @Test
    void hasMultipleMniiMnieMiecTrueMniiMiec() {
        initTimestampFullImport();
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.hasMultipleMniiMnieMiec()).isTrue();
    }

    @Test
    void hasMultipleMniiMnieMiecTrueMnieMiec() {
        initTimestampFullExport();
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.hasMultipleMniiMnieMiec()).isTrue();
    }

    @Test
    void hasMultipleMniiMnieMiecTrueMniiMnie() {
        initTimestampFullImport();
        initTimestampFullExport();
        Assertions.assertThat(timestampWrapper.hasMultipleMniiMnieMiec()).isTrue();
    }

    @Test
    void hasMultipleMniiMnieMiecFalseMnii() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.hasMultipleMniiMnieMiec()).isFalse();
    }

    @Test
    void hasMultipleMniiMnieMiecFalseMnie() {
        initTimestampFullExport();
        Assertions.assertThat(timestampWrapper.hasMultipleMniiMnieMiec()).isFalse();
    }

    @Test
    void hasMultipleMniiMnieMiecFalseMiec() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.hasMultipleMniiMnieMiec()).isFalse();
    }

    @Test
    void getTimeValue() {
        Assertions.assertThat(timestampWrapper.getTimeValue()).isEqualTo("timeValue");
    }

    @Test
    void getReferenceCalculationTimeValue() {
        Assertions.assertThat(timestampWrapper.getReferenceCalculationTimeValue()).isEqualTo("referenceCalculationTimeValue");
    }

    @Test
    void getMibnii() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getMibnii()).isNotNull();
        Assertions.assertThat(timestampWrapper.getMibnii().getV()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void getMibiec() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.getMibiec()).isNotNull();
        Assertions.assertThat(timestampWrapper.getMibiec().getV()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void getAntcfinal() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getAntcfinal()).isNotNull();
        Assertions.assertThat(timestampWrapper.getAntcfinal().getV()).isEqualTo(BigDecimal.valueOf(2));
    }

    @Test
    void getMniiValue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getMniiValue()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void getMiecValue() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.getMiecValue()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void getMnieValue() {
        initTimestampFullExport();
        Assertions.assertThat(timestampWrapper.getMnieValue()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void getMibniiValue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getMibniiValue()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void getMibiecValue() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.getMibiecValue()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void getAntcfinalValue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getAntcfinalValue()).isEqualTo(BigDecimal.valueOf(2));
    }

    @Test
    void getMniiIntValue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getMniiIntValue()).isEqualTo(10);
    }

    @Test
    void getMiecIntValue() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.getMiecIntValue()).isEqualTo(10);
    }

    @Test
    void getMibniiIntValue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getMibniiIntValue()).isEqualTo(1);
    }

    @Test
    void getMibiecIntValue() {
        initTimestampExportCorner();
        Assertions.assertThat(timestampWrapper.getMibiecIntValue()).isEqualTo(1);
    }

    @Test
    void getAntcfinalIntValue() {
        initTimestampFullImport();
        Assertions.assertThat(timestampWrapper.getAntcfinalIntValue()).isEqualTo(2);
    }
}

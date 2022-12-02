package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;

public class TTimestampWrapper {
    private final TTimestamp timestamp;

    // Timestamp
    public TTimestampWrapper(TTimestamp timestamp) {
        this.timestamp = timestamp;
    }

    public TTimestamp getTimestamp() {
        return timestamp;
    }

    // Values checkers
    public boolean hasMnii() {
        return timestamp.getMNII() != null && timestamp.getMNII().getV() != null;
    }

    public boolean hasMnie() {
        return timestamp.getMNIE() != null && timestamp.getMNIE().getV() != null;
    }

    public boolean hasMiec() {
        return timestamp.getMIEC() != null && timestamp.getMIEC().getV() != null;
    }

    public boolean hasMibnii() {
        return timestamp.getMiBNII() != null && timestamp.getMiBNII().getV() != null;
    }

    public boolean hasMibiec() {
        return timestamp.getMiBIEC() != null && timestamp.getMiBIEC().getV() != null;
    }

    public boolean hasAntcfinal() {
        return timestamp.getANTCFinal() != null && timestamp.getANTCFinal().getV() != null;
    }

    public boolean hasShiftingFactors() {
        return timestamp.getShiftingFactors() != null
                && timestamp.getShiftingFactors().getShiftingFactor() != null
                && !timestamp.getShiftingFactors().getShiftingFactor().isEmpty();
    }

    public boolean hasCalculationDirections() {
        return timestamp.getCalculationDirections() != null
                && timestamp.getCalculationDirections().getCalculationDirection() != null
                && !timestamp.getCalculationDirections().getCalculationDirection().isEmpty();
    }

    public boolean hasNoneOfMniiMnieMiec() {
        return !hasMnii() && !hasMnie() && !hasMiec();
    }

    public boolean hasMultipleMniiMnieMiec() {
        // simultaneous presence of at least two values among MNII (full import), MIEC (export-corner) and MNIE (full export)
        return (hasMnii() && hasMnie())
                || (hasMnii() && hasMiec())
                || (hasMnie() && hasMiec());
    }

    // Getters for "natural" values
    public String getTimeValue() {
        return timestamp.getTime().getV();
    }

    public String getReferenceCalculationTimeValue() {
        return timestamp.getReferenceCalculationTime().getV();
    }

    public QuantityType getMibnii() {
        return timestamp.getMiBNII();
    }

    public QuantityType getMibiec() {
        return timestamp.getMiBIEC();
    }

    public QuantityType getAntcfinal() {
        return timestamp.getANTCFinal();
    }

    // Getters for BigDecimal values
    public BigDecimal getMniiValue() {
        return timestamp.getMNII().getV();
    }

    public BigDecimal getMiecValue() {
        return timestamp.getMIEC().getV();
    }

    public BigDecimal getMnieValue() {
        return timestamp.getMNIE().getV();
    }

    public BigDecimal getMibniiValue() {
        return timestamp.getMiBNII().getV();
    }

    public BigDecimal getMibiecValue() {
        return timestamp.getMiBIEC().getV();
    }

    public BigDecimal getAntcfinalValue() {
        return timestamp.getANTCFinal().getV();
    }

    // Getters for int values
    public int getMniiIntValue() {
        return timestamp.getMNII().getV().intValue();
    }

    public int getMiecIntValue() {
        return timestamp.getMIEC().getV().intValue();
    }

    public int getMibniiIntValue() {
        return timestamp.getMiBNII().getV().intValue();
    }

    public int getMibiecIntValue() {
        return timestamp.getMiBIEC().getV().intValue();
    }

    public int getAntcfinalIntValue() {
        return timestamp.getANTCFinal().getV().intValue();
    }
}

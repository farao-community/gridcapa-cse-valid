package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.powsybl.iidm.network.Country;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TTimestampWrapper {
    private final TTimestamp timestamp;
    private final EicCodesConfiguration eicCodesConfiguration;
    private Map<String, Boolean> exportingCountryMap;

    // Timestamp
    public TTimestampWrapper(TTimestamp timestamp, EicCodesConfiguration eicCodesConfiguration) {
        this.timestamp = timestamp;
        this.eicCodesConfiguration = eicCodesConfiguration;
        initExportingCountryMap();
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
                && !timestamp.getCalculationDirections().isEmpty()
                && timestamp.getCalculationDirections().get(0).getCalculationDirection() != null
                && !timestamp.getCalculationDirections().get(0).getCalculationDirection().isEmpty();
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

    //

    private void initExportingCountryMap() {
        if (hasCalculationDirections()) {
            exportingCountryMap = new HashMap<>();
            List<TCalculationDirection> calculationDirections = timestamp.getCalculationDirections().get(0).getCalculationDirection();
            calculationDirections.forEach(tCalculationDirection -> {
                if (tCalculationDirection.getInArea().getV().equals(eicCodesConfiguration.getItaly())) {
                    exportingCountryMap.put(tCalculationDirection.getOutArea().getV(), false);
                } else if (tCalculationDirection.getOutArea().getV().equals(eicCodesConfiguration.getItaly())) {
                    exportingCountryMap.put(tCalculationDirection.getInArea().getV(), true);
                }
            });
        }
    }

    public Boolean isFranceExporting() {
        return exportingCountryMap.get(eicCodesConfiguration.getFrance());
    }

    public Map<String, Double> getImportCornerSplittingFactors() {
        TSplittingFactors tSplittingFactors = timestamp.getSplittingFactors();
        Map<String, Double> splittingFactorsMap = tSplittingFactors.getSplittingFactor().stream()
                .collect(Collectors.toMap(
                    tFactor -> toEic(tFactor.getCountry().getV()),
                    tFactor -> tFactor.getFactor().getV().doubleValue()
                ));
        splittingFactorsMap.put(toEic(Country.IT), -1.);
        return splittingFactorsMap;
    }

    public Map<String, Double> getExportCornerSplittingFactors() {
        TShiftingFactors tShiftingFactors = timestamp.getShiftingFactors();
        return tShiftingFactors.getShiftingFactor().stream()
                .collect(Collectors.toMap(
                    tFactor -> toEic(tFactor.getCountry().getV()),
                    tFactor -> tFactor.getFactor().getV().doubleValue() * getFactorSignOfCountry(tFactor.getCountry().getV())
                ));
    }

    public Map<String, Double> getExportCornerSplittingFactorsMapReduceToFranceAndItaly() {
        Map<String, Double> result = new HashMap<>();
        double franceFactor = isFranceExporting() ? -1.0 : 1.0;
        result.put(toEic(Country.FR), franceFactor);
        result.put(toEic(Country.IT), franceFactor * -1);
        return result;
    }

    private double getFactorSignOfCountry(String country) {
        String countryEic = toEic(country);
        Boolean isCountryExporting = exportingCountryMap.get(countryEic);
        if (isCountryExporting == null) {
            throw new CseValidInvalidDataException("Country " + country + " must appear in InArea or OutArea");
        }
        return isCountryExporting ? -1 : 1;
    }

    private String toEic(String country) {
        return toEic(Country.valueOf(country));
    }

    private String toEic(Country country) {
        return new EICode(country).getAreaCode();
    }
}

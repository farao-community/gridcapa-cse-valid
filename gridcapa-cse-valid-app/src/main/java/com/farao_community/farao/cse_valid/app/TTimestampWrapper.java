/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.powsybl.iidm.network.Country;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public class TTimestampWrapper {
    private final TTimestamp timestamp;
    private final EicCodesConfiguration eicCodesConfiguration;
    private Map<String, Boolean> exportingCountryMap;

    // Timestamp
    public TTimestampWrapper(TTimestamp timestamp, EicCodesConfiguration eicCodesConfiguration) {
        this.timestamp = timestamp;
        this.eicCodesConfiguration = eicCodesConfiguration;
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

    Map<String, Boolean> getExportCornerActiveForCountryMap() {
        if (exportingCountryMap == null) {
            exportingCountryMap = new HashMap<>();
            List<TCalculationDirection> calculationDirections = timestamp.getCalculationDirections().get(0).getCalculationDirection();
            calculationDirections.forEach(tCalculationDirection -> {
                if (tCalculationDirection.getInArea().getV().equals(eicCodesConfiguration.getItaly())) {
                    exportingCountryMap.put(tCalculationDirection.getOutArea().getV(), false);
                } else if (tCalculationDirection.getOutArea().getV().equals(eicCodesConfiguration.getItaly())) {
                    exportingCountryMap.put(tCalculationDirection.getInArea().getV(), true);
                }
            });
            exportingCountryMap.put(toEic(Country.IT), true);
        }
        return exportingCountryMap;
    }

    public boolean isExportCornerActiveForCountry(String countryEic) {
        getExportCornerActiveForCountryMap();
        return Optional.ofNullable(exportingCountryMap.get(countryEic))
                .orElseThrow(() -> new CseValidInvalidDataException("Country " + countryEic + " must appear in InArea or OutArea"));
    }

    public boolean isExportCornerActiveForFrance() {
        return isExportCornerActiveForCountry(eicCodesConfiguration.getFrance());
    }

    public Map<String, Double> getImportCornerSplittingFactors() {
        return timestamp.getSplittingFactors().getSplittingFactor().stream()
                .collect(Collectors.toMap(
                    tFactor -> toEic(tFactor.getCountry().getV()),
                    tFactor -> tFactor.getFactor().getV().doubleValue()
                ));
    }

    public Map<String, Double> getExportCornerSplittingFactors() {
        getExportCornerActiveForCountryMap();
        return timestamp.getShiftingFactors().getShiftingFactor().stream()
                .collect(Collectors.toMap(
                    tFactor -> toEic(tFactor.getCountry().getV()),
                    tFactor -> tFactor.getFactor().getV().doubleValue())
                );
    }

    private String toEic(String country) {
        return toEic(Country.valueOf(country));
    }

    private String toEic(Country country) {
        return new EICode(country).getAreaCode();
    }
}

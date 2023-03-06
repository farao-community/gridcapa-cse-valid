/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.utils;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.CountryType;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public final class TSplittingAndShiftingFactorsTestData {

    private TSplittingAndShiftingFactorsTestData() {
    }

    public static TSplittingFactors getTSplittingFactors() {
        TSplittingFactors tSplittingFactors = new TSplittingFactors();

        tSplittingFactors.getSplittingFactor().add(getTFactor(BigDecimal.valueOf(0.4), "FR"));
        tSplittingFactors.getSplittingFactor().add(getTFactor(BigDecimal.valueOf(0.2), "AT"));
        tSplittingFactors.getSplittingFactor().add(getTFactor(BigDecimal.valueOf(0.3), "SI"));
        tSplittingFactors.getSplittingFactor().add(getTFactor(BigDecimal.valueOf(0.1), "CH"));

        return tSplittingFactors;
    }

    public static TShiftingFactors getTShiftingFactorsWithFranceInArea() {
        TShiftingFactors tShiftingFactors = new TShiftingFactors();

        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.7), "IT"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.2), "AT"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.1), "SI"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.4), "CH"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.6), "FR"));

        return tShiftingFactors;
    }

    public static TShiftingFactors getTShiftingFactorsWithFranceOutArea() {
        TShiftingFactors tShiftingFactors = new TShiftingFactors();

        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.7), "FR"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.2), "AT"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.1), "SI"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.4), "CH"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.valueOf(0.6), "IT"));

        return tShiftingFactors;
    }

    private static TFactor getTFactor(BigDecimal factor, String country) {
        TFactor tFactor = new TFactor();

        QuantityType quantityType = new QuantityType();
        quantityType.setV(factor);
        tFactor.setFactor(quantityType);

        CountryType countryType = new CountryType();
        countryType.setV(country);
        tFactor.setCountry(countryType);

        return tFactor;
    }
}

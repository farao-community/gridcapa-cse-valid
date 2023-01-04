/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.util;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.CountryType;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public final class TSplittingAndShifttingFactorsTestData {

    private TSplittingAndShifttingFactorsTestData() {
    }

    public static TSplittingFactors getTSplittingFactors() {
        TSplittingFactors tSplittingFactors = new TSplittingFactors();

        tSplittingFactors.getSplittingFactor().add(getTFactor(BigDecimal.ONE, "FR"));
        tSplittingFactors.getSplittingFactor().add(getTFactor(BigDecimal.TEN, "IT"));

        return tSplittingFactors;
    }

    public static TShiftingFactors getTShiftingFactors() {
        TShiftingFactors tShiftingFactors = new TShiftingFactors();

        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.ONE, "FR"));
        tShiftingFactors.getShiftingFactor().add(getTFactor(BigDecimal.TEN, "IT"));

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

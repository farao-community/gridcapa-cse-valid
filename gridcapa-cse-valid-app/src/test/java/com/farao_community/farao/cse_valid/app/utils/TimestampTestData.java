/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.utils;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirections;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import xsd.etso_code_lists.CodingSchemeType;
import xsd.etso_core_cmpts.AreaType;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.cse_valid.app.Constants.IN_AREA;
import static com.farao_community.farao.cse_valid.app.Constants.OUT_AREA;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public final class TimestampTestData {

    private TimestampTestData() {
    }

    /* --------------- FULL IMPORT --------------- */

    public static TTimestamp getTimestampWithMniiAndMnie() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);

        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.TEN);
        timestamp.setMNIE(mnieValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMnieMiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.ONE);
        timestamp.setMNIE(mnieValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMniiAndMiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMnie() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.ZERO);
        timestamp.setMNIE(mnieValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMnii() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMibniiAndAntcfinalBothZero() {
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

        return timestamp;
    }

    public static TTimestamp getTimestampWithMniiAndAntcfinal() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMniiAndMibnii() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ZERO);
        timestamp.setMiBNII(mibniiValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcAboveTarget() {
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

        return timestamp;
    }

    public static TTimestamp getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget() {
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

        TSplittingFactors tSplittingFactors = TSplittingAndShiftingFactorsTestData.getTSplittingFactors();
        timestamp.setSplittingFactors(tSplittingFactors);

        return timestamp;
    }

    /* --------------- EXPORT CORNER --------------- */

    public static TTimestamp getTimestampWithMiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMibiecAndAntcfinalBothZero() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ZERO);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMiecAndAntcfinal() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMiecAndMibiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ZERO);
        timestamp.setMiBIEC(mibiecValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithoutShiftingFactors() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ONE);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.TEN);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimestampWithoutCalculationDirections() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ONE);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.TEN);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = new TShiftingFactors();
        shiftingFactors.getShiftingFactor().add(new TFactor());
        timestamp.setShiftingFactors(shiftingFactors);

        return timestamp;
    }

    public static TTimestamp getTimestampWithMiecAndMibiecAndAntcfinalAndActualNtcAboveTarget() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ONE);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.TEN);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = new TShiftingFactors();
        shiftingFactors.getShiftingFactor().add(new TFactor());
        timestamp.setShiftingFactors(shiftingFactors);

        TCalculationDirections calculationDirections = new TCalculationDirections();
        calculationDirections.getCalculationDirection().add(new TCalculationDirection());
        timestamp.getCalculationDirections().add(calculationDirections);

        return timestamp;
    }

    public static TTimestamp getTimestampWithoutFranceInAreaOrOutArea() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ONE);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = new TShiftingFactors();
        shiftingFactors.getShiftingFactor().add(new TFactor());
        timestamp.setShiftingFactors(shiftingFactors);

        AreaType inArea = new AreaType();
        inArea.setV(IN_AREA);
        inArea.setCodingScheme(CodingSchemeType.A_01);

        AreaType outArea = new AreaType();
        outArea.setV(OUT_AREA);
        outArea.setCodingScheme(CodingSchemeType.A_01);

        TCalculationDirection tCalculationDirection = new TCalculationDirection();
        tCalculationDirection.setInArea(inArea);
        tCalculationDirection.setOutArea(outArea);

        TCalculationDirections tCalculationDirections = new TCalculationDirections();
        List<TCalculationDirection> tCalculationDirectionList = tCalculationDirections.getCalculationDirection();
        tCalculationDirectionList.add(tCalculationDirection);

        List<TCalculationDirections> tCalculationDirectionsList = timestamp.getCalculationDirections();
        tCalculationDirectionsList.add(tCalculationDirections);

        return timestamp;
    }

    public static TTimestamp getTimestampWithFranceInArea() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ONE);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = TSplittingAndShiftingFactorsTestData.getTShiftingFactorsWithFranceInArea();
        timestamp.setShiftingFactors(shiftingFactors);

        TCalculationDirections tCalculationDirections = new TCalculationDirections();
        List<TCalculationDirection> tCalculationDirectionList = tCalculationDirections.getCalculationDirection();
        tCalculationDirectionList.addAll(TCalculationDirectionTestData.getTCalculationDirectionListWithFranceInArea());

        List<TCalculationDirections> tCalculationDirectionsList = timestamp.getCalculationDirections();
        tCalculationDirectionsList.add(tCalculationDirections);

        return timestamp;
    }

    public static TTimestamp getTimestampWithFranceOutArea() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ONE);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = TSplittingAndShiftingFactorsTestData.getTShiftingFactorsWithFranceOutArea();
        timestamp.setShiftingFactors(shiftingFactors);

        TCalculationDirections tCalculationDirections = new TCalculationDirections();
        List<TCalculationDirection> tCalculationDirectionList = tCalculationDirections.getCalculationDirection();
        tCalculationDirectionList.addAll(TCalculationDirectionTestData.getTCalculationDirectionListWithFranceOutArea());

        List<TCalculationDirections> tCalculationDirectionsList = timestamp.getCalculationDirections();
        tCalculationDirectionsList.add(tCalculationDirections);

        return timestamp;
    }

    public static Map<String, Boolean> getCountryImportingFromItalyMapWithFranceInArea() {
        Map<String, Boolean> exportingCountryMap = new HashMap<>();
        exportingCountryMap.put("10YCH-SWISSGRIDZ", false);
        exportingCountryMap.put("10YAT-APG------L", false);
        exportingCountryMap.put("10YSI-ELES-----O", false);
        exportingCountryMap.put("10YFR-RTE------C", true);
        exportingCountryMap.put("10YIT-GRTN-----B", true);
        return exportingCountryMap;
    }

    public static Map<String, Boolean> getCountryImportingFromItalyMapWithFranceOutArea() {
        Map<String, Boolean> exportingCountryMap = new HashMap<>();
        exportingCountryMap.put("10YFR-RTE------C", false);
        exportingCountryMap.put("10YAT-APG------L", false);
        exportingCountryMap.put("10YSI-ELES-----O", false);
        exportingCountryMap.put("10YCH-SWISSGRIDZ", true);
        exportingCountryMap.put("10YIT-GRTN-----B", true);
        return exportingCountryMap;
    }

    public static Map<String, Double> getSplittingFactorsForFullImportWithoutItaly() {
        Map<String, Double> importCornerSplittingFactors = new HashMap<>();
        importCornerSplittingFactors.put("10YAT-APG------L", 0.2);
        importCornerSplittingFactors.put("10YSI-ELES-----O", 0.3);
        importCornerSplittingFactors.put("10YCH-SWISSGRIDZ", 0.1);
        importCornerSplittingFactors.put("10YFR-RTE------C", 0.4);
        return importCornerSplittingFactors;
    }

    public static Map<String, Double> getSplittingFactorsForFullImportWithItaly() {
        Map<String, Double> importCornerSplittingFactors = getSplittingFactorsForFullImportWithoutItaly();
        importCornerSplittingFactors.put("10YIT-GRTN-----B", -1.0);
        return importCornerSplittingFactors;
    }

    public static Map<String, Double> getSplittingFactorsForExportCornerWithAllCountriesAndFranceInAreaWithoutSign() {
        Map<String, Double> exportCornerSplittingFactors = new HashMap<>();
        exportCornerSplittingFactors.put("10YCH-SWISSGRIDZ", 0.7);
        exportCornerSplittingFactors.put("10YAT-APG------L", 0.2);
        exportCornerSplittingFactors.put("10YSI-ELES-----O", 0.1);
        exportCornerSplittingFactors.put("10YFR-RTE------C", 0.4);
        exportCornerSplittingFactors.put("10YIT-GRTN-----B", 0.6);
        return exportCornerSplittingFactors;
    }

    public static Map<String, Double> getSplittingFactorsForExportCornerWithAllCountriesAndFranceInAreaWithSign() {
        Map<String, Double> exportCornerSplittingFactors = new HashMap<>();
        exportCornerSplittingFactors.put("10YCH-SWISSGRIDZ", 0.7);
        exportCornerSplittingFactors.put("10YAT-APG------L", 0.2);
        exportCornerSplittingFactors.put("10YSI-ELES-----O", 0.1);
        exportCornerSplittingFactors.put("10YFR-RTE------C", -0.4);
        exportCornerSplittingFactors.put("10YIT-GRTN-----B", -0.6);
        return exportCornerSplittingFactors;
    }

    public static Map<String, Double> getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutAreaWithoutSign() {
        Map<String, Double> exportCornerSplittingFactors = new HashMap<>();
        exportCornerSplittingFactors.put("10YFR-RTE------C", 0.7);
        exportCornerSplittingFactors.put("10YAT-APG------L", 0.2);
        exportCornerSplittingFactors.put("10YSI-ELES-----O", 0.1);
        exportCornerSplittingFactors.put("10YCH-SWISSGRIDZ", 0.4);
        exportCornerSplittingFactors.put("10YIT-GRTN-----B", 0.6);
        return exportCornerSplittingFactors;
    }

    public static Map<String, Double> getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutAreaWithSign() {
        Map<String, Double> exportCornerSplittingFactors = new HashMap<>();
        exportCornerSplittingFactors.put("10YFR-RTE------C", 0.7);
        exportCornerSplittingFactors.put("10YAT-APG------L", 0.2);
        exportCornerSplittingFactors.put("10YSI-ELES-----O", 0.1);
        exportCornerSplittingFactors.put("10YCH-SWISSGRIDZ", -0.4);
        exportCornerSplittingFactors.put("10YIT-GRTN-----B", -0.6);
        return exportCornerSplittingFactors;
    }

    public static Map<String, Double> getSplittingFactorsForExportCornerWithItalyFranceAndFranceInArea() {
        Map<String, Double> importCornerSplittingFactors = new HashMap<>();
        importCornerSplittingFactors.put("10YFR-RTE------C", -1.0);
        importCornerSplittingFactors.put("10YIT-GRTN-----B", 1.0);
        return importCornerSplittingFactors;
    }

    public static Map<String, Double> getSplittingFactorsForExportCornerWithItalyFranceAndFranceOutArea() {
        Map<String, Double> importCornerSplittingFactors = new HashMap<>();
        importCornerSplittingFactors.put("10YFR-RTE------C", 1.0);
        importCornerSplittingFactors.put("10YIT-GRTN-----B", -1.0);
        return importCornerSplittingFactors;
    }
}

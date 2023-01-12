/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirections;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.util.TimeStampTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class TTimestampWrapperTest {
    private TTimestamp timestamp;
    private TTimestampWrapper timestampWrapper;

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @BeforeEach
    void initTimestampWrapper() {
        timestamp = new TTimestamp();
        timestamp.setTime(new TTime());
        timestamp.getTime().setV("timeValue");
        timestamp.setReferenceCalculationTime(new TTime());
        timestamp.getReferenceCalculationTime().setV("referenceCalculationTimeValue");
        timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
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
        assertEquals(timestampWrapper.getTimestamp(), timestamp);
    }

    @Test
    void hasMniiTrue() {
        initTimestampFullImport();
        assertTrue(timestampWrapper.hasMnii());
    }

    @Test
    void hasMniiFalse() {
        assertFalse(timestampWrapper.hasMnii());

        timestamp.setMNII(new QuantityType());
        assertFalse(timestampWrapper.hasMnii());
    }

    @Test
    void hasMnieTrue() {
        initTimestampFullExport();
        assertTrue(timestampWrapper.hasMnie());
    }

    @Test
    void hasMnieFalse() {
        assertFalse(timestampWrapper.hasMnie());

        timestamp.setMNIE(new QuantityType());
        assertFalse(timestampWrapper.hasMnie());
    }

    @Test
    void hasMiecTrue() {
        initTimestampExportCorner();
        assertTrue(timestampWrapper.hasMiec());
    }

    @Test
    void hasMiecFalse() {
        assertFalse(timestampWrapper.hasMiec());

        timestamp.setMIEC(new QuantityType());
        assertFalse(timestampWrapper.hasMiec());
    }

    @Test
    void hasMibniiTrue() {
        initTimestampFullImport();
        assertTrue(timestampWrapper.hasMibnii());
    }

    @Test
    void hasMibniiFalse() {
        assertFalse(timestampWrapper.hasMibnii());
    }

    @Test
    void hasMibiecTrue() {
        initTimestampExportCorner();
        assertTrue(timestampWrapper.hasMibiec());
    }

    @Test
    void hasMibiecFalse() {
        assertFalse(timestampWrapper.hasMibiec());
    }

    @Test
    void hasAntcfinalTrue() {
        initTimestampFullImport();
        assertTrue(timestampWrapper.hasAntcfinal());
    }

    @Test
    void hasAntcfinalFalse() {
        assertFalse(timestampWrapper.hasAntcfinal());
    }

    @Test
    void hasShiftingFactorsTrue() {
        timestamp.setShiftingFactors(new TShiftingFactors());
        timestamp.getShiftingFactors().getShiftingFactor().add(new TFactor());
        assertTrue(timestampWrapper.hasShiftingFactors());
    }

    @Test
    void hasShiftingFactorsFalse() {
        assertFalse(timestampWrapper.hasShiftingFactors());

        timestamp.setShiftingFactors(new TShiftingFactors());
        assertFalse(timestampWrapper.hasShiftingFactors());
    }

    @Test
    void hasCalculationDirectionsTrue() {
        timestamp.getCalculationDirections().add(new TCalculationDirections());
        timestamp.getCalculationDirections().get(0).getCalculationDirection().add(new TCalculationDirection());
        assertTrue(timestampWrapper.hasCalculationDirections());
    }

    @Test
    void hasCalculationDirectionsFalse() {
        assertFalse(timestampWrapper.hasCalculationDirections());

        timestamp.getCalculationDirections().add(new TCalculationDirections());
        assertFalse(timestampWrapper.hasCalculationDirections());
    }

    @Test
    void hasNoneOfMniiMnieMiecTrue() {
        assertTrue(timestampWrapper.hasNoneOfMniiMnieMiec());
    }

    @Test
    void hasNoneOfMniiMnieMiecFalseFullImport() {
        initTimestampFullImport();
        assertFalse(timestampWrapper.hasNoneOfMniiMnieMiec());
    }

    @Test
    void hasNoneOfMniiMnieMiecFalseExportCorner() {
        initTimestampExportCorner();
        assertFalse(timestampWrapper.hasNoneOfMniiMnieMiec());
    }

    @Test
    void hasNoneOfMniiMnieMiecFalseFullExport() {
        initTimestampFullExport();
        assertFalse(timestampWrapper.hasNoneOfMniiMnieMiec());
    }

    @Test
    void hasMultipleMniiMnieMiecTrueMniiMiec() {
        initTimestampFullImport();
        initTimestampExportCorner();
        assertTrue(timestampWrapper.hasMultipleMniiMnieMiec());
    }

    @Test
    void hasMultipleMniiMnieMiecTrueMnieMiec() {
        initTimestampFullExport();
        initTimestampExportCorner();
        assertTrue(timestampWrapper.hasMultipleMniiMnieMiec());
    }

    @Test
    void hasMultipleMniiMnieMiecTrueMniiMnie() {
        initTimestampFullImport();
        initTimestampFullExport();
        assertTrue(timestampWrapper.hasMultipleMniiMnieMiec());
    }

    @Test
    void hasMultipleMniiMnieMiecFalseMnii() {
        initTimestampFullImport();
        assertFalse(timestampWrapper.hasMultipleMniiMnieMiec());
    }

    @Test
    void hasMultipleMniiMnieMiecFalseMnie() {
        initTimestampFullExport();
        assertFalse(timestampWrapper.hasMultipleMniiMnieMiec());
    }

    @Test
    void hasMultipleMniiMnieMiecFalseMiec() {
        initTimestampExportCorner();
        assertFalse(timestampWrapper.hasMultipleMniiMnieMiec());
    }

    @Test
    void getTimeValue() {
        assertEquals(timestampWrapper.getTimeValue(), "timeValue");
    }

    @Test
    void getReferenceCalculationTimeValue() {
        assertEquals(timestampWrapper.getReferenceCalculationTimeValue(), "referenceCalculationTimeValue");
    }

    @Test
    void getMibnii() {
        initTimestampFullImport();
        assertNotNull(timestampWrapper.getMibnii());
        assertEquals(timestampWrapper.getMibnii().getV(), BigDecimal.ONE);
    }

    @Test
    void getMibiec() {
        initTimestampExportCorner();
        assertNotNull(timestampWrapper.getMibiec());
        assertEquals(timestampWrapper.getMibiec().getV(), BigDecimal.ONE);
    }

    @Test
    void getAntcfinal() {
        initTimestampFullImport();
        assertNotNull(timestampWrapper.getAntcfinal());
        assertEquals(timestampWrapper.getAntcfinal().getV(), BigDecimal.valueOf(2));
    }

    @Test
    void getMniiValue() {
        initTimestampFullImport();
        assertEquals(timestampWrapper.getMniiValue(), BigDecimal.TEN);
    }

    @Test
    void getMiecValue() {
        initTimestampExportCorner();
        assertEquals(timestampWrapper.getMiecValue(), BigDecimal.TEN);
    }

    @Test
    void getMnieValue() {
        initTimestampFullExport();
        assertEquals(timestampWrapper.getMnieValue(), BigDecimal.TEN);
    }

    @Test
    void getMibniiValue() {
        initTimestampFullImport();
        assertEquals(timestampWrapper.getMibniiValue(), BigDecimal.ONE);
    }

    @Test
    void getMibiecValue() {
        initTimestampExportCorner();
        assertEquals(timestampWrapper.getMibiecValue(), BigDecimal.ONE);
    }

    @Test
    void getAntcfinalValue() {
        initTimestampFullImport();
        assertEquals(timestampWrapper.getAntcfinalValue(), BigDecimal.valueOf(2));
    }

    @Test
    void getMniiIntValue() {
        initTimestampFullImport();
        assertEquals(timestampWrapper.getMniiIntValue(), 10);
    }

    @Test
    void getMiecIntValue() {
        initTimestampExportCorner();
        assertEquals(timestampWrapper.getMiecIntValue(), 10);
    }

    @Test
    void getMibniiIntValue() {
        initTimestampFullImport();
        assertEquals(timestampWrapper.getMibniiIntValue(), 1);
    }

    @Test
    void getMibiecIntValue() {
        initTimestampExportCorner();
        assertEquals(timestampWrapper.getMibiecIntValue(), 1);
    }

    @Test
    void getAntcfinalIntValue() {
        initTimestampFullImport();
        assertEquals(timestampWrapper.getAntcfinalIntValue(), 2);
    }

    /* ------------------- getCountryImportingFromItalyMap ------------------- */

    @Test
    void getCountryImportingFromItalyMapWithFranceInArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        Map<String, Boolean> expected = TimeStampTestData.getCountryImportingFromItalyMapWithFranceInArea();
        Map<String, Boolean> exportingCountryMap = timestampWrapper.getCountryImportingFromItalyMap();

        assertEquals(expected, exportingCountryMap);
    }

    @Test
    void getCountryImportingFromItalyMapWithFranceOutArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        Map<String, Boolean> expected = TimeStampTestData.getCountryImportingFromItalyMapWithFranceOutArea();
        Map<String, Boolean> exportingCountryMap = timestampWrapper.getCountryImportingFromItalyMap();

        assertEquals(expected, exportingCountryMap);
    }

    /* ------------------- isCountryImportingFromItaly ------------------- */

    @Test
    void isCountryImportingFromItalyShouldReturnTrue() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        boolean isCountryExporting = timestampWrapper.isCountryImportingFromItaly(eicCodesConfiguration.getFrance());

        assertTrue(isCountryExporting);
    }

    @Test
    void isCountryImportingFromItalyShouldReturnFalse() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        boolean isCountryExporting = timestampWrapper.isCountryImportingFromItaly(eicCodesConfiguration.getFrance());

        assertFalse(isCountryExporting);
    }

    @Test
    void isCountryImportingFromItalyShouldReturnThrowCseValidInvalidDataException() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);
        String countryEic = "falseCountry";
        String errorMessage = "Country " + countryEic + " must appear in InArea or OutArea";

        CseValidInvalidDataException thrown = assertThrows(CseValidInvalidDataException.class, () -> {
            timestampWrapper.isCountryImportingFromItaly(countryEic);
        }, "CseValidInvalidDataException error was expected");

        assertEquals(errorMessage, thrown.getMessage());
    }

    /* ------------------- isFranceImportingFromItaly ------------------- */

    @Test
    void isFranceImportingFromItalyShouldReturnTrue() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        boolean isFranceExporting = timestampWrapper.isFranceImportingFromItaly();

        assertTrue(isFranceExporting);
    }

    @Test
    void isFranceImportingFromItalyShouldReturnFalse() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        boolean isFranceExporting = timestampWrapper.isFranceImportingFromItaly();

        assertFalse(isFranceExporting);
    }

    /* ------------------- getImportCornerSplittingFactors ------------------- */

    @Test
    void getImportCornerSplittingFactors() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForFullImportWithoutItaly();
        Map<String, Double> splittingFactorsMap = timestampWrapper.getImportCornerSplittingFactors();

        assertEquals(expected, splittingFactorsMap);
    }

    /* ------------------- getExportCornerSplittingFactors ------------------- */

    @Test
    void getExportCornerSplittingFactorsWithFranceInArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceInAreaWithoutSign();
        Map<String, Double> splittingFactorsMap = timestampWrapper.getExportCornerSplittingFactors();

        assertEquals(expected, splittingFactorsMap);
    }

    @Test
    void getExportCornerSplittingFactorsWithFranceOutArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutAreaWithoutSign();
        Map<String, Double> splittingFactorsMap = timestampWrapper.getExportCornerSplittingFactors();

        assertEquals(expected, splittingFactorsMap);
    }
}

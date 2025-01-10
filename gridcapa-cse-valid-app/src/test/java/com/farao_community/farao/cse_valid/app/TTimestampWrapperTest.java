/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirections;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.utils.TimestampTestData;
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

    @Autowired
    private EicCodesMapper eicCodesMapper;

    @BeforeEach
    void initTimestampWrapper() {
        timestamp = new TTimestamp();
        timestamp.setTime(new TTime());
        timestamp.getTime().setV("timeValue");
        timestamp.setReferenceCalculationTime(new TTime());
        timestamp.getReferenceCalculationTime().setV("referenceCalculationTimeValue");
        timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);
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
        assertEquals("timeValue", timestampWrapper.getTimeValue());
    }

    @Test
    void getReferenceCalculationTimeValue() {
        assertEquals("referenceCalculationTimeValue", timestampWrapper.getReferenceCalculationTimeValue());
    }

    @Test
    void getMibnii() {
        initTimestampFullImport();
        assertNotNull(timestampWrapper.getMibnii());
        assertEquals(BigDecimal.ONE, timestampWrapper.getMibnii().getV());
    }

    @Test
    void getMibiec() {
        initTimestampExportCorner();
        assertNotNull(timestampWrapper.getMibiec());
        assertEquals(BigDecimal.ONE, timestampWrapper.getMibiec().getV());
    }

    @Test
    void getAntcfinal() {
        initTimestampFullImport();
        assertNotNull(timestampWrapper.getAntcfinal());
        assertEquals(BigDecimal.valueOf(2), timestampWrapper.getAntcfinal().getV());
    }

    @Test
    void getMniiValue() {
        initTimestampFullImport();
        assertEquals(BigDecimal.TEN, timestampWrapper.getMniiValue());
    }

    @Test
    void getMiecValue() {
        initTimestampExportCorner();
        assertEquals(BigDecimal.TEN, timestampWrapper.getMiecValue());
    }

    @Test
    void getMnieValue() {
        initTimestampFullExport();
        assertEquals(BigDecimal.TEN, timestampWrapper.getMnieValue());
    }

    @Test
    void getMibniiValue() {
        initTimestampFullImport();
        assertEquals(BigDecimal.ONE, timestampWrapper.getMibniiValue());
    }

    @Test
    void getMibiecValue() {
        initTimestampExportCorner();
        assertEquals(BigDecimal.ONE, timestampWrapper.getMibiecValue());
    }

    @Test
    void getAntcfinalValue() {
        initTimestampFullImport();
        assertEquals(BigDecimal.valueOf(2), timestampWrapper.getAntcfinalValue());
    }

    @Test
    void getMniiIntValue() {
        initTimestampFullImport();
        assertEquals(10, timestampWrapper.getMniiIntValue());
    }

    @Test
    void getMiecIntValue() {
        initTimestampExportCorner();
        assertEquals(10, timestampWrapper.getMiecIntValue());
    }

    @Test
    void getMibniiIntValue() {
        initTimestampFullImport();
        assertEquals(1, timestampWrapper.getMibniiIntValue());
    }

    @Test
    void getMibiecIntValue() {
        initTimestampExportCorner();
        assertEquals(1, timestampWrapper.getMibiecIntValue());
    }

    @Test
    void getAntcfinalIntValue() {
        initTimestampFullImport();
        assertEquals(2, timestampWrapper.getAntcfinalIntValue());
    }

    /* ------------------- getCountryImportingFromItalyMap ------------------- */

    @Test
    void getCountryImportingFromItalyMapWithFranceInArea() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Boolean> expected = TimestampTestData.getCountryImportingFromItalyMapWithFranceInArea();
        Map<String, Boolean> exportingCountryMap = customTimestampWrapper.getCountryImportingFromItalyMap();

        assertEquals(expected, exportingCountryMap);
    }

    @Test
    void getCountryImportingFromItalyMapWithFranceOutArea() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Boolean> expected = TimestampTestData.getCountryImportingFromItalyMapWithFranceOutArea();
        Map<String, Boolean> exportingCountryMap = customTimestampWrapper.getCountryImportingFromItalyMap();

        assertEquals(expected, exportingCountryMap);
    }

    /* ------------------- isCountryImportingFromItaly ------------------- */

    @Test
    void isCountryImportingFromItalyShouldReturnTrue() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        boolean isCountryExporting = customTimestampWrapper.isCountryImportingFromItaly(eicCodesConfiguration.getFrance());

        assertTrue(isCountryExporting);
    }

    @Test
    void isCountryImportingFromItalyShouldReturnFalse() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        boolean isCountryExporting = customTimestampWrapper.isCountryImportingFromItaly(eicCodesConfiguration.getFrance());

        assertFalse(isCountryExporting);
    }

    @Test
    void isCountryImportingFromItalyShouldReturnThrowCseValidInvalidDataException() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);
        String countryEic = "falseCountry";
        String errorMessage = "Country " + countryEic + " must appear in InArea or OutArea";

        CseValidInvalidDataException thrown = assertThrows(CseValidInvalidDataException.class, () -> {
            customTimestampWrapper.isCountryImportingFromItaly(countryEic);
        }, "CseValidInvalidDataException error was expected");

        assertEquals(errorMessage, thrown.getMessage());
    }

    /* ------------------- isFranceImportingFromItaly ------------------- */

    @Test
    void isFranceImportingFromItalyShouldReturnTrue() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        boolean isFranceExporting = customTimestampWrapper.isFranceImportingFromItaly();

        assertTrue(isFranceExporting);
    }

    @Test
    void isFranceImportingFromItalyShouldReturnFalse() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        boolean isFranceExporting = customTimestampWrapper.isFranceImportingFromItaly();

        assertFalse(isFranceExporting);
    }

    /* ------------------- getImportCornerSplittingFactors ------------------- */

    @Test
    void getImportCornerSplittingFactors() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForFullImportWithoutItaly();
        Map<String, Double> splittingFactorsMap = customTimestampWrapper.getImportCornerSplittingFactors();

        assertEquals(expected, splittingFactorsMap);
    }

    /* ------------------- getExportCornerSplittingFactors ------------------- */

    @Test
    void getExportCornerSplittingFactorsWithFranceInArea() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceInAreaWithoutSign();
        Map<String, Double> splittingFactorsMap = customTimestampWrapper.getExportCornerSplittingFactors();

        assertEquals(expected, splittingFactorsMap);
    }

    @Test
    void getExportCornerSplittingFactorsWithFranceOutArea() {
        TTimestamp customTimestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper customTimestampWrapper = new TTimestampWrapper(customTimestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutAreaWithoutSign();
        Map<String, Double> splittingFactorsMap = customTimestampWrapper.getExportCornerSplittingFactors();

        assertEquals(expected, splittingFactorsMap);
    }
}

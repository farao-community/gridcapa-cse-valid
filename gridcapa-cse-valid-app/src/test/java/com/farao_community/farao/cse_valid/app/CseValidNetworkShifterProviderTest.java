/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.utils.TimestampTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class CseValidNetworkShifterProviderTest {

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Autowired
    private EicCodesMapper eicCodesMapper;

    @MockitoBean
    private FileImporter fileImporter;

    @MockitoSpyBean
    private CseValidNetworkShifterProvider cseValidNetworkShifterProvider;

    /* ------------------- getSplittingFactorsForFullImport ------------------- */

    @Test
    void getSplittingFactorsForFullImport() {
        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForFullImportWithItaly();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifterProvider.getSplittingFactorsForFullImport(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    /* ------------------- getSplittingFactorsForExportCornerWithItalyFrance ------------------- */

    @Test
    void getSplittingFactorsForExportCornerWithItalyFranceAndFranceInArea() {
        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForExportCornerWithItalyFranceAndFranceInArea();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifterProvider.getSplittingFactorsForExportCornerWithItalyFrance(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    @Test
    void getSplittingFactorsForExportCornerWithItalyFranceAndFranceOutArea() {
        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForExportCornerWithItalyFranceAndFranceOutArea();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifterProvider.getSplittingFactorsForExportCornerWithItalyFrance(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    /* ------------------- getSplittingFactorsForExportCornerWithAllCountries ------------------- */

    @Test
    void getSplittingFactorsForExportCornerWithAllCountriesAndFranceInArea() {
        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceInAreaWithSign();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifterProvider.getSplittingFactorsForExportCornerWithAllCountries(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    @Test
    void getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutArea() {
        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimestampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutAreaWithSign();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifterProvider.getSplittingFactorsForExportCornerWithAllCountries(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }
}

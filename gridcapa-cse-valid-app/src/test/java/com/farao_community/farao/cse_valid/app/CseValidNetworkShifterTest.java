/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.utils.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.utils.TimeStampTestData;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class CseValidNetworkShifterTest {

    private static final String GLSK_FILE_URL = "file://glsk.xml";

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Autowired
    private EicCodesMapper eicCodesMapper;

    @MockBean
    private FileImporter fileImporter;

    @SpyBean
    private CseValidNetworkShifter cseValidNetworkShifter;

    /* ------------------- getSplittingFactorsForFullImport ------------------- */

    @Test
    void getSplittingFactorsForFullImport() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForFullImportWithItaly();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifter.getSplittingFactorsForFullImport(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    /* ------------------- getSplittingFactorsForExportCornerWithItalyFrance ------------------- */

    @Test
    void getSplittingFactorsForExportCornerWithItalyFranceAndFranceInArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForExportCornerWithItalyFranceAndFranceInArea();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifter.getSplittingFactorsForExportCornerWithItalyFrance(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    @Test
    void getSplittingFactorsForExportCornerWithItalyFranceAndFranceOutArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForExportCornerWithItalyFranceAndFranceOutArea();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifter.getSplittingFactorsForExportCornerWithItalyFrance(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    /* ------------------- getSplittingFactorsForExportCornerWithAllCountries ------------------- */

    @Test
    void getSplittingFactorsForExportCornerWithAllCountriesAndFranceInArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceInAreaWithSign();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifter.getSplittingFactorsForExportCornerWithAllCountries(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    @Test
    void getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutArea() {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        Map<String, Double> expected = TimeStampTestData.getSplittingFactorsForExportCornerWithAllCountriesAndFranceOutAreaWithSign();
        Map<String, Double> splittingFactorsMap = cseValidNetworkShifter.getSplittingFactorsForExportCornerWithAllCountries(timestampWrapper);

        assertEquals(expected, splittingFactorsMap);
    }

    /* ------------------- shiftNetwork ------------------- */

    @Test
    void shiftNetwork() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        double shiftValue = 1000.0;

        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterForExportCornerWithAllCountries(timestampWrapper, network, glskUrl);

        cseValidNetworkShifter.shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void shiftNetworkShouldThrowCseValidShiftFailureExceptionBecauseGlskLimitationExceptionWasThrownWhenShifting() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        double shiftValue = 1000.0;

        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        doThrow(GlskLimitationException.class).when(networkShifter).shiftNetwork(shiftValue, network);
        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterForExportCornerWithAllCountries(timestampWrapper, network, glskUrl);

        assertThrows(CseValidShiftFailureException.class, () -> {
            cseValidNetworkShifter.shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        }, "CseValidShiftFailureException error was expected");

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void shiftNetworkShouldThrowCseValidShiftFailureExceptionBecauseShiftingExceptionWasThrownWhenShifting() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        double shiftValue = 1000.0;

        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        doThrow(ShiftingException.class).when(networkShifter).shiftNetwork(shiftValue, network);
        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterForExportCornerWithAllCountries(timestampWrapper, network, glskUrl);

        assertThrows(CseValidShiftFailureException.class, () -> {
            cseValidNetworkShifter.shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        }, "CseValidShiftFailureException error was expected");

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }
}

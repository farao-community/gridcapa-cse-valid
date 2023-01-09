/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.cse_valid.app.util.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.util.TimeStampTestData;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class CseValidNetworkShifterTest {

    private static final String GLSK_FILE_URL = "file://glsk.xml";

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @MockBean
    private FileImporter fileImporter;

    @SpyBean
    private CseValidNetworkShifter cseValidNetworkShifter;

    /* ------------------- getNetworkShifterWithSplittingFactors ------------------- */

    @Test
    void getNetworkShifterWithSplittingFactors() {
        Network network = mock(Network.class);
        GlskDocument glskDocument = mock(GlskDocument.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        when(glskDocument.getZonalScalable(network)).thenReturn(null);
        when(fileImporter.importGlsk(GLSK_FILE_URL)).thenReturn(glskDocument);
        when(timestampWrapper.getImportCornerSplittingFactors()).thenReturn(TimeStampTestData.getImportCornerSplittingFactors());

        NetworkShifter networkShifter = cseValidNetworkShifter.getNetworkShifterWithSplittingFactors(timestampWrapper, network, GLSK_FILE_URL);

        verify(fileImporter, times(1)).importGlsk(GLSK_FILE_URL);
        verify(glskDocument, times(1)).getZonalScalable(network);
        assertNotNull(networkShifter);
    }

    /* ------------------- getNetworkShifterReduceToFranceAndItaly ------------------- */

    @Test
    void getNetworkShifterReduceToFranceAndItalyWithFranceInArea() {
        Network network = mock(Network.class);
        GlskDocument glskDocument = mock(GlskDocument.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        when(glskDocument.getZonalScalable(network)).thenReturn(null);
        when(fileImporter.importGlsk(GLSK_FILE_URL)).thenReturn(glskDocument);
        when(timestampWrapper.getExportCornerSplittingFactorsMapReduceToFranceAndItaly()).thenReturn(TimeStampTestData.getExportCornerSplittingFactorsMapReduceToFranceAndItalyWithFranceInArea());

        NetworkShifter networkShifter = cseValidNetworkShifter.getNetworkShifterReduceToFranceAndItaly(timestampWrapper, network, GLSK_FILE_URL);

        verify(fileImporter, times(1)).importGlsk(GLSK_FILE_URL);
        verify(glskDocument, times(1)).getZonalScalable(network);
        assertNotNull(networkShifter);
    }

    @Test
    void getNetworkShifterReduceToFranceAndItalyWithFranceOutArea() {
        Network network = mock(Network.class);
        GlskDocument glskDocument = mock(GlskDocument.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        when(glskDocument.getZonalScalable(network)).thenReturn(null);
        when(fileImporter.importGlsk(GLSK_FILE_URL)).thenReturn(glskDocument);
        when(timestampWrapper.getExportCornerSplittingFactorsMapReduceToFranceAndItaly()).thenReturn(TimeStampTestData.getExportCornerSplittingFactorsMapReduceToFranceAndItalyWithFranceOutArea());

        NetworkShifter networkShifter = cseValidNetworkShifter.getNetworkShifterReduceToFranceAndItaly(timestampWrapper, network, GLSK_FILE_URL);

        verify(fileImporter, times(1)).importGlsk(GLSK_FILE_URL);
        verify(glskDocument, times(1)).getZonalScalable(network);
        assertNotNull(networkShifter);
    }

    /* ------------------- getNetworkShiftedWithShiftingFactors ------------------- */

    @Test
    void getNetworkShiftedWithShiftingFactorsWithFranceInArea() {
        Network network = mock(Network.class);
        GlskDocument glskDocument = mock(GlskDocument.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        when(glskDocument.getZonalScalable(network)).thenReturn(null);
        when(fileImporter.importGlsk(GLSK_FILE_URL)).thenReturn(glskDocument);
        when(timestampWrapper.getExportCornerSplittingFactors()).thenReturn(TimeStampTestData.getExportCornerSplittingFactorsWithFranceInArea());

        NetworkShifter networkShifter = cseValidNetworkShifter.getNetworkShifterWithShiftingFactors(timestampWrapper, network, GLSK_FILE_URL);

        verify(fileImporter, times(1)).importGlsk(GLSK_FILE_URL);
        verify(glskDocument, times(1)).getZonalScalable(network);
        assertNotNull(networkShifter);
    }

    @Test
    void getNetworkShiftedWithShiftingFactorsWithFranceOutArea() {
        Network network = mock(Network.class);
        GlskDocument glskDocument = mock(GlskDocument.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        when(glskDocument.getZonalScalable(network)).thenReturn(null);
        when(fileImporter.importGlsk(GLSK_FILE_URL)).thenReturn(glskDocument);
        when(timestampWrapper.getExportCornerSplittingFactors()).thenReturn(TimeStampTestData.getExportCornerSplittingFactorsWithFranceOutArea());

        NetworkShifter networkShifter = cseValidNetworkShifter.getNetworkShifterWithShiftingFactors(timestampWrapper, network, GLSK_FILE_URL);

        verify(fileImporter, times(1)).importGlsk(GLSK_FILE_URL);
        verify(glskDocument, times(1)).getZonalScalable(network);
        assertNotNull(networkShifter);
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

        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterWithShiftingFactors(timestampWrapper, network, glskUrl);

        cseValidNetworkShifter.shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void getNetworkShiftedWithShiftingFactorsShouldThrowRunTimeExceptionBecauseGlskLimitationExceptionWasThrownWhenShifting() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        double shiftValue = 1000.0;

        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        doThrow(GlskLimitationException.class).when(networkShifter).shiftNetwork(shiftValue, network);
        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterWithShiftingFactors(timestampWrapper, network, glskUrl);

        assertThrows(CseValidShiftFailureException.class, () -> {
            cseValidNetworkShifter.shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        });

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void getNetworkShiftedWithShiftingFactorsShouldThrowRunTimeExceptionBecauseShiftingExceptionWasThrownWhenShifting() throws GlskLimitationException, ShiftingException {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        double shiftValue = 1000.0;

        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        TTimestampWrapper timestampWrapper = mock(TTimestampWrapper.class);

        doThrow(ShiftingException.class).when(networkShifter).shiftNetwork(shiftValue, network);
        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterWithShiftingFactors(timestampWrapper, network, glskUrl);

        assertThrows(CseValidShiftFailureException.class, () -> {
            cseValidNetworkShifter.shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);
        });

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }
}

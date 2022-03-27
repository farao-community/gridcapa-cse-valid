/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.util.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.util.TSplittingAndShifttingFactorsTestData;
import com.farao_community.farao.cse_valid.app.util.TimeStampTestData;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.math.BigDecimal;

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

    @MockBean
    private FileImporter fileImporter;

    @SpyBean
    private CseValidNetworkShifter cseValidNetworkShifter;

    @Test
    void getNetworkShifterWithSplittingFactors() {
        TSplittingFactors tSplittingFactors = TSplittingAndShifttingFactorsTestData.getTSplittingFactors();
        Network network = mock(Network.class);
        GlskDocument glskDocument = mock(GlskDocument.class);

        when(glskDocument.getZonalScalable(network)).thenReturn(null);
        when(fileImporter.importGlsk(GLSK_FILE_URL)).thenReturn(glskDocument);

        NetworkShifter networkShifter = cseValidNetworkShifter.getNetworkShifterWithSplittingFactors(tSplittingFactors, network, GLSK_FILE_URL);

        verify(fileImporter, times(1)).importGlsk(GLSK_FILE_URL);
        verify(glskDocument, times(1)).getZonalScalable(network);
        assertNotNull(networkShifter);
    }

    @Test
    void getNetworkShifterWithShifttingFactors() {
        TShiftingFactors tShiftingFactors = TSplittingAndShifttingFactorsTestData.getTShiftingFactors();
        Network network = mock(Network.class);
        GlskDocument glskDocument = mock(GlskDocument.class);

        when(glskDocument.getZonalScalable(network)).thenReturn(null);
        when(fileImporter.importGlsk(GLSK_FILE_URL)).thenReturn(glskDocument);

        NetworkShifter networkShifter = cseValidNetworkShifter.getNetworkShifterWithShifttingFactors(tShiftingFactors, network, GLSK_FILE_URL);

        verify(fileImporter, times(1)).importGlsk(GLSK_FILE_URL);
        verify(glskDocument, times(1)).getZonalScalable(network);
        assertNotNull(networkShifter);
    }

    @Test
    void shiftNetworkWithShifttingFactors() throws GlskLimitationException, ShiftingException {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmFileName = cseValidRequest.getCgm().getFilename();
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        BigDecimal miec = timestamp.getMIEC().getV();
        BigDecimal mibiec = timestamp.getMiBIEC().getV();
        BigDecimal antcFinal = timestamp.getANTCFinal().getV();
        double shiftValue = miec.subtract(mibiec.subtract(antcFinal)).doubleValue();

        when(fileImporter.importNetwork(cgmFileName, cgmUrl)).thenReturn(network);
        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterWithShifttingFactors(timestamp.getShiftingFactors(), network, glskUrl);

        cseValidNetworkShifter.shiftNetworkWithShifttingFactors(timestamp, cseValidRequest);

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void shiftNetworkWithShifttingFactorsShouldThrowRunTimeExceptionBecauseGlskLimitationExceptionWasThrownWhenShiftting() throws GlskLimitationException, ShiftingException {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmFileName = cseValidRequest.getCgm().getFilename();
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        BigDecimal miec = timestamp.getMIEC().getV();
        BigDecimal mibiec = timestamp.getMiBIEC().getV();
        BigDecimal antcFinal = timestamp.getANTCFinal().getV();
        double shiftValue = miec.subtract(mibiec.subtract(antcFinal)).doubleValue();

        when(fileImporter.importNetwork(cgmFileName, cgmUrl)).thenReturn(network);
        doThrow(GlskLimitationException.class).when(networkShifter).shiftNetwork(shiftValue, network);
        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterWithShifttingFactors(timestamp.getShiftingFactors(), network, glskUrl);

        assertThrows(RuntimeException.class, () -> {
            cseValidNetworkShifter.shiftNetworkWithShifttingFactors(timestamp, cseValidRequest);
        });

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }

    @Test
    void shiftNetworkWithShifttingFactorsShouldThrowRunTimeExceptionBecauseShiftingExceptionWasThrownWhenShiftting() throws GlskLimitationException, ShiftingException {
        TTimestamp timestamp = TimeStampTestData.getTimeStampWithFranceInArea();
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String cgmFileName = cseValidRequest.getCgm().getFilename();
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);

        BigDecimal miec = timestamp.getMIEC().getV();
        BigDecimal mibiec = timestamp.getMiBIEC().getV();
        BigDecimal antcFinal = timestamp.getANTCFinal().getV();
        double shiftValue = miec.subtract(mibiec.subtract(antcFinal)).doubleValue();

        when(fileImporter.importNetwork(cgmFileName, cgmUrl)).thenReturn(network);
        doThrow(ShiftingException.class).when(networkShifter).shiftNetwork(shiftValue, network);
        doReturn(networkShifter).when(cseValidNetworkShifter).getNetworkShifterWithShifttingFactors(timestamp.getShiftingFactors(), network, glskUrl);

        assertThrows(RuntimeException.class, () -> {
            cseValidNetworkShifter.shiftNetworkWithShifttingFactors(timestamp, cseValidRequest);
        });

        verify(networkShifter, times(1)).shiftNetwork(shiftValue, network);
    }
}

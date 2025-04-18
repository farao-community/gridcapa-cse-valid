/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;

    private static Network mockNetworkWithLines(String... lineIds) {
        Network network = mock(Network.class);
        for (String lineId : lineIds) {
            Branch l = mock(Line.class);
            when(l.getId()).thenReturn(lineId);
            when(network.getIdentifiable(lineId)).thenReturn(l);
        }
        return network;
    }

    @Test
    void testImportTtcAdjustmentFile() {
        TcDocumentType document = fileImporter.importTtcAdjustment(getClass().getResource("/TTC_Adjustment_20200813_2D4_CSE1_Simple_Import.xml").toString());
        assertEquals("TTC_Adjustment_20200813_2D4_CSE", document.getDocumentIdentification().getV());
        assertEquals(1, document.getAdjustmentResults().size());
        assertEquals("2020-08-12T22:30Z", document.getAdjustmentResults().get(0).getTimestamp().get(0).getReferenceCalculationTime().getV());
        assertEquals("Critical Branch", document.getAdjustmentResults().get(0).getTimestamp().get(0).getTTCLimitedBy().getV());
    }

    @Test
    void testImportTtcNonExistingFile() {
        assertNull(fileImporter.importTtcAdjustment("/DoesNotExist.xml"));
    }

    @Test
    void testImportGlsk() {
        GlskDocument glskDocument = fileImporter.importGlsk(Objects.requireNonNull(getClass().getResource("/20211125_1930_2D4_CO_GSK_CSE1.xml")).toString());
        assertNotNull(glskDocument);
    }

    @Test
    void testImportNetwork() {
        Network network = fileImporter.importNetwork("cgm.uct", Objects.requireNonNull(getClass().getResource("/20211125_1930_2D4_CO_Final_CSE1.uct")).toString());
        assertNotNull(network);
    }

    @Test
    void testImportRaoResult() throws IOException {
        InputStream cracInputStream = getClass().getResourceAsStream("/crac-for-rao-result-v1.5.json");
        assertNotNull(cracInputStream);

        final Network network = mockNetworkWithLines("ne1Id", "ne2Id", "ne3Id");
        final Identifiable injectionIdentifiable = mock(Identifiable.class);
        TwoWindingsTransformer pst = mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger = mock(PhaseTapChanger.class);
        when(pst.getPhaseTapChanger()).thenReturn(pstChanger);
        when(pstChanger.getTapPosition()).thenReturn(2);
        when(network.getTwoWindingsTransformer("pst")).thenReturn(pst);
        Map<Integer, PhaseTapChangerStep> mockedSteps = new HashMap<>();
        mockPhaseTapChangerStep(mockedSteps, 3, 3.);
        mockPhaseTapChangerStep(mockedSteps, 2, 2.5);
        mockPhaseTapChangerStep(mockedSteps, 1, 2.);
        mockPhaseTapChangerStep(mockedSteps, 0, 1.5);
        mockPhaseTapChangerStep(mockedSteps, -1, 1.);
        mockPhaseTapChangerStep(mockedSteps, -2, .5);
        mockPhaseTapChangerStep(mockedSteps, -3, .0);
        when(pstChanger.getAllSteps()).thenReturn(mockedSteps);
        // pst2
        TwoWindingsTransformer pst2 = mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger2 = mock(PhaseTapChanger.class);
        when(pst2.getPhaseTapChanger()).thenReturn(pstChanger2);
        when(pstChanger2.getTapPosition()).thenReturn(1);
        when(network.getTwoWindingsTransformer("pst2")).thenReturn(pst2);
        when(pstChanger2.getAllSteps()).thenReturn(mockedSteps);
        // pst 3
        TwoWindingsTransformer pst3 = mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger3 = Mockito.mock(PhaseTapChanger.class);
        Mockito.when(pst3.getPhaseTapChanger()).thenReturn(pstChanger2);
        Mockito.when(pstChanger3.getTapPosition()).thenReturn(1);
        Mockito.when(network.getTwoWindingsTransformer("pst3")).thenReturn(pst2);
        Mockito.when(pstChanger3.getAllSteps()).thenReturn(mockedSteps);

        when(network.getIdentifiable("injection")).thenReturn(injectionIdentifiable);
        when(injectionIdentifiable.getType()).thenReturn(IdentifiableType.GENERATOR);

        Crac crac = Crac.read("crac.json", cracInputStream, network);
        RaoResult raoResult = fileImporter.importRaoResult(Objects.requireNonNull(getClass().getResource("/rao-result-v1.1.json")).toString(), crac);
        assertNotNull(raoResult);
    }

    @Test
    void testImportCracFromJson() {
        final Network network = mockNetworkWithLines("ne1Id", "ne2Id", "ne3Id");
        final Identifiable injectionIdentifiable = mock(Identifiable.class);
        when(network.getIdentifiable("injection")).thenReturn(injectionIdentifiable);
        when(injectionIdentifiable.getType()).thenReturn(IdentifiableType.GENERATOR);
        // pst
        TwoWindingsTransformer pst = mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger = mock(PhaseTapChanger.class);
        when(pst.getPhaseTapChanger()).thenReturn(pstChanger);
        when(pstChanger.getTapPosition()).thenReturn(2);
        when(network.getTwoWindingsTransformer("pst")).thenReturn(pst);
        Map<Integer, PhaseTapChangerStep> mockedSteps = new HashMap<>();
        mockPhaseTapChangerStep(mockedSteps, 3, 3.);
        mockPhaseTapChangerStep(mockedSteps, 2, 2.5);
        mockPhaseTapChangerStep(mockedSteps, 1, 2.);
        mockPhaseTapChangerStep(mockedSteps, 0, 1.5);
        mockPhaseTapChangerStep(mockedSteps, -1, 1.);
        mockPhaseTapChangerStep(mockedSteps, -2, .5);
        mockPhaseTapChangerStep(mockedSteps, -3, .0);
        when(pstChanger.getAllSteps()).thenReturn(mockedSteps);
        // pst2
        TwoWindingsTransformer pst2 = mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger2 = mock(PhaseTapChanger.class);
        when(pst2.getPhaseTapChanger()).thenReturn(pstChanger2);
        when(pstChanger2.getTapPosition()).thenReturn(1);
        when(network.getTwoWindingsTransformer("pst2")).thenReturn(pst2);
        when(pstChanger2.getAllSteps()).thenReturn(mockedSteps);
        // pst 3
        TwoWindingsTransformer pst3 = mock(TwoWindingsTransformer.class);
        PhaseTapChanger pstChanger3 = Mockito.mock(PhaseTapChanger.class);
        Mockito.when(pst3.getPhaseTapChanger()).thenReturn(pstChanger2);
        Mockito.when(pstChanger3.getTapPosition()).thenReturn(1);
        Mockito.when(network.getTwoWindingsTransformer("pst3")).thenReturn(pst2);
        Mockito.when(pstChanger3.getAllSteps()).thenReturn(mockedSteps);

        Crac crac = fileImporter.importCracFromJson(Objects.requireNonNull(getClass().getResource("/crac-for-rao-result-v1.5.json")).toString(), network);
        assertNotNull(crac);
    }

    @Test
    void importCracCreationContext() {
        String cracUrl = Objects.requireNonNull(getClass().getResource("/20211125_0030_2D4_CRAC_FR1.xml")).toString();
        Network network = fileImporter.importNetwork("cgm.uct", Objects.requireNonNull(getClass().getResource("/20211125_1930_2D4_CO_Final_CSE1.uct")).toString());

        CseCracCreationContext cracCreationContext = fileImporter.importCracCreationContext(cracUrl, network);

        assertNotNull(cracCreationContext);
    }

    private static void mockPhaseTapChangerStep(final Map<Integer, PhaseTapChangerStep> mockedSteps,
                                                final Integer tapPosition,
                                                final Double alpha) {
        PhaseTapChangerStep step03 = mock(PhaseTapChangerStep.class);
        when(step03.getAlpha()).thenReturn(alpha);
        mockedSteps.put(tapPosition, step03);
    }
}

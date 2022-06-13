/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;

    @Test
    void testImportTtcAdjustmentFile() {
        TcDocumentType document = fileImporter.importTtcAdjustment(getClass().getResourceAsStream("/TTC_Adjustment_20200813_2D4_CSE1_Simple_Import.xml"));
        assertEquals("TTC_Adjustment_20200813_2D4_CSE", document.getDocumentIdentification().getV());
        assertEquals(1, document.getAdjustmentResults().size());
        assertEquals("2020-08-12T22:30Z", document.getAdjustmentResults().get(0).getTimestamp().get(0).getReferenceCalculationTime().getV());
        assertEquals("Critical Branch", document.getAdjustmentResults().get(0).getTimestamp().get(0).getTTCLimitedBy().getV());
    }

    @Test
    void testImportTtcNonExistingFile() {
        InputStream resourceAsStream = getClass().getResourceAsStream("/DoesNotExist.xml");
        assertThrows(CseValidInvalidDataException.class, () -> fileImporter.importTtcAdjustment(resourceAsStream));
    }

    @Test
    void testImportGlsk() throws IOException {
        GlskDocument glskDocument = fileImporter.importGlsk(Objects.requireNonNull(getClass().getResource("/20211125_1930_2D4_CO_GSK_CSE1.xml")).toString());
        assertNotNull(glskDocument);
    }

    @Test
    void testImportNetwork() throws IOException {
        Network network = fileImporter.importNetwork("cgm.uct", Objects.requireNonNull(getClass().getResource("/20211125_1930_2D4_CO_Final_CSE1.uct")).toString());
        assertNotNull(network);
    }

    @Test
    void testImportRaoResult() throws IOException {
        InputStream cracInputStream = getClass().getResourceAsStream("/crac-for-rao-result-v1.1.json");
        InputStream raoResultInputStream = getClass().getResourceAsStream("/rao-result-v1.1.json");
        assertNotNull(cracInputStream);
        Crac crac = CracImporters.importCrac("crac.json", cracInputStream);
        RaoResult raoResult = fileImporter.importRaoResult(Objects.requireNonNull(getClass().getResource("/rao-result-v1.1.json")).toString(), crac);
        assertNotNull(raoResult);
    }

    @Test
    void testImportCracFromJson() throws IOException {
        Crac crac = fileImporter.importCracFromJson(Objects.requireNonNull(getClass().getResource("/crac-for-rao-result-v1.1.json")).toString());
        assertNotNull(crac);
    }
}

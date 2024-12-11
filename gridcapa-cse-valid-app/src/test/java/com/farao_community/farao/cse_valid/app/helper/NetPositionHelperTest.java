/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.helper;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class NetPositionHelperTest {

    @Test
    void computeItalianImport() {
        Network network = Network.read("20211125_1930_2D4_CO_Final_CSE1.uct", getClass().getResourceAsStream("/20211125_1930_2D4_CO_Final_CSE1.uct"));
        double mniiExpected = 7632.0;
        double mniiActual = Math.round(NetPositionHelper.computeItalianImport(network));
        assertEquals(mniiExpected, mniiActual);
    }

    @Test
    void computeFranceImportFromItaly() {
        Network network = Network.read("20211125_1930_2D4_CO_Final_CSE1.uct", getClass().getResourceAsStream("/20211125_1930_2D4_CO_Final_CSE1.uct"));
        double mniiExpected = -2365.0;
        double mniiActual = Math.round(NetPositionHelper.computeFranceImportFromItaly(network));
        assertEquals(mniiExpected, mniiActual);
    }
}

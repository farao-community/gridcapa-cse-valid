/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
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
        Network network = Network.read("networkWithPRA.xiidm", getClass().getResourceAsStream("/20230707_0730_D2CC/networkWithPRA.xiidm"));
        double mniiExpected = 5030;
        double mniiActual = Math.round(NetPositionHelper.computeItalianImport(network));
        assertEquals(mniiExpected, mniiActual);
    }

    @Test
    void computeFranceImportFromItaly() {
        Network network = Network.read("networkWithPRA.xiidm", getClass().getResourceAsStream("/20230707_0730_D2CC/networkWithPRA.xiidm"));
        double mniiExpected = -1661;
        double mniiActual = Math.round(NetPositionHelper.computeFranceImportFromItaly(network));
        assertEquals(mniiExpected, mniiActual);
    }
}

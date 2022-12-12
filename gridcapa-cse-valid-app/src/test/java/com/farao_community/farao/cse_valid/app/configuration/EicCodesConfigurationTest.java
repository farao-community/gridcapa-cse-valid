/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class EicCodesConfigurationTest {
    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Test
    void checkEicCodesConfiguration() {
        assertEquals("10YAT-APG------L", eicCodesConfiguration.getAustria());
        assertEquals("10YFR-RTE------C", eicCodesConfiguration.getFrance());
        assertEquals("10YIT-GRTN-----B", eicCodesConfiguration.getItaly());
        assertEquals("10YSI-ELES-----O", eicCodesConfiguration.getSlovenia());
        assertEquals("10YCH-SWISSGRIDZ", eicCodesConfiguration.getSwitzerland());
    }
}

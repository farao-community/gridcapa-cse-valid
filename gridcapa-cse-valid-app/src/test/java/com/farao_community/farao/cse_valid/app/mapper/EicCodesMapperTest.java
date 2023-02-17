/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.mapper;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class EicCodesMapperTest {

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @Autowired
    private EicCodesMapper eicCodesMapper;

    @Test
    void mapToEicCodesShouldReturnAustriaCode() {
        String expected = eicCodesConfiguration.getAustria();
        String eicCode = eicCodesMapper.mapToEic("AT");
        assertEquals(expected, eicCode);
    }

    @Test
    void mapToEicCodesShouldReturnFranceCode() {
        String expected = eicCodesConfiguration.getFrance();
        String eicCode = eicCodesMapper.mapToEic("FR");
        assertEquals(expected, eicCode);
    }

    @Test
    void mapToEicCodesShouldReturnItalyCode() {
        String expected = eicCodesConfiguration.getItaly();
        String eicCode = eicCodesMapper.mapToEic("IT");
        assertEquals(expected, eicCode);
    }

    @Test
    void mapToEicCodesShouldReturnSloveniaCode() {
        String expected = eicCodesConfiguration.getSlovenia();
        String eicCode = eicCodesMapper.mapToEic("SI");
        assertEquals(expected, eicCode);
    }

    @Test
    void mapToEicCodesShouldReturnSwitzerlandCode() {
        String expected = eicCodesConfiguration.getSwitzerland();
        String eicCode = eicCodesMapper.mapToEic("CH");
        assertEquals(expected, eicCode);
    }

    @Test
    void mapToEicCodesShouldThrowCseValidInvalidDataException() {
        assertThrows(CseValidInvalidDataException.class, () -> {
            eicCodesMapper.mapToEic("TN");
        }, "CseValidInvalidDataException error was expected");
    }

    @Test
    void mapToEicCodesShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            eicCodesMapper.mapToEic("");
        }, "IllegalArgumentException error was expected");
    }
}

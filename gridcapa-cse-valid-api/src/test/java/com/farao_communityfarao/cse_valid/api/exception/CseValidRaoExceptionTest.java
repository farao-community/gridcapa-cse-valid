/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_communityfarao.cse_valid.api.exception;

import com.farao_community.farao.cse_valid.api.exception.CseValidRaoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class CseValidRaoExceptionTest {
    @Test
    void checkStandardExceptionUsage() {
        CseValidRaoException exception = new CseValidRaoException("Exception message");
        assertEquals("Exception message", exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(500, exception.getStatus());
        assertEquals("500-RaoException", exception.getCode());
        assertEquals("Exception message", exception.getTitle());
        assertEquals("Exception message", exception.getDetails());

        Exception cause = new RuntimeException("Cause");
        exception = new CseValidRaoException("Exception message", cause);
        assertEquals("Exception message", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(500, exception.getStatus());
        assertEquals("500-RaoException", exception.getCode());
        assertEquals("Exception message", exception.getTitle());
        assertEquals("Exception message; nested exception is java.lang.RuntimeException: Cause", exception.getDetails());
    }
}

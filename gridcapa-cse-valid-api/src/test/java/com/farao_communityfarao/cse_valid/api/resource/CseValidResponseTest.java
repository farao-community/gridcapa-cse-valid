/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_communityfarao.cse_valid.api.resource;

import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class CseValidResponseTest {

    @Test
    void checkCoreValidResponse() {
        Instant computationStartInstant = Instant.parse("2021-01-01T00:30:00Z");
        Instant computationEndInstant = Instant.parse("2021-01-01T00:35:00Z");
        String resultFileUrl = "testUrl";
        CseValidResponse coreValidResponse = new CseValidResponse("id", resultFileUrl, computationStartInstant, computationEndInstant);
        assertNotNull(coreValidResponse);
        assertEquals("id", coreValidResponse.getId());
        assertEquals(resultFileUrl, coreValidResponse.getResultFileUrl());
        assertEquals("2021-01-01T00:30:00Z", coreValidResponse.getComputationStartInstant().toString());
        assertEquals("2021-01-01T00:35:00Z", coreValidResponse.getComputationEndInstant().toString());
    }
}

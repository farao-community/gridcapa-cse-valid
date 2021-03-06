/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class AmqpMessagesConfigurationTest {
    @Autowired
    private AmqpMessagesConfiguration amqpConfiguration;

    @Autowired
    private Queue requestQueue;

    @Autowired
    private FanoutExchange responseExchange;

    @Test
    void checkAmqpMessageConfiguration() {
        assertNotNull(amqpConfiguration);
        assertNotNull(requestQueue);
        assertEquals("cse-valid-requests", requestQueue.getName());
        assertNotNull(responseExchange);
        assertEquals("cse-valid-response", responseExchange.getName());
        assertEquals("600000", amqpConfiguration.cseValidResponseExpiration());
    }
}

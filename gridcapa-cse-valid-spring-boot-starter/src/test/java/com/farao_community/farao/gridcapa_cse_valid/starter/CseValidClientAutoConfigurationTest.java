/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse_valid.starter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class CseValidClientAutoConfigurationTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void createContext() {
        context = new AnnotationConfigApplicationContext();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void registerCseValidClient() {
        context.registerBean("amqpTemplate", AmqpTemplate.class, () -> Mockito.mock(AmqpTemplate.class));
        context.register(CseValidClientAutoConfiguration.class);
        context.refresh();
        CseValidClient client = context.getBean(CseValidClient.class);
        assertNotNull(client);
    }
}

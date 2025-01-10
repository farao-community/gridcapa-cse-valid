/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.configuration;

import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Configuration
public class AmqpMessagesBeans {
    private final AmqpMessagesConfiguration config;

    public AmqpMessagesBeans(final AmqpMessagesConfiguration config) {
        this.config = config;
    }

    @Bean
    AsyncAmqpTemplate asyncTemplate(final RabbitTemplate rabbitTemplate) {
        final AsyncRabbitTemplate asyncTemplate = new AsyncRabbitTemplate(rabbitTemplate);
        asyncTemplate.setReceiveTimeout(config.getAsyncTimeOut());
        return asyncTemplate;
    }

}

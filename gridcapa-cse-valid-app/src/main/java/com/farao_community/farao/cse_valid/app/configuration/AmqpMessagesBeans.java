/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.configuration;

import com.farao_community.farao.cse_valid.app.CseValidListener;
import org.springframework.amqp.core.AsyncAmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

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
    AsyncAmqpTemplate asyncTemplate(RabbitTemplate rabbitTemplate) {
        AsyncRabbitTemplate asyncTemplate = new AsyncRabbitTemplate(rabbitTemplate);
        asyncTemplate.setReceiveTimeout(config.getAsyncTimeOut());
        return asyncTemplate;
    }

    @Bean
    public Queue cseValidRequestQueue() {
        return new Queue(config.getRequestDestination());
    }

    @Bean
    public TopicExchange cseValidTopicExchange() {
        return new TopicExchange(config.getRequestDestination());
    }

    @Bean
    public Binding cseValidRequestBinding() {
        return BindingBuilder.bind(cseValidRequestQueue()).to(cseValidTopicExchange()).with(Optional.ofNullable(config.getRequestRoutingKey()).orElse("#"));
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory,
                                                             Queue cseValidRequestQueue,
                                                             CseValidListener listener) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(cseValidRequestQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        return simpleMessageListenerContainer;
    }

    @Bean
    public FanoutExchange cseValidResponseExchange() {
        return new FanoutExchange(config.getResponseDestination());
    }
}

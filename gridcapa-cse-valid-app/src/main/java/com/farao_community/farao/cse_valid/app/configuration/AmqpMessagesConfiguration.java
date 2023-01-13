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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Configuration
public class AmqpMessagesConfiguration {

    @Value("${cse-valid-runner.bindings.response.destination}")
    private String responseDestination;
    @Value("${cse-valid-runner.bindings.response.expiration}")
    private String responseExpiration;
    @Value("${cse-valid-runner.bindings.request.destination}")
    private String requestDestination;
    @Value("${cse-valid-runner.bindings.request.routing-key}")
    private String requestRoutingKey;
    @Value("${cse-valid-runner.async-time-out}")
    private long asyncTimeOut;

    @Bean
    AsyncAmqpTemplate asyncTemplate(RabbitTemplate rabbitTemplate) {
        AsyncRabbitTemplate asyncTemplate = new AsyncRabbitTemplate(rabbitTemplate);
        asyncTemplate.setReceiveTimeout(asyncTimeOut);
        return asyncTemplate;
    }

    @Bean
    public Queue cseValidRequestQueue() {
        return new Queue(requestDestination);
    }

    @Bean
    public TopicExchange cseValidTopicExchange() {
        return new TopicExchange(requestDestination);
    }

    @Bean
    public Binding cseValidRequestBinding() {
        return BindingBuilder.bind(cseValidRequestQueue()).to(cseValidTopicExchange()).with(Optional.ofNullable(requestRoutingKey).orElse("#"));
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
        return new FanoutExchange(responseDestination);
    }

    public String cseValidResponseExpiration() {
        return responseExpiration;
    }
}

/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.support.GenericMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class CseValidListenerTest {
    @MockBean
    public CseValidHandler cseValidHandler;

    @Autowired
    public CseValidListener cseValidListener;

    @MockBean
    private StreamBridge streamBridge;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @TestConfiguration
    static class ProcessPublicationServiceTestConfiguration {
        @Bean
        @Primary
        public AmqpTemplate amqpTemplate() {
            return Mockito.mock(AmqpTemplate.class);
        }
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(amqpTemplate, cseValidHandler, streamBridge);
    }

    @Test
    void checkThatCorrectMessageIsHandledCorrectly() throws URISyntaxException, IOException {
        final byte[] correctMessage = Files.readAllBytes(Paths.get(getClass().getResource("/validRequest.json").toURI()));
        final Message amqpMessage = new Message(correctMessage, new MessageProperties());
        amqpMessage.getMessageProperties().setHeader("amqp_correlationId", "12345");
        amqpMessage.getMessageProperties().setHeader("amqp_replyTo", "amq.rabbitmq.reply-to.someQueue");
        final GenericMessage<byte[]> message = new GenericMessage<>(amqpMessage.getBody(),
                amqpMessage.getMessageProperties().getHeaders());
        final Instant computationStartInstant = Instant.parse("2021-01-01T00:30:00Z");
        final Instant computationEndInstant = Instant.parse("2021-01-01T00:35:00Z");
        final String resultFileUrl = "testUrl";
        final CseValidResponse cseValidResponse = new CseValidResponse("c7fc89da-dcd7-40d2-8d63-b8aef0a1ecdf", resultFileUrl, computationStartInstant, computationEndInstant);
        Mockito.when(cseValidHandler.handleCseValidRequest(Mockito.any(CseValidRequest.class))).thenReturn(cseValidResponse);

        cseValidListener.onMessage(message);
        Mockito.verify(streamBridge, Mockito.times(2)).send(Mockito.anyString(), Mockito.any());
        Mockito.verify(cseValidHandler, Mockito.times(1)).handleCseValidRequest(Mockito.any(CseValidRequest.class));
    }

    @Test
    void checkThatInvalidMessageReturnsError() throws URISyntaxException, IOException {
        final byte[] invalidMessage = Files.readAllBytes(Paths.get(getClass().getResource("/invalidRequest.json").toURI()));
        final Message amqpMessage = new Message(invalidMessage, new MessageProperties());
        amqpMessage.getMessageProperties().setHeader("amqp_correlationId", "67890");
        amqpMessage.getMessageProperties().setHeader("amqp_replyTo", "amq.rabbitmq.reply-to.someErrorQueue");
        final GenericMessage<byte[]> message = new GenericMessage<>(amqpMessage.getBody(), amqpMessage.getMessageProperties().getHeaders());
        cseValidListener.onMessage(message);
        Mockito.verify(streamBridge, Mockito.times(0)).send(Mockito.anyString(), Mockito.any());
        Mockito.verify(cseValidHandler, Mockito.times(0)).handleCseValidRequest(Mockito.any(CseValidRequest.class));
    }
}

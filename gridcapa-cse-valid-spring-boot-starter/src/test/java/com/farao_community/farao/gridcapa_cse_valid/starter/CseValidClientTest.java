/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse_valid.starter;

import com.farao_community.farao.cse_valid.api.JsonApiConverter;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class CseValidClientTest {
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void checkThatClientHandleMessageCorrectly() throws IOException {
        AmqpTemplate amqpTemplate = Mockito.mock(AmqpTemplate.class);
        CseValidClient client = new CseValidClient(amqpTemplate, buildProperties());
        CseValidRequest request = jsonApiConverter.fromJsonMessage(getClass().getResourceAsStream("/cseValidRequest.json").readAllBytes(), CseValidRequest.class);
        Message responseMessage = Mockito.mock(Message.class);

        Mockito.when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/cseValidResponse.json").readAllBytes());
        Mockito.when(amqpTemplate.sendAndReceive(Mockito.same("my-queue"), Mockito.any())).thenReturn(responseMessage);
        CseValidResponse response = client.run(request);

        assertEquals("test", response.getId());
    }

    private CseValidClientProperties buildProperties() {
        CseValidClientProperties properties = new CseValidClientProperties();
        CseValidClientProperties.BindingConfiguration amqpConfiguration = new CseValidClientProperties.BindingConfiguration();
        amqpConfiguration.setDestination("my-queue");
        amqpConfiguration.setExpiration("60000");
        amqpConfiguration.setApplicationId("application-id");
        properties.setBinding(amqpConfiguration);
        return properties;
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse_valid.starter;

import com.farao_community.farao.cse_valid.api.JsonApiConverter;
import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class CseValidClient {
    private static final int DEFAULT_PRIORITY = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidClient.class);
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";

    private final AmqpTemplate amqpTemplate;
    private final CseValidClientProperties cseValidClientProperties;
    private final JsonApiConverter jsonConverter;

    public CseValidClient(AmqpTemplate amqpTemplate, CseValidClientProperties cseValidClientProperties) {
        this.amqpTemplate = amqpTemplate;
        this.cseValidClientProperties = cseValidClientProperties;
        this.jsonConverter = new JsonApiConverter();
    }

    public CseValidResponse run(CseValidRequest cseValidRequest, int priority) {
        LOGGER.info("Cse valid request sent: {}", cseValidRequest);
        Message responseMessage = amqpTemplate.sendAndReceive(cseValidClientProperties.getAmqp().getQueueName(), buildMessage(cseValidRequest, priority));
        if (responseMessage != null) {
            return CseValidResponseConversionHelper.convertCseValidResponse(responseMessage, jsonConverter);
        } else {
            throw new CseValidInternalException("Cse valid Runner server did not respond");
        }
    }

    public CseValidResponse run(CseValidRequest cseValidRequest) {
        return run(cseValidRequest, DEFAULT_PRIORITY);
    }

    public Message buildMessage(CseValidRequest cseValidRequest, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(cseValidRequest))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(cseValidClientProperties.getAmqp().getApplicationId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(cseValidClientProperties.getAmqp().getExpiration())
                .setPriority(priority)
                .build();
    }
}

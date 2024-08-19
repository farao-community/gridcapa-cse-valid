/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.JsonApiConverter;
import com.farao_community.farao.cse_valid.api.exception.AbstractCseValidException;
import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.app.configuration.AmqpMessagesConfiguration;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CseValidListener implements MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseValidListener.class);
    private static final String CSE_RUN_FAILED = "CSE run failed: %s";
    private static final String APPLICATION_ID = "cse-valid-runner";
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";
    private static final int PRIORITY = 1;
    private static final String TASK_STATUS_UPDATE = "task-status-update";

    private final JsonApiConverter jsonApiConverter;
    private final AmqpTemplate amqpTemplate;
    private final CseValidHandler cseValidHandler;
    private final AmqpMessagesConfiguration amqpMessagesConfiguration;
    private final StreamBridge streamBridge;
    private final Logger businessLogger;

    public CseValidListener(AmqpTemplate amqpTemplate, CseValidHandler cseValidHandler,
                            AmqpMessagesConfiguration amqpMessagesConfiguration, StreamBridge streamBridge,
                            Logger businessLogger) {
        this.amqpTemplate = amqpTemplate;
        this.cseValidHandler = cseValidHandler;
        this.streamBridge = streamBridge;
        this.businessLogger = businessLogger;
        this.jsonApiConverter = new JsonApiConverter();
        this.amqpMessagesConfiguration = amqpMessagesConfiguration;
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String correlationId = message.getMessageProperties().getCorrelationId();

        CseValidRequest cseValidRequest = null;
        try {
            cseValidRequest = jsonApiConverter.fromJsonMessage(message.getBody(), CseValidRequest.class);
            // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", cseValidRequest.getId());
            LOGGER.info("Cse valid request received: {}", cseValidRequest);
            sendStatusUpdate(cseValidRequest.getId(), TaskStatus.RUNNING);
            CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
            sendStatusUpdate(cseValidRequest.getId(), TaskStatus.SUCCESS);
            sendCseValidResponse(cseValidResponse, replyTo, correlationId);
            LOGGER.info("Cse valid response sent: {}", cseValidResponse);
        } catch (Exception e) {
            Optional.ofNullable(cseValidRequest).ifPresent(request -> sendStatusUpdate(request.getId(), TaskStatus.ERROR));
            LOGGER.error(String.format(CSE_RUN_FAILED, e.getMessage()), e); // It enables to retrieve stack trace, impossible with business logger
            sendErrorResponse(e, replyTo, correlationId);
        }
    }

    private void sendStatusUpdate(String requestId, TaskStatus taskStatus) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), taskStatus));
    }

    private void sendErrorResponse(Exception e, String replyTo, String correlationId) {
        AbstractCseValidException wrappingException = new CseValidInternalException(String.format(CSE_RUN_FAILED, e.getMessage()), e);
        businessLogger.error(String.format(CSE_RUN_FAILED, wrappingException.getDetails()));
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createErrorResponse(wrappingException, correlationId));
        } else {
            amqpTemplate.send(amqpMessagesConfiguration.cseValidResponseExchange().getName(), "", createErrorResponse(wrappingException, correlationId));
        }
    }

    private void sendCseValidResponse(CseValidResponse cseValidResponse, String replyTo, String correlationId) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createMessageResponse(cseValidResponse, correlationId));
        } else {
            amqpTemplate.send(amqpMessagesConfiguration.cseValidResponseExchange().getName(), "", createMessageResponse(cseValidResponse, correlationId));
        }
    }

    private Message createMessageResponse(CseValidResponse cseValidResponse, String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(cseValidResponse))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private Message createErrorResponse(AbstractCseValidException exception, String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(exception))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private MessageProperties buildMessageResponseProperties(String correlationId) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(APPLICATION_ID)
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setCorrelationId(correlationId)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(amqpMessagesConfiguration.cseValidResponseExpiration())
                .setPriority(PRIORITY)
                .build();
    }

}

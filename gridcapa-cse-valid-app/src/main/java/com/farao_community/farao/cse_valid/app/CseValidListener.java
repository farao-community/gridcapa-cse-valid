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
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CseValidListener {
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

    public CseValidListener(final AmqpTemplate amqpTemplate,
                            final CseValidHandler cseValidHandler,
                            final AmqpMessagesConfiguration amqpMessagesConfiguration,
                            final StreamBridge streamBridge,
                            final Logger businessLogger) {
        this.amqpTemplate = amqpTemplate;
        this.cseValidHandler = cseValidHandler;
        this.streamBridge = streamBridge;
        this.businessLogger = businessLogger;
        this.jsonApiConverter = new JsonApiConverter();
        this.amqpMessagesConfiguration = amqpMessagesConfiguration;
    }

    @Bean
    public Consumer<GenericMessage<byte[]>> request() {
        return this::onMessage;
    }

    public void onMessage(final GenericMessage<byte[]> message) {
        final String replyTo = Optional.ofNullable(message.getHeaders().get("amqp_replyTo"))
                .orElse("")
                .toString();
        final String correlationId = Optional.ofNullable(message.getHeaders().get("amqp_correlationId"))
                .orElse("")
                .toString();
        CseValidRequest cseValidRequest = null;
        try {
            cseValidRequest = jsonApiConverter.fromJsonMessage(message.getPayload(), CseValidRequest.class);
            // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", cseValidRequest.getId());
            LOGGER.info("Cse valid request received: {}", cseValidRequest);
            sendStatusUpdate(cseValidRequest.getId(), TaskStatus.RUNNING);
            final CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
            sendStatusUpdate(cseValidRequest.getId(), TaskStatus.SUCCESS);
            sendCseValidResponse(cseValidResponse, replyTo, correlationId);
            LOGGER.info("Cse valid response sent: {}", cseValidResponse);
        } catch (final Exception e) {
            Optional.ofNullable(cseValidRequest).ifPresent(request -> sendStatusUpdate(request.getId(), TaskStatus.ERROR));
            LOGGER.error(String.format(CSE_RUN_FAILED, e.getMessage()), e); // It enables to retrieve stack trace, impossible with business logger
            sendErrorResponse(e, replyTo, correlationId);
        }
    }

    private void sendStatusUpdate(final String requestId, final TaskStatus taskStatus) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), taskStatus));
    }

    private void sendErrorResponse(final Exception e, final String replyTo, final String correlationId) {
        final AbstractCseValidException wrappingException = new CseValidInternalException(String.format(CSE_RUN_FAILED, e.getMessage()), e);
        final String errorMessage = String.format(CSE_RUN_FAILED, wrappingException.getDetails());
        businessLogger.error(errorMessage);
        amqpTemplate.send(replyTo, createErrorResponse(wrappingException, correlationId));
    }

    private void sendCseValidResponse(final CseValidResponse cseValidResponse, final String replyTo, final String correlationId) {
        amqpTemplate.send(replyTo, createMessageResponse(cseValidResponse, correlationId));
    }

    private Message createMessageResponse(final CseValidResponse cseValidResponse, final String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(cseValidResponse))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private Message createErrorResponse(final AbstractCseValidException exception, final String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(exception))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private MessageProperties buildMessageResponseProperties(final String correlationId) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(APPLICATION_ID)
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setCorrelationId(correlationId)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(String.valueOf(amqpMessagesConfiguration.getAsyncTimeOut()))
                .setPriority(PRIORITY)
                .build();
    }

}

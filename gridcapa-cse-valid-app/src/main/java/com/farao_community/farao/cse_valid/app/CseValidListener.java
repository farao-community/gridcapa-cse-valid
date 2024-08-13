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
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
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
    private static final String TASK_STATUS_UPDATE = "task-status-update";

    private final JsonApiConverter jsonApiConverter;
    private final CseValidHandler cseValidHandler;
    private final StreamBridge streamBridge;
    private final Logger businessLogger;

    public CseValidListener(CseValidHandler cseValidHandler, StreamBridge streamBridge,
                            Logger businessLogger) {
        this.cseValidHandler = cseValidHandler;
        this.streamBridge = streamBridge;
        this.businessLogger = businessLogger;
        this.jsonApiConverter = new JsonApiConverter();
    }

    @Override
    public void onMessage(Message message) {
        CseValidRequest cseValidRequest = null;
        try {
            cseValidRequest = jsonApiConverter.fromJsonMessage(message.getBody(), CseValidRequest.class);
            // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", cseValidRequest.getId());
            LOGGER.info("Cse valid request received: {}", cseValidRequest);
            sendStatusUpdate(cseValidRequest.getId(), TaskStatus.RUNNING);
            cseValidHandler.handleCseValidRequest(cseValidRequest);
            sendStatusUpdate(cseValidRequest.getId(), TaskStatus.SUCCESS);
        } catch (Exception e) {
            Optional.ofNullable(cseValidRequest).ifPresent(request -> sendStatusUpdate(request.getId(), TaskStatus.ERROR));
            LOGGER.error(String.format(CSE_RUN_FAILED, e.getMessage()), e); // It enables to retrieve stack trace, impossible with business logger
            logError(e);
        }
    }

    private void sendStatusUpdate(String requestId, TaskStatus taskStatus) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), taskStatus));
    }

    private void logError(Exception e) {
        AbstractCseValidException wrappingException = new CseValidInternalException(String.format(CSE_RUN_FAILED, e.getMessage()), e);
        businessLogger.error(String.format(CSE_RUN_FAILED, wrappingException.getDetails()));
    }

}

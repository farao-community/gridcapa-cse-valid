/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

    public String cseValidResponseExpiration() {
        return responseExpiration;
    }

    public String getResponseDestination() {
        return responseDestination;
    }

    public String getRequestDestination() {
        return requestDestination;
    }

    public String getRequestRoutingKey() {
        return requestRoutingKey;
    }

    public long getAsyncTimeOut() {
        return asyncTimeOut;
    }
}

/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_cse_valid.starter;

import com.farao_community.farao.cse_valid.api.JsonApiConverter;
import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import org.springframework.amqp.core.Message;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public final class CseValidResponseConversionHelper {

    private CseValidResponseConversionHelper() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static CseValidResponse convertCseValidResponse(Message message, JsonApiConverter jsonConverter) {
        try {
            return jsonConverter.fromJsonMessage(message.getBody(), CseValidResponse.class);
        } catch (ResourceParseException resourceParseException) {
            // exception details from cse-valid-runner app is wrapped into a ResourceParseException on json Api Error format.
            String originCause = resourceParseException.getErrors().getErrors().get(0).getDetail();
            throw new CseValidInvalidDataException(originCause);
        } catch (Exception unknownException) {
            throw new CseValidInvalidDataException("Unsupported exception thrown by cse-valid-runner app", unknownException);
        }
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.api;

import com.farao_community.farao.cse_valid.api.exception.AbstractCseValidException;
import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import com.github.jasminb.jsonapi.SerializationFeature;
import com.github.jasminb.jsonapi.exceptions.DocumentSerializationException;
import com.github.jasminb.jsonapi.models.errors.Error;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class JsonApiConverter {
    private final ObjectMapper objectMapper;

    public JsonApiConverter() {
        this.objectMapper = createObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    public <T> T fromJsonMessage(byte[] jsonMessage, Class<T> tClass) {
        ResourceConverter converter = createConverter();
        return converter.readDocument(jsonMessage, tClass).get();
    }

    public <T> byte[] toJsonMessage(T jsonApiObject) {
        ResourceConverter converter = createConverter();
        JSONAPIDocument<?> jsonapiDocument = new JSONAPIDocument<>(jsonApiObject);
        try {
            return converter.writeDocument(jsonapiDocument);
        } catch (DocumentSerializationException e) {
            throw new CseValidInternalException("Exception occurred during object conversion", e);
        }
    }

    public byte[] toJsonMessage(AbstractCseValidException exception) {
        ResourceConverter converter = createConverter();
        JSONAPIDocument<?> jsonapiDocument = new JSONAPIDocument<>(convertExceptionToJsonError(exception));
        try {
            return converter.writeDocument(jsonapiDocument);
        } catch (DocumentSerializationException e) {
            throw new CseValidInternalException("Exception occurred during exception message conversion", e);
        }
    }

    private ResourceConverter createConverter() {
        ResourceConverter converter = new ResourceConverter(objectMapper, CseValidRequest.class, CseValidResponse.class);
        converter.disableSerializationOption(SerializationFeature.INCLUDE_META);
        return converter;
    }

    private Error convertExceptionToJsonError(AbstractCseValidException exception) {
        Error error = new Error();
        error.setStatus(Integer.toString(exception.getStatus()));
        error.setCode(exception.getCode());
        error.setTitle(exception.getTitle());
        error.setDetail(exception.getDetails());
        return error;
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }
}

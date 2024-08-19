/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.Instant;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Type("cse-valid-response")
public class CseValidResponse {
    @Id
    private final String id;
    private final Instant computationStartInstant;
    private final Instant computationEndInstant;
    private final String resultFileUrl;

    @JsonCreator
    public CseValidResponse(@JsonProperty("id") String id,  @JsonProperty("resultFileUrl") String resultFileUrl, @JsonProperty("computationStartInstant") Instant computationStartInstant, @JsonProperty("computationEndInstant") Instant computationEndInstant) {
        this.id = id;
        this.computationStartInstant = computationStartInstant;
        this.computationEndInstant = computationEndInstant;
        this.resultFileUrl = resultFileUrl;
    }

    public String getId() {
        return id;
    }

    public Instant getComputationStartInstant() {
        return computationStartInstant;
    }

    public Instant getComputationEndInstant() {
        return computationEndInstant;
    }

    public String getResultFileUrl() {
        return resultFileUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

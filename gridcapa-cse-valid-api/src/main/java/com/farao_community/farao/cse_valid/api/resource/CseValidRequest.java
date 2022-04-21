/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.api.resource;

import com.farao_community.farao.cse_valid.api.OffsetDateTimeDeserializer;
import com.farao_community.farao.cse_valid.api.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Type("cse-valid-request")
public class CseValidRequest {
    @Id
    private final String id;
    private final ProcessType processType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private final OffsetDateTime timestamp;
    private final CseValidFileResource ttcAdjustment;
    private final CseValidFileResource crac;
    private final CseValidFileResource cgm;
    private final CseValidFileResource glsk;

    @JsonCreator
    public CseValidRequest(@JsonProperty("id") String id,
                            @JsonProperty("processType") ProcessType processType,
                            @JsonProperty("timestamp") OffsetDateTime timestamp,
                            @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                            @JsonProperty("crac") CseValidFileResource crac,
                            @JsonProperty("cgm") CseValidFileResource cgm,
                            @JsonProperty("glsk") CseValidFileResource glsk) {
        this.id = id;
        this.processType = processType;
        this.timestamp = timestamp;
        this.ttcAdjustment = ttcAdjustment;
        this.crac = crac;
        this.cgm = cgm;
        this.glsk = glsk;
    }

    public static CseValidRequest buildD2ccValidRequest(@JsonProperty("id") String id,
                                                        @JsonProperty("timestamp") OffsetDateTime timestamp,
                                                        @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                                                        @JsonProperty("crac") CseValidFileResource crac,
                                                        @JsonProperty("cgm") CseValidFileResource cgm,
                                                        @JsonProperty("glsk") CseValidFileResource glsk) {
        return new CseValidRequest(id, ProcessType.D2CC, timestamp, ttcAdjustment, crac, cgm, glsk);
    }

    public static CseValidRequest buildIdccValidRequest(@JsonProperty("id") String id,
                                                        @JsonProperty("timestamp") OffsetDateTime timestamp,
                                                        @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                                                        @JsonProperty("crac") CseValidFileResource crac,
                                                        @JsonProperty("cgm") CseValidFileResource cgm,
                                                        @JsonProperty("glsk") CseValidFileResource glsk) {
        return new CseValidRequest(id, ProcessType.IDCC, timestamp, ttcAdjustment, crac, cgm, glsk);
    }

    public String getId() {
        return id;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public CseValidFileResource getTtcAdjustment() {
        return ttcAdjustment;
    }

    public CseValidFileResource getCrac() {
        return crac;
    }

    public CseValidFileResource getCgm() {
        return cgm;
    }

    public CseValidFileResource getGlsk() {
        return glsk;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

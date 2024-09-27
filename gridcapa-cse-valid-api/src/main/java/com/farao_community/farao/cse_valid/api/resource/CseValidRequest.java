/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
    private final String currentRunId;
    private final ProcessType processType;

    /**
     * Timestamp is the target process calculation timestamp
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private final OffsetDateTime timestamp;

    /**
     * For Valid Cse process, the time defined in the ttc-adjustment file may be different from the target calculation time
     * We need to add this information in case of automatic launch via cse-valid-publication
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private final OffsetDateTime time;
    private final CseValidFileResource ttcAdjustment;
    private final CseValidFileResource importCrac;
    private final CseValidFileResource exportCrac;
    private final CseValidFileResource cgm;
    private final CseValidFileResource glsk;

    @JsonCreator
    public CseValidRequest(@JsonProperty("id") String id,
                           @JsonProperty("currentRunId") String currentRunId,
                           @JsonProperty("processType") ProcessType processType,
                           @JsonProperty("timestamp") OffsetDateTime timestamp,
                           @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                           @JsonProperty("importCrac") CseValidFileResource importCrac,
                           @JsonProperty("exportCrac") CseValidFileResource exportCrac,
                           @JsonProperty("cgm") CseValidFileResource cgm,
                           @JsonProperty("glsk") CseValidFileResource glsk,
                           @JsonProperty("time") OffsetDateTime time) {
        this.id = id;
        this.currentRunId = currentRunId;
        this.processType = processType;
        this.timestamp = timestamp;
        this.ttcAdjustment = ttcAdjustment;
        this.importCrac = importCrac;
        this.exportCrac = exportCrac;
        this.cgm = cgm;
        this.glsk = glsk;
        this.time = time;
    }

    public static CseValidRequest buildD2ccValidRequest(@JsonProperty("id") String id,
                                                        @JsonProperty("currentRunId") String currentRunId,
                                                        @JsonProperty("timestamp") OffsetDateTime timestamp,
                                                        @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                                                        @JsonProperty("importCrac") CseValidFileResource importCrac,
                                                        @JsonProperty("exportCrac") CseValidFileResource exportCrac,
                                                        @JsonProperty("cgm") CseValidFileResource cgm,
                                                        @JsonProperty("glsk") CseValidFileResource glsk) {
        return new CseValidRequest(id, currentRunId, ProcessType.D2CC, timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk, timestamp);
    }

    public static CseValidRequest buildIdccValidRequest(@JsonProperty("id") String id,
                                                        @JsonProperty("currentRunId") String currentRunId,
                                                        @JsonProperty("timestamp") OffsetDateTime timestamp,
                                                        @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                                                        @JsonProperty("importCrac") CseValidFileResource importCrac,
                                                        @JsonProperty("exportCrac") CseValidFileResource exportCrac,
                                                        @JsonProperty("cgm") CseValidFileResource cgm,
                                                        @JsonProperty("glsk") CseValidFileResource glsk) {
        return new CseValidRequest(id, currentRunId, ProcessType.IDCC, timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk, timestamp);
    }

    public static CseValidRequest buildD2ccValidRequest(@JsonProperty("id") String id,
                                                        @JsonProperty("currentRunId") String currentRunId,
                                                        @JsonProperty("timestamp") OffsetDateTime timestamp,
                                                        @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                                                        @JsonProperty("importCrac") CseValidFileResource importCrac,
                                                        @JsonProperty("exportCrac") CseValidFileResource exportCrac,
                                                        @JsonProperty("cgm") CseValidFileResource cgm,
                                                        @JsonProperty("glsk") CseValidFileResource glsk,
                                                        @JsonProperty("time") OffsetDateTime time) {
        return new CseValidRequest(id, currentRunId, ProcessType.D2CC, timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk, time);
    }

    public static CseValidRequest buildIdccValidRequest(@JsonProperty("id") String id,
                                                        @JsonProperty("currentRunId") String currentRunId,
                                                        @JsonProperty("timestamp") OffsetDateTime timestamp,
                                                        @JsonProperty("ttcAdjustment") CseValidFileResource ttcAdjustment,
                                                        @JsonProperty("importCrac") CseValidFileResource importCrac,
                                                        @JsonProperty("exportCrac") CseValidFileResource exportCrac,
                                                        @JsonProperty("cgm") CseValidFileResource cgm,
                                                        @JsonProperty("glsk") CseValidFileResource glsk,
                                                        @JsonProperty("time") OffsetDateTime time) {
        return new CseValidRequest(id, currentRunId, ProcessType.IDCC, timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk, time);
    }

    public String getId() {
        return id;
    }

    public String getCurrentRunId() {
        return currentRunId;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public CseValidFileResource getTtcAdjustment() {
        return ttcAdjustment;
    }

    public CseValidFileResource getImportCrac() {
        return importCrac;
    }

    public CseValidFileResource getExportCrac() {
        return exportCrac;
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

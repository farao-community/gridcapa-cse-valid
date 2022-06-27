/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.net_position;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Collections;
import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Getter
@ToString
@SuperBuilder
@Jacksonized
public class NetPositionReport {
    @NonNull
    private final Map<String, AreaReport> areasReport;

    public NetPositionReport(@JsonProperty(required = true) Map<String, AreaReport> areasReport) {
        this.areasReport = Collections.unmodifiableMap(areasReport);
    }
}

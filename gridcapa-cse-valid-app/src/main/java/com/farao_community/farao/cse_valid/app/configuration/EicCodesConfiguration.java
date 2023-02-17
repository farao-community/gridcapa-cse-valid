/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Set;

@ConstructorBinding
@EnableConfigurationProperties
@ConfigurationProperties("cse-valid-runner.eic-codes")
public class EicCodesConfiguration {
    private final String austria;
    private final String france;
    private final String italy;
    private final String slovenia;
    private final String switzerland;

    public EicCodesConfiguration(String austria, String france, String italy, String slovenia, String switzerland) {
        this.austria = austria;
        this.france = france;
        this.italy = italy;
        this.slovenia = slovenia;
        this.switzerland = switzerland;
    }

    public String getAustria() {
        return austria;
    }

    public String getFrance() {
        return france;
    }

    public String getItaly() {
        return italy;
    }

    public String getSlovenia() {
        return slovenia;
    }

    public String getSwitzerland() {
        return switzerland;
    }

    public Set<String> getCseCodes() {
        return Set.of(austria, france, italy, slovenia, switzerland);
    }
}

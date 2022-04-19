/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.app.configuration.MinioAdapter;
import org.springframework.stereotype.Component;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CseValidHandler {

    private final MinioAdapter minioAdapter;

    public CseValidHandler(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        return new CseValidResponse(cseValidRequest.getId());
    }
}

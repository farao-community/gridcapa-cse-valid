/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_communityfarao.cse_valid.api;

import com.farao_community.farao.cse_valid.api.JsonApiConverter;
import com.farao_community.farao.cse_valid.api.exception.AbstractCseValidException;
import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class JsonApiConverterTest {

    @Test
    void checkCseValidInputsJsonConversion() throws URISyntaxException, IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        String inputMessage = Files.readString(Paths.get(getClass().getResource("/validRequest.json").toURI()));
        CseValidRequest cseValidRequest = jsonApiConverter.fromJsonMessage(inputMessage.getBytes(), CseValidRequest.class);
        assertEquals("test", cseValidRequest.getId());
        assertEquals("ttcAdjustment.txt", cseValidRequest.getTtcAdjustment().getFilename());
        assertEquals("https://ttcAdjustment/file/url", cseValidRequest.getTtcAdjustment().getUrl());
        assertEquals("importCrac.txt", cseValidRequest.getImportCrac().getFilename());
        assertEquals("https://importCrac/file/url", cseValidRequest.getImportCrac().getUrl());
        assertEquals("exportCrac.txt", cseValidRequest.getExportCrac().getFilename());
        assertEquals("https://exportCrac/file/url", cseValidRequest.getExportCrac().getUrl());
        assertEquals("cgm.txt", cseValidRequest.getCgm().getFilename());
        assertEquals("https://cgm/file/url", cseValidRequest.getCgm().getUrl());
        assertEquals("glsk.txt", cseValidRequest.getGlsk().getFilename());
        assertEquals("https://glsk/file/url", cseValidRequest.getGlsk().getUrl());
    }

    @Test
    void checkInternalExceptionJsonConversion() throws URISyntaxException, IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        AbstractCseValidException exception = new CseValidInternalException("Something really bad happened");
        String expectedMessage = Files.readString(Paths.get(getClass().getResource("/cseValidInternalError.json").toURI()));
        assertEquals(expectedMessage, new String(jsonApiConverter.toJsonMessage(exception)));
    }

}

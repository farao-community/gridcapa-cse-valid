/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapterProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class CseValidHandlerTest {

    @Autowired
    CseValidHandler cseValidHandler;

    @MockBean
    MinioAdapter minioAdapter;

    //@MockBean
    private FileImporter fileImporter;

    private static CseValidRequest cseValidRequest;

    /*@BeforeAll //todo delete?
    static void setUp() {

        cseValidRequest = new CseValidRequest("id",
                ProcessType.D2CC,
                OffsetDateTime.now(),
                createFileResource(ttcFileName),
                createFileResource("crac.xml"),
                createFileResource("cgm.xml"),
                createFileResource("glsk.xml"));
    }*/

    @Test
    void simpleTestWithExistingTtcAdjustmentFile() {
        String ttcFileName = "TTC_Adjustment_20200813_2D4_CSE1_no_calcul.xml";
        cseValidRequest = new CseValidRequest("id",
                ProcessType.D2CC,
                OffsetDateTime.of(2020, 8, 12, 22, 30, 0, 0, ZoneOffset.UTC),
                createFileResource(ttcFileName),
                new CseValidFileResource("crac.xml", "file://crac.xml"),
                new CseValidFileResource("cgm.xml", "file://cgm.xml"),
                new CseValidFileResource("glsk.xml", "file://glsk.xml"));
        //when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "basepath", "url", "accesskey", "secretkey"));
        //when(minioAdapter.getFile(any())).thenReturn(getClass().getResourceAsStream("/" + ttcFileName));
        CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
        assertEquals("id", cseValidResponse.getId()); //todo add check with result file
        //assertEquals("", cseValidResponse.getResultFileUrl());
    }

    @Test
    void simpleTestWithNonExistingTtcAdjustmentFile() {
        cseValidRequest = new CseValidRequest("id",
                ProcessType.D2CC,
                OffsetDateTime.now(),
                new CseValidFileResource("ttc.xml", "file://ttc.xml"),
                new CseValidFileResource("crac.xml", "file://crac.xml"),
                new CseValidFileResource("cgm.xml", "file://cgm.xml"),
                new CseValidFileResource("glsk.xml", "file://glsk.xml"));
        when(minioAdapter.getProperties()).thenReturn(new MinioAdapterProperties("bucket", "basepath", "url", "accesskey", "secretkey"));
        when(minioAdapter.getFile(any())).thenReturn(getClass().getResourceAsStream("/doesNotExist.xml"));
        when(minioAdapter.generatePreSignedUrl(any())).thenReturn("/output.xml");
        CseValidResponse cseValidResponse = cseValidHandler.handleCseValidRequest(cseValidRequest);
        assertNotNull(cseValidResponse);
        assertEquals("id", cseValidResponse.getId());
        assertEquals("/output.xml", cseValidResponse.getResultFileUrl());
    }

    private CseValidFileResource createFileResource(String filename) {
        return new CseValidFileResource(filename, Objects.requireNonNull(getClass().getResource("/" + filename)).toExternalForm());
    }
}

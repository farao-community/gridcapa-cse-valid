/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_communityfarao.cse_valid.api.resource;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class CseValidRequestTest {

    private OffsetDateTime timestamp;
    private OffsetDateTime time;
    private CseValidFileResource ttcAdjustment;
    private CseValidFileResource importCrac;
    private CseValidFileResource exportCrac;
    private CseValidFileResource cgm;
    private CseValidFileResource glsk;

    @BeforeEach
    void setUp() {
        timestamp = OffsetDateTime.parse("2022-04-20T00:30Z");
        time = OffsetDateTime.parse("2022-04-20T01:30Z");
        ttcAdjustment = new CseValidFileResource("ttcAdjustment.txt", "http://path/to/ttcAdjustment/file");
        importCrac = new CseValidFileResource("importCrac.txt", "http://path/to/importCrac/file");
        exportCrac = new CseValidFileResource("exportCrac.txt", "http://path/to/exportCrac/file");
        cgm = new CseValidFileResource("cgm.txt", "http://path/to/cgm/file");
        glsk = new CseValidFileResource("glsk.txt", "http://path/to/glsk/file");
    }

    @Test
    void checkManualCseValidD2ccRequest() {
        CseValidRequest cseValidRequest = CseValidRequest.buildD2ccValidRequest("id", timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk);
        assertNotNull(cseValidRequest);
        assertEquals("id", cseValidRequest.getId());
        assertEquals(ProcessType.D2CC, cseValidRequest.getProcessType());
        assertEquals("2022-04-20T00:30Z", cseValidRequest.getTimestamp().toString());
        assertEquals("ttcAdjustment.txt", cseValidRequest.getTtcAdjustment().getFilename());
        assertEquals("importCrac.txt", cseValidRequest.getImportCrac().getFilename());
        assertEquals("exportCrac.txt", cseValidRequest.getExportCrac().getFilename());
        assertEquals("cgm.txt", cseValidRequest.getCgm().getFilename());
        assertEquals("glsk.txt", cseValidRequest.getGlsk().getFilename());
    }

    @Test
    void checkManualCseValidIdccRequest() {
        CseValidRequest cseValidRequest = CseValidRequest.buildIdccValidRequest("id", timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk);
        assertNotNull(cseValidRequest);
        assertEquals("id", cseValidRequest.getId());
        assertEquals(ProcessType.IDCC, cseValidRequest.getProcessType());
        assertEquals("2022-04-20T00:30Z", cseValidRequest.getTimestamp().toString());
        assertEquals("ttcAdjustment.txt", cseValidRequest.getTtcAdjustment().getFilename());
        assertEquals("importCrac.txt", cseValidRequest.getImportCrac().getFilename());
        assertEquals("exportCrac.txt", cseValidRequest.getExportCrac().getFilename());
        assertEquals("cgm.txt", cseValidRequest.getCgm().getFilename());
        assertEquals("glsk.txt", cseValidRequest.getGlsk().getFilename());
    }

    @Test
    void checkCseValidD2ccRequestWithDifferentTime() {
        CseValidRequest cseValidRequest = CseValidRequest.buildD2ccValidRequest("id", timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk, time);
        assertNotNull(cseValidRequest);
        assertEquals("id", cseValidRequest.getId());
        assertEquals(ProcessType.D2CC, cseValidRequest.getProcessType());
        assertEquals("2022-04-20T00:30Z", cseValidRequest.getTimestamp().toString());
        assertEquals("2022-04-20T01:30Z", cseValidRequest.getTime().toString());
        assertEquals("ttcAdjustment.txt", cseValidRequest.getTtcAdjustment().getFilename());
        assertEquals("importCrac.txt", cseValidRequest.getImportCrac().getFilename());
        assertEquals("exportCrac.txt", cseValidRequest.getExportCrac().getFilename());
        assertEquals("cgm.txt", cseValidRequest.getCgm().getFilename());
        assertEquals("glsk.txt", cseValidRequest.getGlsk().getFilename());
    }

    @Test
    void checkCseValidIdccRequestWithDifferentTime() {
        CseValidRequest cseValidRequest = CseValidRequest.buildIdccValidRequest("id", timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk, time);
        assertNotNull(cseValidRequest);
        assertEquals("id", cseValidRequest.getId());
        assertEquals(ProcessType.IDCC, cseValidRequest.getProcessType());
        assertEquals("2022-04-20T00:30Z", cseValidRequest.getTimestamp().toString());
        assertEquals("2022-04-20T01:30Z", cseValidRequest.getTime().toString());
        assertEquals("ttcAdjustment.txt", cseValidRequest.getTtcAdjustment().getFilename());
        assertEquals("importCrac.txt", cseValidRequest.getImportCrac().getFilename());
        assertEquals("exportCrac.txt", cseValidRequest.getExportCrac().getFilename());
        assertEquals("cgm.txt", cseValidRequest.getCgm().getFilename());
        assertEquals("glsk.txt", cseValidRequest.getGlsk().getFilename());
    }
}

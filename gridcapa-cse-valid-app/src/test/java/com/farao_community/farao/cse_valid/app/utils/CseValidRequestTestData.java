/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.utils;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public final class CseValidRequestTestData {

    private static final String TTC_ADJUSTMENT_FILE_NAME = "ttc_adj.txt";
    private static final String TTC_ADJUSTMENT_FILE_URL = "file://ttcadj.txt";

    private static final String IMPORT_CRAC_FILE_NAME = "importCrac.txt";
    private static final String IMPORT_CRAC_FILE_URL = "file://importCrac.txt";

    private static final String EXPORT_CRAC_FILE_NAME = "exportCrac.txt";
    private static final String EXPORT_CRAC_FILE_URL = "file://exportCrac.txt";

    private static final String CGM_FILE_NAME = "cgm.uct";
    private static final String CGM_FILE_URL = "file://cgm.uct";

    private static final String GLSK_FILE_NAME = "glsk.xml";
    private static final String GLSK_FILE_URL = "file://glsk.xml";

    private CseValidRequestTestData() {
    }

    public static CseValidRequest getCseValidRequestMissingAllFiles(ProcessType processType) {
        String id = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        OffsetDateTime timestamp = OffsetDateTime.parse("2022-12-13T14:30Z");
        return new CseValidRequest(id, runId, processType, timestamp, null, null, null, null, null, timestamp);
    }

    /* --------------- FULL IMPORT --------------- */

    public static CseValidRequest getImportCseValidRequest(ProcessType processType) {
        String id = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        OffsetDateTime timestamp = OffsetDateTime.parse("2022-12-13T14:30Z");
        CseValidFileResource ttcAdjustment = new CseValidFileResource(TTC_ADJUSTMENT_FILE_NAME, TTC_ADJUSTMENT_FILE_URL);
        CseValidFileResource importCrac = new CseValidFileResource(IMPORT_CRAC_FILE_NAME, IMPORT_CRAC_FILE_URL);
        CseValidFileResource cgm = new CseValidFileResource(CGM_FILE_NAME, CGM_FILE_URL);
        CseValidFileResource glsk = new CseValidFileResource(GLSK_FILE_NAME, GLSK_FILE_URL);
        return new CseValidRequest(id, runId, processType, timestamp, ttcAdjustment, importCrac, null, cgm, glsk, timestamp);
    }

    /* --------------- EXPORT CORNER --------------- */

    public static CseValidRequest getExportCseValidRequest(ProcessType processType) {
        String id = UUID.randomUUID().toString();
        String runId = UUID.randomUUID().toString();
        OffsetDateTime timestamp = OffsetDateTime.parse("2022-12-13T14:30Z");
        CseValidFileResource ttcAdjustment = new CseValidFileResource(TTC_ADJUSTMENT_FILE_NAME, TTC_ADJUSTMENT_FILE_URL);
        CseValidFileResource importCrac = new CseValidFileResource(IMPORT_CRAC_FILE_NAME, IMPORT_CRAC_FILE_URL);
        CseValidFileResource exportCrac = new CseValidFileResource(EXPORT_CRAC_FILE_NAME, EXPORT_CRAC_FILE_URL);
        CseValidFileResource cgm = new CseValidFileResource(CGM_FILE_NAME, CGM_FILE_URL);
        CseValidFileResource glsk = new CseValidFileResource(GLSK_FILE_NAME, GLSK_FILE_URL);
        return new CseValidRequest(id, runId, processType, timestamp, ttcAdjustment, importCrac, exportCrac, cgm, glsk, timestamp);
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.validator;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
import com.farao_community.farao.cse_valid.app.util.CseValidRequestTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class CseValidRequestValidatorTest {

    @Autowired
    private CseValidRequestValidator cseValidRequestValidator;

    /* ------------------- checkAllFilesExist ------------------- */

    @Test
    void checkAllFilesExistShouldThrowAnExceptionWhenFranceImportingAndRequestIsNull() {
        CseValidRequest cseValidRequest = null;
        String errorMessage = "Request is null";

        CseValidRequestValidatorException thrown = assertThrows(CseValidRequestValidatorException.class, () -> {
            cseValidRequestValidator.checkAllFilesExist(cseValidRequest, true);
        }, "CseValidRequestValidatorException error was expected");

        assertEquals(errorMessage, thrown.getMessage());
    }

    @Test
    void checkAllFilesExistShouldThrowAnExceptionWhenFranceExportingAndRequestIsNull() {
        CseValidRequest cseValidRequest = null;
        String errorMessage = "Request is null";

        CseValidRequestValidatorException thrown = assertThrows(CseValidRequestValidatorException.class, () -> {
            cseValidRequestValidator.checkAllFilesExist(cseValidRequest, false);
        }, "CseValidRequestValidatorException error was expected");

        assertEquals(errorMessage, thrown.getMessage());
    }

    @Test
    void checkAllFilesExistShouldThrowAnExceptionWhenMissingAllFilesAndFranceImporting() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getCseValidRequestMissingAllFiles(ProcessType.IDCC);
        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, GLSK file, CRAC file, CRAC Transit file.";

        CseValidRequestValidatorException thrown = assertThrows(CseValidRequestValidatorException.class, () -> {
            cseValidRequestValidator.checkAllFilesExist(cseValidRequest, true);
        }, "CseValidRequestValidatorException error was expected");

        assertEquals(errorMessage, thrown.getMessage());
    }

    @Test
    void checkAllFilesExistShouldThrowAnExceptionWhenMissingAllFilesAndFranceExporting() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getCseValidRequestMissingAllFiles(ProcessType.IDCC);
        String errorMessage = "Process fail during TSO validation phase: Missing CGM file, GLSK file, CRAC file.";

        CseValidRequestValidatorException thrown = assertThrows(CseValidRequestValidatorException.class, () -> {
            cseValidRequestValidator.checkAllFilesExist(cseValidRequest, false);
        }, "CseValidRequestValidatorException error was expected");

        assertEquals(errorMessage, thrown.getMessage());
    }

    @Test
    void checkAllFilesExistWhenFranceImportingAndAllFilesExist() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        assertDoesNotThrow(() -> cseValidRequestValidator.checkAllFilesExist(cseValidRequest, true));
    }

    @Test
    void checkAllFilesExistWhenFranceExportingAndAllFilesExist() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        assertDoesNotThrow(() -> cseValidRequestValidator.checkAllFilesExist(cseValidRequest, false));
    }
}

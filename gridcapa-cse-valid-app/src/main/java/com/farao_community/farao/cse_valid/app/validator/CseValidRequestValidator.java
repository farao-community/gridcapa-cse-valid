/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.validator;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;

import java.util.StringJoiner;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */

public final class CseValidRequestValidator {

    private CseValidRequestValidator() {
    }

    public static void checkAllFilesExist(CseValidRequest cseValidRequest, boolean isFranceImportingFromItaly) throws CseValidRequestValidatorException {

        if (cseValidRequest == null) {
            throw new CseValidRequestValidatorException("Request is null");
        }

        final boolean cgmFileExists = fileExists(cseValidRequest.getCgm());
        final boolean glskFileExists = fileExists(cseValidRequest.getGlsk());

        if (isFranceImportingFromItaly) {
            final boolean exportCracFileExist = fileExists(cseValidRequest.getExportCrac());
            final boolean allFilesExist = cgmFileExists && glskFileExists && exportCracFileExist;
            if (!allFilesExist) {
                final String message = buildMessageForMissingFiles(cgmFileExists, glskFileExists, exportCracFileExist);
                throw new CseValidRequestValidatorException(message);
            }
        } else {
            final boolean importCracFileExists = fileExists(cseValidRequest.getImportCrac());
            final boolean allFilesExist = cgmFileExists && glskFileExists && importCracFileExists;
            if (!allFilesExist) {
                final String message = buildMessageForMissingFiles(cgmFileExists, glskFileExists, importCracFileExists);
                throw new CseValidRequestValidatorException(message);
            }
        }
    }

    private static boolean fileExists(CseValidFileResource cseValidFileResource) {
        return cseValidFileResource != null && cseValidFileResource.getFilename() != null && cseValidFileResource.getUrl() != null;
    }

    private static String buildMessageForMissingFiles(boolean cgmFileExists, boolean glskFileExists, boolean cracFileExists) {
        StringJoiner stringJoiner = new StringJoiner(", ", "Process fail during TSO validation phase: Missing ", ".");

        if (!cgmFileExists) {
            stringJoiner.add("CGM file");
        }

        if (!glskFileExists) {
            stringJoiner.add("GLSK file");
        }

        if (!cracFileExists) {
            stringJoiner.add("CRAC file");
        }

        return stringJoiner.toString();
    }
}

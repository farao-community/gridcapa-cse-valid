/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.validator;

import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
public class CseValidRequestValidator {

    public void validateImportCornerCseValidRequest(CseValidRequest cseValidRequest) throws CseValidRequestValidatorException {

        if (cseValidRequest == null) {
            throw new CseValidRequestValidatorException("Request is null");
        }

        final boolean cgmFileExists = fileExists(cseValidRequest.getCgm());
        final boolean glskFileExists = fileExists(cseValidRequest.getGlsk());
        final boolean importCracFileExists = fileExists(cseValidRequest.getImportCrac());
        final boolean allFilesExist = cgmFileExists && glskFileExists && importCracFileExists;
        if (!allFilesExist) {
            final String message = buildMessageImportCornerForMissingFiles(cgmFileExists, glskFileExists, importCracFileExists);
            throw new CseValidRequestValidatorException(message);
        }
    }

    public void validateExportCornerCseValidRequest(CseValidRequest cseValidRequest, boolean isExportCornerActive) throws CseValidRequestValidatorException {

        if (cseValidRequest == null) {
            throw new CseValidRequestValidatorException("Request is null");
        }

        final boolean cgmFileExists = fileExists(cseValidRequest.getCgm());
        final boolean glskFileExists = fileExists(cseValidRequest.getGlsk());
        final boolean importCracFileExists = fileExists(cseValidRequest.getImportCrac());

        if (isExportCornerActive) {
            final boolean exportCracFileExist = fileExists(cseValidRequest.getExportCrac());
            final boolean allFilesExist = cgmFileExists && glskFileExists && importCracFileExists && exportCracFileExist;
            if (!allFilesExist) {
                final String message = buildMessageExportCornerForMissingFiles(cgmFileExists, glskFileExists, importCracFileExists, exportCracFileExist);
                throw new CseValidRequestValidatorException(message);
            }
        } else {
            final boolean allFilesExist = cgmFileExists && glskFileExists && importCracFileExists;
            if (!allFilesExist) {
                final String message = buildMessageImportCornerForMissingFiles(cgmFileExists, glskFileExists, importCracFileExists);
                throw new CseValidRequestValidatorException(message);
            }
        }
    }

    private static boolean fileExists(CseValidFileResource cseValidFileResource) {
        return cseValidFileResource != null && cseValidFileResource.getFilename() != null && cseValidFileResource.getUrl() != null;
    }

    private static String buildMessageImportCornerForMissingFiles(boolean cgmFileExists, boolean glskFileExists, boolean importCracFileExists) {
        StringJoiner stringJoiner = new StringJoiner(", ", "Process fail during TSO validation phase: Missing ", ".");

        if (!cgmFileExists) {
            stringJoiner.add("CGM file");
        }

        if (!glskFileExists) {
            stringJoiner.add("GLSK file");
        }

        if (!importCracFileExists) {
            stringJoiner.add("CRAC file");
        }

        return stringJoiner.toString();
    }

    private static String buildMessageExportCornerForMissingFiles(boolean cgmFileExists, boolean glskFileExists, boolean importCracFileExists, boolean exportCracFileExists) {
        StringJoiner stringJoiner = new StringJoiner(", ", "Process fail during TSO validation phase: Missing ", ".");

        if (!cgmFileExists) {
            stringJoiner.add("CGM file");
        }

        if (!glskFileExists) {
            stringJoiner.add("GLSK file");
        }

        if (!importCracFileExists) {
            stringJoiner.add("CRAC file");
        }

        if (!exportCracFileExists) {
            stringJoiner.add("CRAC Transit file");
        }

        return stringJoiner.toString();
    }
}

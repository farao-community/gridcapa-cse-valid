/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.ObjectFactory;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.TcDocumentType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBIntrospector;
import java.io.InputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public final class FileImporter {

    private FileImporter() {
    }

    public static TcDocumentType importTtcAdjustment(InputStream inputStream) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            return (TcDocumentType) JAXBIntrospector.getValue(jaxbContext.createUnmarshaller().unmarshal(inputStream));
        } catch (Exception e) {
            throw new CseValidInvalidDataException("Cannot import file, it might not exist or it might not follow the xsd rules", e);
        }
    }
}

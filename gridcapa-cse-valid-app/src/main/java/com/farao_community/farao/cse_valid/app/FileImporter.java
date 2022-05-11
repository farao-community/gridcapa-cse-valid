/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.ObjectFactory;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.TcDocumentType;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBIntrospector;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FileImporter {

    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public TcDocumentType importTtcAdjustment(InputStream inputStream) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            return (TcDocumentType) JAXBIntrospector.getValue(jaxbContext.createUnmarshaller().unmarshal(inputStream));
        } catch (Exception e) {
            throw new CseValidInvalidDataException("Cannot import file, it might not exist or it might not follow the xsd rules", e);
        }
    }

    public GlskDocument importGlsk(String glskUrl) throws IOException {
        return GlskDocumentImporters.importGlsk(urlValidationService.openUrlStream(glskUrl));
    }

    public Network importNetwork(String cgmUrl) throws IOException {
        return Importers.loadNetwork(cgmUrl, urlValidationService.openUrlStream(cgmUrl));
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.ObjectFactory;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.TcDocumentType;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBIntrospector;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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

    public Network importNetwork(String filename, String cgmUrl) throws IOException {
        return Importers.loadNetwork(filename, urlValidationService.openUrlStream(cgmUrl));
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) throws IOException {
        return new RaoResultImporter().importRaoResult(urlValidationService.openUrlStream(raoResultUrl), crac);
    }

    public Crac importCracFromJson(String cracUrl) throws IOException {
        InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl);
        return CracImporters.importCrac(FilenameUtils.getName(new URL(cracUrl).getPath()), cracResultStream);
    }

    public String buildTtcFileUrl(CseValidRequest cseValidRequest) {
        return cseValidRequest.getProcessType().toString() +
                "/TTC_ADJUSTMENT/" +
                cseValidRequest.getTtcAdjustment().getFilename();
    }

    public String buildNetworkFileUrl(CseValidRequest cseValidRequest) {
        return cseValidRequest.getProcessType().toString() +
                "/CGMs/" +
                cseValidRequest.getCgm().getFilename();
    }

    public String buildGlskFileUrl(CseValidRequest cseValidRequest) {
        return cseValidRequest.getProcessType().toString() +
                "/GLSKs/" +
                cseValidRequest.getGlsk().getFilename();
    }
}

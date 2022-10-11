/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.ObjectFactory;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBIntrospector;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FileImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);

    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public TcDocumentType importTtcAdjustment(String ttcUrl) {
        try (InputStream inputStream = urlValidationService.openUrlStream(ttcUrl)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (TcDocumentType) JAXBIntrospector.getValue(unmarshaller.unmarshal(inputStream));
        } catch (Exception e) {
            LOGGER.error("Impossible to import TTC adjustment file: {}", ttcUrl, e);
            return null;
        }
    }

    public GlskDocument importGlsk(String glskUrl) throws IOException {
        return GlskDocumentImporters.importGlsk(urlValidationService.openUrlStream(glskUrl));
    }

    public Network importNetwork(String cgmUrl) {
        try {
            String filename = getFilenameFromUrl(cgmUrl);
            return importNetwork(filename, cgmUrl);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Cannot import Network from url %s", cgmUrl), e);
        }
    }

    public Network importNetwork(String filename, String cgmUrl) throws IOException {
        return Importers.loadNetwork(filename, urlValidationService.openUrlStream(cgmUrl));
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) throws IOException {
        return new RaoResultImporter().importRaoResult(urlValidationService.openUrlStream(raoResultUrl), crac);
    }

    public CseCrac importCseCrac(String cracUrl) throws IOException {
        InputStream cracInputStream = urlValidationService.openUrlStream(cracUrl);
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(cracInputStream);
    }

    public Crac importCrac(CseCrac cseCrac, OffsetDateTime targetProcessDateTime, Network network) {
        return CracCreators.createCrac(cseCrac, network, targetProcessDateTime).getCrac();
    }

    public Crac importCracFromJson(String cracUrl) {
        try (InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl)) {
            return CracImporters.importCrac(FilenameUtils.getName(new URL(cracUrl).getPath()), cracResultStream);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Cannot import crac from JSON : %s", cracUrl));
        }
    }

    private static String getFilenameFromUrl(String url) {
        try {
            return FilenameUtils.getName(new URL(url).getPath());
        } catch (MalformedURLException e) {
            throw new CseValidInvalidDataException(String.format("URL is invalid: %s", url));
        }
    }

}

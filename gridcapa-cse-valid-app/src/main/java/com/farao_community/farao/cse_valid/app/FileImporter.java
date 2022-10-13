/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.UrlWhitelistConfiguration;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FileImporter {
    private final UrlWhitelistConfiguration urlWhitelistConfiguration;
    private final Logger businessLogger;

    public FileImporter(UrlWhitelistConfiguration urlWhitelistConfiguration, Logger businessLogger) {
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
        this.businessLogger = businessLogger;
    }

    public TcDocumentType importTtcAdjustment(String ttcUrl) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (TcDocumentType) JAXBIntrospector.getValue(unmarshaller.unmarshal(openUrlStream(ttcUrl)));
        } catch (Exception e) {
            businessLogger.error("Impossible to import TTC adjustment file: {}", ttcUrl, e);
            return null;
        }
    }

    public GlskDocument importGlsk(String glskUrl) {
        return GlskDocumentImporters.importGlsk(openUrlStream(glskUrl));
    }

    public Network importNetwork(String cgmUrl) {
        return importNetwork(getFilenameFromUrl(cgmUrl), cgmUrl);
    }

    public Network importNetwork(String filename, String cgmUrl) {
        return Importers.loadNetwork(filename, openUrlStream(cgmUrl));
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        return new RaoResultImporter().importRaoResult(openUrlStream(raoResultUrl), crac);
    }

    public CseCrac importCseCrac(String cracUrl) {
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(openUrlStream(cracUrl));
    }

    public Crac importCrac(CseCrac cseCrac, OffsetDateTime targetProcessDateTime, Network network) {
        return CracCreators.createCrac(cseCrac, network, targetProcessDateTime).getCrac();
    }

    public Crac importCracFromJson(String cracUrl) {
        return CracImporters.importCrac(getFilenameFromUrl(cracUrl), openUrlStream(cracUrl));
    }

    private InputStream openUrlStream(String urlString) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new CseValidInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URL(urlString);
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error while retrieving content of file : %s, Link may have expired.", getFilenameFromUrl(urlString)), e);
        }
    }

    private String getFilenameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Exception occurred while retrieving file name from URL : %s", stringUrl), e);
        }
    }

}

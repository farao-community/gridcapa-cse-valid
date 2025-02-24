/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.configuration.UrlWhitelistConfiguration;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.ObjectFactory;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonImporter;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class FileImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);

    private final UrlWhitelistConfiguration urlWhitelistConfiguration;
    private final Logger businessLogger;

    public FileImporter(UrlWhitelistConfiguration urlWhitelistConfiguration, Logger businessLogger) {
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
        this.businessLogger = businessLogger;
    }

    public TcDocumentType importTtcAdjustment(String ttcUrl) {
        try (InputStream is = openUrlStream(ttcUrl)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (TcDocumentType) JAXBIntrospector.getValue(unmarshaller.unmarshal(is));
        } catch (Exception e) {
            String msg = String.format("Error importing TTC adjustment file at %s", ttcUrl);
            LOGGER.warn(msg, e);
            businessLogger.warn("{}. Nested cause: {}", msg, e.getMessage());
            return null;
        }
    }

    public GlskDocument importGlsk(String glskUrl) {
        try (InputStream is = openUrlStream(glskUrl)) {
            return GlskDocumentImporters.importGlskWithCalculationDirections(is);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing GLSK file at %s", glskUrl), e);
        }
    }

    public Network importNetwork(String cgmUrl) {
        try {
            return importNetwork(getFilenameFromUrl(cgmUrl), cgmUrl);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing network at %s", cgmUrl), e);
        }
    }

    public Network importNetwork(String filename, String cgmUrl) {
        try (InputStream is = openUrlStream(cgmUrl)) {
            return Network.read(filename, is);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing network at %s", cgmUrl), e);
        }
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        try (InputStream is = openUrlStream(raoResultUrl)) {
            return new RaoResultJsonImporter().importData(is, crac);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing RAO result at %s", raoResultUrl), e);
        }
    }

    public CseCracCreationContext importCracCreationContext(String cracUrl, Network network) {
        try (InputStream is = openUrlStream(cracUrl)) {
            return (CseCracCreationContext) Crac.readWithContext(getFilenameFromUrl(cracUrl), is, network);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing native CRAC at %s", cracUrl), e);
        }
    }

    public Crac importCracFromJson(String cracUrl, Network network) {
        try (InputStream is = openUrlStream(cracUrl)) {
            return Crac.read(getFilenameFromUrl(cracUrl), is, network);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing CRAC from JSON at %s", cracUrl), e);
        }
    }

    private InputStream openUrlStream(String urlString) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new CseValidInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URI(urlString).toURL();
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new CseValidInvalidDataException(String.format("Error while retrieving content of URL : %s, Link may have expired.", urlString), e);
        }
    }

    private String getFilenameFromUrl(String stringUrl) throws MalformedURLException {
        try {
            URL url = new URI(stringUrl).toURL();
            return FilenameUtils.getName(url.getPath());
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new CseValidInvalidDataException(String.format("Error while retrieving filename from URL : %s.", stringUrl), e);
        }
    }
}

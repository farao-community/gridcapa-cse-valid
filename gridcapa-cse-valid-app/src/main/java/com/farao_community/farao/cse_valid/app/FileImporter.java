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
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
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
            return GlskDocumentImporters.importGlsk(is);
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
            return Importers.loadNetwork(filename, is);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing network at %s", cgmUrl), e);
        }
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        try (InputStream is = openUrlStream(raoResultUrl)) {
            return new RaoResultImporter().importRaoResult(is, crac);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing RAO result at %s", raoResultUrl), e);
        }
    }

    public CseCracCreationContext importCracCreationContext(String cracUrl, OffsetDateTime targetProcessDateTime, Network network) {
        try (InputStream is = openUrlStream(cracUrl)) {
            CseCrac nativeCseCrac = new CseCracImporter().importNativeCrac(is);
            return (CseCracCreationContext) CracCreators.createCrac(nativeCseCrac, network, targetProcessDateTime);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing native CRAC at %s", cracUrl), e);
        }
    }

    public Crac importCracFromJson(String cracUrl) {
        try (InputStream is = openUrlStream(cracUrl)) {
            return CracImporters.importCrac(getFilenameFromUrl(cracUrl), is);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error importing CRAC from JSON at %s", cracUrl), e);
        }
    }

    private InputStream openUrlStream(String urlString) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new CseValidInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URL(urlString);
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Error while retrieving content of URL : %s, Link may have expired.", urlString), e);
        }
    }

    private String getFilenameFromUrl(String stringUrl) throws MalformedURLException {
        URL url = new URL(stringUrl);
        return FilenameUtils.getName(url.getPath());
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.ObjectFactory;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBIntrospector;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FileImporter {
    public static final String ARTIFACTS_S = "artifacts/%s";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private final MinioAdapter minioAdapter;
    private final UrlValidationService urlValidationService;

    public FileImporter(MinioAdapter minioAdapter, UrlValidationService urlValidationService) {
        this.minioAdapter = minioAdapter;
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

    public CseCrac importCseCrac(String cracUrl) throws IOException {
        InputStream cracInputStream = urlValidationService.openUrlStream(cracUrl);
        CseCracImporter cseCracImporter = new CseCracImporter();
        return cseCracImporter.importNativeCrac(cracInputStream);
    }

    public Crac importCrac(CseCrac cseCrac, OffsetDateTime targetProcessDateTime, Network network) {
        //CracCreationParameters cracCreationParameters = CracCreationParameters.load(); todo specific treatment for gridcapa cse import. Need the same for validation?
        //CseCracCreationParameters cseCracCreationParameters = new CseCracCreationParameters();
        //cseCracCreationParameters.setBusBarChangeSwitchesSet(busBarChangeSwitchesSet);
        //cracCreationParameters.addExtension(CseCracCreationParameters.class, cseCracCreationParameters);
        return CracCreators.createCrac(cseCrac, network, targetProcessDateTime).getCrac();
    }

    public Crac importCracFromJson(String cracUrl) {
        try (InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl)) {
            return CracImporters.importCrac(FilenameUtils.getName(new URL(cracUrl).getPath()), cracResultStream);
        } catch (IOException e) {
            throw new CseValidInvalidDataException(String.format("Cannot import crac from JSON : %s", cracUrl));
        }
    }

    public String buildTtcFileUrl(CseValidRequest cseValidRequest) { // todo why not using cseValidRequest.getTtcAdjustment().getUrl() ??
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

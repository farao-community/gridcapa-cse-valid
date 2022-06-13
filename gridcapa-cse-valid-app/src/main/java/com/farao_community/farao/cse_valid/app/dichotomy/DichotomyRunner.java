/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.index.RangeDivisionIndexStrategy;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DichotomyRunner.class);
    private static final RangeDivisionIndexStrategy INDEX_STRATEGY_CONFIGURATION = new RangeDivisionIndexStrategy(false);
    private static final double SHIFT_TOLERANCE = 1;
    private final FileImporter fileImporter;
    private final MinioAdapter minioAdapter;
    private final RaoRunnerClient raoRunnerClient;
    private GlskDocument glskDocument;
    private Network network;

    public DichotomyRunner(FileImporter fileImporter, MinioAdapter minioAdapter, RaoRunnerClient raoRunnerClient) {
        this.fileImporter = fileImporter;
        this.minioAdapter = minioAdapter;
        this.raoRunnerClient = raoRunnerClient;
    }

    public DichotomyResult<RaoResponse> runDichotomy(CseValidRequest cseValidRequest, TTimestamp timestamp) {
        importFiles(cseValidRequest);
        int npAugmented = timestamp.getMNII().getV().intValue();
        int np = timestamp.getMiBNII().getV().intValue() - timestamp.getANTCFinal().getV().intValue();
        double minValue = 0;
        double maxValue = (double) npAugmented - np;
        double precision = 50;

        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
                new Index<>(minValue, maxValue, precision),
                INDEX_STRATEGY_CONFIGURATION,
                getNetworkShifter(timestamp.getSplittingFactors()),
                getNetworkValidator(cseValidRequest));
        return engine.run(network);
    }

    private void importFiles(CseValidRequest cseValidRequest) {
        try {
            this.glskDocument = importGlskFile(cseValidRequest);
            this.network = importNetworkFile(cseValidRequest);
        } catch (IOException e) {
            LOGGER.error("Can not import files");
            throw new CseValidInvalidDataException("Can not import files");
        }
    }

    private NetworkShifter getNetworkShifter(TSplittingFactors splittingFactors) {
        return new LinearScaler(glskDocument.getZonalScalable(network),
                new SplittingFactors(convertSplittingFactors(splittingFactors)),
                SHIFT_TOLERANCE);
    }

    private Map<String, Double> convertSplittingFactors(TSplittingFactors tSplittingFactors) {
        Map<String, Double> splittingFactors = new TreeMap<>();
        tSplittingFactors.getSplittingFactor().forEach(factor -> splittingFactors.put(toEic(factor.getCountry().getV()), factor.getFactor().getV().doubleValue()));
        splittingFactors.put(toEic("IT"), -1.);
        return splittingFactors;
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }

    private NetworkValidator<RaoResponse> getNetworkValidator(CseValidRequest cseValidRequest) {
        return new RaoValidator(cseValidRequest, raoRunnerClient, fileImporter);
    }

    public GlskDocument importGlskFile(CseValidRequest cseValidRequest) throws IOException {
        String url = fileImporter.buildGlskFileUrl(cseValidRequest);
        String file = minioAdapter.generatePreSignedUrl(url);
        return fileImporter.importGlsk(file);
    }

    public Network importNetworkFile(CseValidRequest cseValidRequest) throws IOException {
        String url = fileImporter.buildNetworkFileUrl(cseValidRequest);
        String fileUrl = minioAdapter.generatePreSignedUrl(url);
        return fileImporter.importNetwork(cseValidRequest.getCgm().getFilename(), fileUrl);
    }
}

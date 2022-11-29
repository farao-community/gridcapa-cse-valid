/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.index.RangeDivisionIndexStrategy;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final RangeDivisionIndexStrategy INDEX_STRATEGY_CONFIGURATION = new RangeDivisionIndexStrategy(false);
    private static final String DICHOTOMY_PARAMETERS_MSG = "Minimum dichotomy index: {}, Maximum dichotomy index: {}, Dichotomy precision: {}";
    private static final double DEFAULT_DICHOTOMY_PRECISION = 50;
    private static final int DEFAULT_MIN_INDEX = 0;
    private static final double SHIFT_TOLERANCE = 1;

    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final RaoRunnerClient raoRunnerClient;
    private final Logger businessLogger;

    public DichotomyRunner(FileImporter fileImporter, FileExporter fileExporter, RaoRunnerClient raoRunnerClient, Logger businessLogger) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.raoRunnerClient = raoRunnerClient;
        this.businessLogger = businessLogger;
    }

    public DichotomyResult<RaoResponse> runDichotomy(CseValidRequest cseValidRequest, TTimestamp timestamp) {
        int npAugmented = timestamp.getMNII().getV().intValue();
        int np = timestamp.getMiBNII().getV().intValue() - timestamp.getANTCFinal().getV().intValue();
        double maxValue = (double) npAugmented - np;
        Network network = importNetworkFile(cseValidRequest);
        String jsonCracUrl = getJsonCracUrl(cseValidRequest, network);
        businessLogger.info(DICHOTOMY_PARAMETERS_MSG, DEFAULT_MIN_INDEX, (int) maxValue, (int) DEFAULT_DICHOTOMY_PRECISION);
        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
                new Index<>(DEFAULT_MIN_INDEX, maxValue, DEFAULT_DICHOTOMY_PRECISION),
                INDEX_STRATEGY_CONFIGURATION,
                getNetworkShifter(timestamp.getSplittingFactors(), network, cseValidRequest),
                getNetworkValidator(cseValidRequest, jsonCracUrl));
        return engine.run(network);
    }

    private String getJsonCracUrl(CseValidRequest cseValidRequest, Network network) {
        CseCrac cseCrac = fileImporter.importCseCrac(cseValidRequest.getImportCrac().getUrl());
        Crac crac = fileImporter.importCrac(cseCrac, cseValidRequest.getTimestamp(), network);
        return fileExporter.saveCracInJsonFormat(crac, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
    }

    private NetworkShifter getNetworkShifter(TSplittingFactors splittingFactors, Network network, CseValidRequest cseValidRequest) {
        GlskDocument glskDocument = fileImporter.importGlsk(cseValidRequest.getGlsk().getUrl());
        return new LinearScaler(
            glskDocument.getZonalScalable(network),
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

    private NetworkValidator<RaoResponse> getNetworkValidator(CseValidRequest cseValidRequest, String jsonCracUrl) {
        String raoParametersURL = fileExporter.saveRaoParameters(cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
        return new RaoValidator(
                cseValidRequest.getProcessType(),
                cseValidRequest.getId(),
                cseValidRequest.getTimestamp(),
                jsonCracUrl,
                raoParametersURL,
                raoRunnerClient,
                fileImporter,
                fileExporter);
    }

    public Network importNetworkFile(CseValidRequest cseValidRequest) {
        return fileImporter.importNetwork(cseValidRequest.getCgm().getFilename(), cseValidRequest.getCgm().getUrl());
    }
}

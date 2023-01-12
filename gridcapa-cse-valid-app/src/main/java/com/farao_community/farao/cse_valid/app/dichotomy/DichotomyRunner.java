/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.CseValidNetworkShifter;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.TTimestampWrapper;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.index.RangeDivisionIndexStrategy;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final RangeDivisionIndexStrategy INDEX_STRATEGY_CONFIGURATION = new RangeDivisionIndexStrategy(false);
    private static final String DICHOTOMY_PARAMETERS_MSG = "Minimum dichotomy index: {}, Maximum dichotomy index: {}, Dichotomy precision: {}";
    private static final double DEFAULT_DICHOTOMY_PRECISION = 50;
    private static final int DEFAULT_MIN_INDEX = 0;

    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final RaoRunnerClient raoRunnerClient;
    private final Logger businessLogger;
    private final CseValidNetworkShifter cseValidNetworkShifter;

    public DichotomyRunner(FileImporter fileImporter,
                           FileExporter fileExporter,
                           RaoRunnerClient raoRunnerClient,
                           Logger businessLogger,
                           CseValidNetworkShifter cseValidNetworkShifter) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.raoRunnerClient = raoRunnerClient;
        this.businessLogger = businessLogger;
        this.cseValidNetworkShifter = cseValidNetworkShifter;
    }

    public DichotomyResult<RaoResponse> runImportCornerDichotomy(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, String jsonCracUrl, String raoParametersURL) {
        int npAugmented = timestampWrapper.getMniiIntValue();
        int np = timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue();
        double maxValue = (double) npAugmented - np;
        Network network = fileImporter.importNetwork(cseValidRequest.getCgm().getUrl());
        businessLogger.info(DICHOTOMY_PARAMETERS_MSG, DEFAULT_MIN_INDEX, (int) maxValue, (int) DEFAULT_DICHOTOMY_PRECISION);
        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
                new Index<>(DEFAULT_MIN_INDEX, maxValue, DEFAULT_DICHOTOMY_PRECISION),
                INDEX_STRATEGY_CONFIGURATION,
                cseValidNetworkShifter.getNetworkShifterWithSplittingFactors(timestampWrapper, network, cseValidRequest.getGlsk().getUrl()),
                getNetworkValidator(cseValidRequest, jsonCracUrl, raoParametersURL));
        return engine.run(network);
    }

    public DichotomyResult<RaoResponse> runExportCornerDichotomy(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, String jsonCracUrl, String raoParametersURL) {
        int npAugmented = timestampWrapper.getMiecIntValue();
        int np = timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue();
        double maxValue = (double) npAugmented - np;
        Network network = fileImporter.importNetwork(cseValidRequest.getCgm().getUrl());
        businessLogger.info(DICHOTOMY_PARAMETERS_MSG, DEFAULT_MIN_INDEX, (int) maxValue, (int) DEFAULT_DICHOTOMY_PRECISION);
        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
                new Index<>(DEFAULT_MIN_INDEX, maxValue, DEFAULT_DICHOTOMY_PRECISION),
                INDEX_STRATEGY_CONFIGURATION,
                cseValidNetworkShifter.getNetworkShifterReduceToFranceAndItaly(timestampWrapper, network, cseValidRequest.getGlsk().getUrl()),
                getNetworkValidator(cseValidRequest, jsonCracUrl, raoParametersURL));
        return engine.run(network);
    }

    private NetworkValidator<RaoResponse> getNetworkValidator(CseValidRequest cseValidRequest, String jsonCracUrl, String raoParametersURL) {
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
}

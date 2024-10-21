/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.CseValidNetworkShifterProvider;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.TTimestampWrapper;
import com.farao_community.farao.cse_valid.app.helper.NetPositionHelper;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.index.RangeDivisionIndexStrategy;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final RangeDivisionIndexStrategy INDEX_STRATEGY_CONFIGURATION = new RangeDivisionIndexStrategy(false);
    private static final String DICHOTOMY_PARAMETERS_MSG = "Minimum dichotomy index: {}, Maximum dichotomy index: {}, Dichotomy precision: {}";
    private static final double DEFAULT_DICHOTOMY_PRECISION = 50;
    private static final int DEFAULT_MIN_INDEX = 0;
    private static final int DEFAULT_MAX_INDEX = 0;

    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final RaoRunnerClient raoRunnerClient;
    private final Logger businessLogger;
    private final CseValidNetworkShifterProvider cseValidNetworkShifterProvider;

    public DichotomyRunner(FileImporter fileImporter,
                           FileExporter fileExporter,
                           RaoRunnerClient raoRunnerClient,
                           Logger businessLogger,
                           CseValidNetworkShifterProvider cseValidNetworkShifterProvider) {
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.raoRunnerClient = raoRunnerClient;
        this.businessLogger = businessLogger;
        this.cseValidNetworkShifterProvider = cseValidNetworkShifterProvider;
    }

    public DichotomyResult<RaoSuccessResponse> runDichotomy(TTimestampWrapper timestampWrapper,
                                                     CseValidRequest cseValidRequest,
                                                     String jsonCracUrl,
                                                     String raoParametersURL,
                                                     Network network,
                                                     boolean isForExportCorner) {
        final double minValue;
        final double maxValue;
        final NetworkShifter networkShifter;
        if (isForExportCorner) {
            Network initialNetwork = fileImporter.importNetwork(cseValidRequest.getCgm().getUrl());
            double franceImportBeforeShifting = NetPositionHelper.computeFranceImportFromItaly(initialNetwork);
            double franceImportAfterShifting = NetPositionHelper.computeFranceImportFromItaly(network);
            minValue = timestampWrapper.isFranceImportingFromItaly()
                    ? franceImportBeforeShifting - franceImportAfterShifting
                    : franceImportAfterShifting - franceImportBeforeShifting;
            maxValue = DEFAULT_MAX_INDEX;
            networkShifter = cseValidNetworkShifterProvider.getNetworkShifterForExportCornerWithItalyFrance(timestampWrapper, network, cseValidRequest.getGlsk().getUrl(), cseValidRequest.getProcessType());
        } else {
            minValue = DEFAULT_MIN_INDEX;
            maxValue = (double) timestampWrapper.getMniiIntValue() - (timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue());
            networkShifter = cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, cseValidRequest.getGlsk().getUrl(), cseValidRequest.getProcessType());
        }
        businessLogger.info(DICHOTOMY_PARAMETERS_MSG, (int) minValue, (int) maxValue, (int) DEFAULT_DICHOTOMY_PRECISION);
        NetworkValidator<RaoSuccessResponse> networkValidator = getNetworkValidator(cseValidRequest, jsonCracUrl, raoParametersURL);
        DichotomyEngine<RaoSuccessResponse> engine = getDichotomyEngine(minValue, maxValue, networkShifter, networkValidator);
        return engine.run(network);
    }

    DichotomyEngine<RaoSuccessResponse> getDichotomyEngine(double minValue, double maxValue, NetworkShifter networkShifter, NetworkValidator<RaoSuccessResponse> networkValidator) {
        return new DichotomyEngine<>(
                new Index<>(minValue, maxValue, DEFAULT_DICHOTOMY_PRECISION),
                INDEX_STRATEGY_CONFIGURATION,
                networkShifter,
                networkValidator);
    }

    NetworkValidator<RaoSuccessResponse> getNetworkValidator(CseValidRequest cseValidRequest, String jsonCracUrl, String raoParametersURL) {
        return new DichotomyNetworkValidator(
                cseValidRequest,
                jsonCracUrl,
                raoParametersURL,
                raoRunnerClient,
                fileImporter,
                fileExporter);
    }
}

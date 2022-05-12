/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.index.RangeDivisionIndexStrategy;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Network;
import com.rte_france.farao.cep_seventy_validation.timestamp_validation.ttc_adjustment.TTimestamp;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyRunner {

    private static final RangeDivisionIndexStrategy INDEX_STRATEGY_CONFIGURATION = new RangeDivisionIndexStrategy(false);
    private final FileImporter fileImporter;
    private GlskDocument glskDocument;
    private Network network;

    public DichotomyRunner(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public DichotomyResult<RaoResponse> runDichotomy(CseValidRequest cseValidRequest, TTimestamp timestamp) throws IOException {
        importFiles(cseValidRequest);
        int npAugmented = timestamp.getMNII().getV().intValue();
        int np = timestamp.getMiBNII().getV().intValue() - timestamp.getANTCFinal().getV().intValue();
        double minValue = 0;
        double maxValue = npAugmented - np;
        double precision = 50;
        Index<RaoResponse> index = new Index<>(minValue, maxValue, precision);

        DichotomyEngine<RaoResponse> engine = new DichotomyEngine<>(
                new Index<>(minValue, maxValue, precision),
                INDEX_STRATEGY_CONFIGURATION,
                getNetworkShifter(),
                null);
        return engine.run(null);

    }

    private void importFiles(CseValidRequest cseValidRequest) {

    }

    private NetworkShifter getNetworkShifter() throws IOException {
        return new LinearScaler(
                fileImporter.importGlsk("").getZonalScalable(null),
                new SplittingFactors(null));
    }
}

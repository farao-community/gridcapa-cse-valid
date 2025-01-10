/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.helper;

import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.powsybl.balances_adjustment.util.CountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public final class NetPositionHelper {

    private NetPositionHelper() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(NetPositionHelper.class);

    public static double computeItalianImport(Network network) {
        runLoadFlow(network);
        CountryArea itArea = new CountryAreaFactory(Country.IT).create(network);
        return Stream.of(Country.FR, Country.AT, Country.CH, Country.SI)
                .map(country -> new CountryAreaFactory(country).create(network).getLeavingFlowToCountry(itArea))
                .reduce(0., Double::sum);
    }

    public static double computeFranceImportFromItaly(Network network) {
        runLoadFlow(network);
        return new CountryAreaFactory(Country.IT).create(network).getLeavingFlowToCountry(new CountryAreaFactory(Country.FR).create(network));
    }

    private static void runLoadFlow(Network network) {
        LoadFlowResult result = LoadFlow.run(network, LoadFlowParameters.load());
        if (result.isFailed()) {
            LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
            throw new CseValidInternalException(String.format("Loadflow computation diverged on network %s", network.getId()));
        }
    }
}

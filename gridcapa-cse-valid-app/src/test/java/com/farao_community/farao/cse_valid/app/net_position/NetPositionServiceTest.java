/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.net_position;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class NetPositionServiceTest {
    private static final double EPSILON = 1e-3;

    @Autowired
    private NetPositionService netPositionService;

    @Test
    void checkNetPositionReportCorrectlyGenerated() throws URISyntaxException {
        String networkUrl = getClass().getResource("/TestCase12Nodes.uct").toURI().toString();
        NetPositionReport netPositionReport = netPositionService.generateNetPositionReport(networkUrl);
        assertThat(netPositionReport.getAreasReport().get("BE").getNetPosition(), is(closeTo(2000., EPSILON)));
        assertThat(netPositionReport.getAreasReport().get("DE").getNetPosition(), is(closeTo(-2500., EPSILON)));
        assertThat(netPositionReport.getAreasReport().get("FR").getNetPosition(), is(closeTo(1000., EPSILON)));
        assertThat(netPositionReport.getAreasReport().get("NL").getNetPosition(), is(closeTo(0., EPSILON)));
    }
}

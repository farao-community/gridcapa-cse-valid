/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.utils;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import xsd.etso_core_cmpts.AreaType;

import java.util.List;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public final class TCalculationDirectionTestData {

    private TCalculationDirectionTestData() {
    }

    public static List<TCalculationDirection> getTCalculationDirectionListWithFranceInArea() {
        return List.of(
                getTCalculationDirection("10YIT-GRTN-----B", "10YCH-SWISSGRIDZ"),
                getTCalculationDirection("10YIT-GRTN-----B", "10YAT-APG------L"),
                getTCalculationDirection("10YIT-GRTN-----B", "10YSI-ELES-----O"),
                getTCalculationDirection("10YFR-RTE------C", "10YIT-GRTN-----B")
        );
    }

    public static List<TCalculationDirection> getTCalculationDirectionListWithFranceOutArea() {
        return List.of(
                getTCalculationDirection("10YIT-GRTN-----B", "10YFR-RTE------C"),
                getTCalculationDirection("10YIT-GRTN-----B", "10YAT-APG------L"),
                getTCalculationDirection("10YIT-GRTN-----B", "10YSI-ELES-----O"),
                getTCalculationDirection("10YCH-SWISSGRIDZ", "10YIT-GRTN-----B")
        );
    }

    private static TCalculationDirection getTCalculationDirection(String countryEicInArea, String countryEicOutArea) {
        AreaType inArea = new AreaType();
        inArea.setV(countryEicInArea);

        AreaType outArea = new AreaType();
        outArea.setV(countryEicOutArea);

        TCalculationDirection tCalculationDirection = new TCalculationDirection();
        tCalculationDirection.setInArea(inArea);
        tCalculationDirection.setOutArea(outArea);

        return tCalculationDirection;
    }
}

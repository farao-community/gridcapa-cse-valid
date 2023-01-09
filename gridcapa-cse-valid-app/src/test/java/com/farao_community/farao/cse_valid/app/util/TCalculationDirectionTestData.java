package com.farao_community.farao.cse_valid.app.util;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import xsd.etso_core_cmpts.AreaType;

import java.util.List;

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

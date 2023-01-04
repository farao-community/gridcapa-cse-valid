package com.farao_community.farao.cse_valid.app.util;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import xsd.etso_core_cmpts.AreaType;

import java.util.List;

public final class TCalculationDirectionTestData {

    private TCalculationDirectionTestData() {
    }

    public static List<TCalculationDirection> getTCalculationDirectionListWithFranceInArea() {
        return List.of(getTCalculationDirectionWithFranceInArea());
    }

    public static List<TCalculationDirection> getTCalculationDirectionListWithFranceOutArea() {
        return List.of(getTCalculationDirectionWithFranceOutArea());
    }

    public static TCalculationDirection getTCalculationDirectionWithFranceInArea() {
        AreaType inArea = new AreaType();
        inArea.setV("10YFR-RTE------C");

        AreaType outArea = new AreaType();
        outArea.setV("10YIT-GRTN-----B");

        TCalculationDirection tCalculationDirection = new TCalculationDirection();
        tCalculationDirection.setInArea(inArea);
        tCalculationDirection.setOutArea(outArea);

        return tCalculationDirection;
    }

    public static TCalculationDirection getTCalculationDirectionWithFranceOutArea() {
        AreaType inArea = new AreaType();
        inArea.setV("10YIT-GRTN-----B");

        AreaType outArea = new AreaType();
        outArea.setV("10YFR-RTE------C");

        TCalculationDirection tCalculationDirection = new TCalculationDirection();
        tCalculationDirection.setInArea(inArea);
        tCalculationDirection.setOutArea(outArea);

        return tCalculationDirection;
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.util;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirections;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TFactor;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import xsd.etso_code_lists.CodingSchemeType;
import xsd.etso_core_cmpts.AreaType;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.util.List;

import static com.farao_community.farao.cse_valid.app.Constants.IN_AREA;
import static com.farao_community.farao.cse_valid.app.Constants.OUT_AREA;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

public final class TimeStampTestData {

    private TimeStampTestData() {
    }

    /* --------------- IMPORT CORNER --------------- */

    public static TTimestamp getTimeStampWithMniiAndMnie() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);

        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.TEN);
        timestamp.setMNIE(mnieValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMnieMiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.ONE);
        timestamp.setMNIE(mnieValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMniiAndMiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMnie() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mnieValue = new QuantityType();
        mnieValue.setV(BigDecimal.ZERO);
        timestamp.setMNIE(mnieValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMnii() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMibniiAndAntcfinalBothZero() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ZERO);
        timestamp.setMiBNII(mibniiValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMniiAndAntcfinal() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMniiAndMibnii() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ZERO);
        timestamp.setMNII(mniiValue);

        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ZERO);
        timestamp.setMiBNII(mibniiValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcAboveTarget() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.ONE);
        timestamp.setMNII(mniiValue);

        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.TEN);
        timestamp.setMiBNII(mibniiValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType mniiValue = new QuantityType();
        mniiValue.setV(BigDecimal.TEN);
        timestamp.setMNII(mniiValue);

        QuantityType mibniiValue = new QuantityType();
        mibniiValue.setV(BigDecimal.ONE);
        timestamp.setMiBNII(mibniiValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    /* --------------- EXPORT CORNER --------------- */

    public static TTimestamp getTimeStampWithMiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMibiecAndAntcfinalBothZero() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ZERO);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMiecAndAntcfinal() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMiecAndMibiec() {
        TTimestamp timestamp = new TTimestamp();

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ZERO);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ZERO);
        timestamp.setMiBIEC(mibiecValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithoutShiftingFactors() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ONE);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.TEN);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithoutCalculationDirections() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ONE);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.TEN);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = new TShiftingFactors();
        shiftingFactors.getShiftingFactor().add(new TFactor());
        timestamp.setShiftingFactors(shiftingFactors);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithMiecAndMibiecAndAntcfinalAndActualNtcAboveTarget() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.ONE);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.TEN);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = new TShiftingFactors();
        shiftingFactors.getShiftingFactor().add(new TFactor());
        timestamp.setShiftingFactors(shiftingFactors);

        TCalculationDirections calculationDirections = new TCalculationDirections();
        calculationDirections.getCalculationDirection().add(new TCalculationDirection());
        timestamp.getCalculationDirections().add(calculationDirections);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithoutFranceInAreaOrOutArea() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ONE);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = new TShiftingFactors();
        shiftingFactors.getShiftingFactor().add(new TFactor());
        timestamp.setShiftingFactors(shiftingFactors);

        AreaType inArea = new AreaType();
        inArea.setV(IN_AREA);
        inArea.setCodingScheme(CodingSchemeType.A_01);

        AreaType outArea = new AreaType();
        outArea.setV(OUT_AREA);
        outArea.setCodingScheme(CodingSchemeType.A_01);

        TCalculationDirection tCalculationDirection = new TCalculationDirection();
        tCalculationDirection.setInArea(inArea);
        tCalculationDirection.setOutArea(outArea);

        TCalculationDirections tCalculationDirections = new TCalculationDirections();
        List<TCalculationDirection> tCalculationDirectionList = tCalculationDirections.getCalculationDirection();
        tCalculationDirectionList.add(tCalculationDirection);

        List<TCalculationDirections> tCalculationDirectionsList = timestamp.getCalculationDirections();
        tCalculationDirectionsList.add(tCalculationDirections);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithFranceInArea() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ONE);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = TSplittingAndShifttingFactorsTestData.getTShiftingFactors();
        timestamp.setShiftingFactors(shiftingFactors);

        TCalculationDirections tCalculationDirections = new TCalculationDirections();
        List<TCalculationDirection> tCalculationDirectionList = tCalculationDirections.getCalculationDirection();
        tCalculationDirectionList.add(TCalculationDirectionTestData.getTCalculationDirectionWithFranceInArea());

        List<TCalculationDirections> tCalculationDirectionsList = timestamp.getCalculationDirections();
        tCalculationDirectionsList.add(tCalculationDirections);

        return timestamp;
    }

    public static TTimestamp getTimeStampWithFranceOutArea() {
        TTimestamp timestamp = new TTimestamp();

        TTime timeValue = new TTime();
        timeValue.setV("time");
        timestamp.setTime(timeValue);

        QuantityType miecValue = new QuantityType();
        miecValue.setV(BigDecimal.TEN);
        timestamp.setMIEC(miecValue);

        QuantityType mibiecValue = new QuantityType();
        mibiecValue.setV(BigDecimal.ONE);
        timestamp.setMiBIEC(mibiecValue);

        QuantityType antcfinalValue = new QuantityType();
        antcfinalValue.setV(BigDecimal.ZERO);
        timestamp.setANTCFinal(antcfinalValue);

        TShiftingFactors shiftingFactors = TSplittingAndShifttingFactorsTestData.getTShiftingFactors();
        timestamp.setShiftingFactors(shiftingFactors);

        TCalculationDirections tCalculationDirections = new TCalculationDirections();
        List<TCalculationDirection> tCalculationDirectionList = tCalculationDirections.getCalculationDirection();
        tCalculationDirectionList.add(TCalculationDirectionTestData.getTCalculationDirectionWithFranceOutArea());

        List<TCalculationDirections> tCalculationDirectionsList = timestamp.getCalculationDirections();
        tCalculationDirectionsList.add(tCalculationDirections);

        return timestamp;
    }
}

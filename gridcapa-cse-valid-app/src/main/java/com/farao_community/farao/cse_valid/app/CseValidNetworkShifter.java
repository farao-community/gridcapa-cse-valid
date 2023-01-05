/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TShiftingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TSplittingFactors;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
public class CseValidNetworkShifter {

    private static final double SHIFT_TOLERANCE = 1;

    private final FileImporter fileImporter;

    public CseValidNetworkShifter(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public NetworkShifter getNetworkShifterWithSplittingFactors(TSplittingFactors tSplittingFactors,
                                                                Network network,
                                                                String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(getSplittingFactorsMap(tSplittingFactors)),
                SHIFT_TOLERANCE);
    }

    public NetworkShifter getNetworkShifterReduceToFranceAndItaly(boolean isExportCornerActive,
                                                                  Network network,
                                                                  String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(getShiftingFactorMapReduceToFranceAndItaly(isExportCornerActive)),
                SHIFT_TOLERANCE);
    }

    NetworkShifter getNetworkShifterWithShiftingFactors(TShiftingFactors tShiftingFactors,
                                                        List<TCalculationDirection> calculationDirections,
                                                        Network network,
                                                        String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(getShiftingFactorsMap(tShiftingFactors, calculationDirections)),
                SHIFT_TOLERANCE);
    }

    private Map<String, Double> getSplittingFactorsMap(TSplittingFactors tSplittingFactors) {
        Map<String, Double> splittingFactorsMap = tSplittingFactors.getSplittingFactor().stream()
                .collect(Collectors.toMap(
                    tFactor -> toEic(tFactor.getCountry().getV()),
                    tFactor -> tFactor.getFactor().getV().doubleValue()
                ));
        splittingFactorsMap.put(toEic(Country.IT), -1.);
        return splittingFactorsMap;
    }

    private Map<String, Double> getShiftingFactorMapReduceToFranceAndItaly(boolean isExportCornerActive) {
        Map<String, Double> result = new HashMap<>();
        double franceFactor = isExportCornerActive ? -1.0 : 1.0;
        result.put(toEic(Country.FR), franceFactor);
        result.put(toEic(Country.IT), franceFactor * -1);
        return result;
    }

    private Map<String, Double> getShiftingFactorsMap(TShiftingFactors tShiftingFactors, List<TCalculationDirection> calculationDirections) {
        return tShiftingFactors.getShiftingFactor().stream()
                .collect(Collectors.toMap(
                    tFactor -> toEic(tFactor.getCountry().getV()),
                    tFactor -> tFactor.getFactor().getV().doubleValue() * getFactorSignOfCountry(tFactor.getCountry().getV(), calculationDirections)
                ));
    }

    private double getFactorSignOfCountry(String country, List<TCalculationDirection> calculationDirections) {
        String countryEic = toEic(country);
        if (CseValidHandler.isCountryInArea(countryEic, calculationDirections)) {
            return -1;
        } else if (CseValidHandler.isCountryOutArea(countryEic, calculationDirections)) {
            return 1;
        }
        throw new CseValidInvalidDataException("Country " + country + " must appear in InArea or OutArea");
    }

    private String toEic(String country) {
        return toEic(Country.valueOf(country));
    }

    private String toEic(Country country) {
        return new EICode(country).getAreaCode();
    }

    public Network getNetworkShiftedWithShiftingFactors(TTimestamp timestamp, CseValidRequest cseValidRequest) {
        TShiftingFactors tShiftingFactors = timestamp.getShiftingFactors();
        List<TCalculationDirection> calculationDirections = timestamp.getCalculationDirections().get(0).getCalculationDirection();
        String cgmFileName = cseValidRequest.getCgm().getFilename();
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        Network network = fileImporter.importNetwork(cgmFileName, cgmUrl);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        NetworkShifter networkShifter = getNetworkShifterWithShiftingFactors(tShiftingFactors, calculationDirections, network, glskUrl);
        double shiftValue = computeShiftValue(timestamp);

        try {
            networkShifter.shiftNetwork(shiftValue, network);
            return network;
        } catch (GlskLimitationException | ShiftingException e) {
            throw new RuntimeException(e);
        }
    }

    private double computeShiftValue(TTimestamp timestamp) {
        BigDecimal miec = timestamp.getMIEC().getV();
        BigDecimal mibiec = timestamp.getMiBIEC().getV();
        BigDecimal antcFinal = timestamp.getANTCFinal().getV();
        return miec.subtract(mibiec.subtract(antcFinal)).doubleValue();
    }
}

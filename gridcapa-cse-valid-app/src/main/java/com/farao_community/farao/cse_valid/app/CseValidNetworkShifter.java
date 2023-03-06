/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@Component
public class CseValidNetworkShifter {

    private static final double SHIFT_TOLERANCE = 1;

    private final EicCodesConfiguration eicCodesConfiguration;
    private final ZonalScalableProvider zonalScalableProvider;

    public CseValidNetworkShifter(EicCodesConfiguration eicCodesConfiguration, ZonalScalableProvider zonalScalableProvider) {
        this.eicCodesConfiguration = eicCodesConfiguration;
        this.zonalScalableProvider = zonalScalableProvider;
    }

    private NetworkShifter getNetworkShifter(Map<String, Double> splittiFactorMap,
                                             Network network,
                                             String glskUrl,
                                             ProcessType processType) {
        return new LinearScaler(
            zonalScalableProvider.get(glskUrl, network, processType),
            new SplittingFactors(splittiFactorMap),
            SHIFT_TOLERANCE);
    }

    public NetworkShifter getNetworkShifterForFullImport(TTimestampWrapper timestampWrapper,
                                                         Network network,
                                                         String glskUrl,
                                                         ProcessType processType) {
        return getNetworkShifter(getSplittingFactorsForFullImport(timestampWrapper), network, glskUrl, processType);
    }

    public NetworkShifter getNetworkShifterForExportCornerWithItalyFrance(TTimestampWrapper timestampWrapper,
                                                                          Network network,
                                                                          String glskUrl,
                                                                          ProcessType processType) {
        return getNetworkShifter(getSplittingFactorsForExportCornerWithItalyFrance(timestampWrapper), network, glskUrl, processType);
    }

    NetworkShifter getNetworkShifterForExportCornerWithAllCountries(TTimestampWrapper timestampWrapper,
                                                                    Network network,
                                                                    String glskUrl,
                                                                    ProcessType processType) {
        return getNetworkShifter(getSplittingFactorsForExportCornerWithAllCountries(timestampWrapper), network, glskUrl, processType);
    }

    Map<String, Double> getSplittingFactorsForFullImport(TTimestampWrapper timestampWrapper) {
        Map<String, Double> splittingFactorsMap = timestampWrapper.getImportCornerSplittingFactors();
        splittingFactorsMap.put(eicCodesConfiguration.getItaly(), -1.);
        return splittingFactorsMap;
    }

    Map<String, Double> getSplittingFactorsForExportCornerWithItalyFrance(TTimestampWrapper timestampWrapper) {
        Map<String, Double> result = new HashMap<>();
        double franceFactor = timestampWrapper.isFranceImportingFromItaly() ? -1.0 : 1.0;
        result.put(eicCodesConfiguration.getFrance(), franceFactor);
        result.put(eicCodesConfiguration.getItaly(), franceFactor * -1);
        return result;
    }

    Map<String, Double> getSplittingFactorsForExportCornerWithAllCountries(TTimestampWrapper timestampWrapper) {
        Map<String, Double> exportCornerSplittingFactors =  timestampWrapper.getExportCornerSplittingFactors().entrySet().stream()
            .filter(stringDoubleEntry -> !stringDoubleEntry.getKey().equals(eicCodesConfiguration.getItaly()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                stringDoubleEntry -> stringDoubleEntry.getValue() * getFactorSignOfCountry(timestampWrapper, stringDoubleEntry.getKey())
            ));
        double sum = exportCornerSplittingFactors.values().stream().mapToDouble(Double::doubleValue).sum();
        exportCornerSplittingFactors.put(eicCodesConfiguration.getItaly(), -1 * sum);
        return exportCornerSplittingFactors;
    }

    private double getFactorSignOfCountry(TTimestampWrapper timestampWrapper, String countryEic) {
        return timestampWrapper.isCountryImportingFromItaly(countryEic) ? -1 : 1;
    }

    public void shiftNetwork(double shiftValue, Network network, TTimestampWrapper timestampWrapper, String glskUrl, ProcessType processType) {
        NetworkShifter networkShifter = getNetworkShifterForExportCornerWithAllCountries(timestampWrapper, network, glskUrl, processType);
        try {
            networkShifter.shiftNetwork(shiftValue, network);
        } catch (GlskLimitationException | ShiftingException e) {
            throw new CseValidShiftFailureException("Export corner initial shift failed to value " + shiftValue, e);
        }
    }
}

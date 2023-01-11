/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.powsybl.glsk.api.GlskDocument;
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

    private final FileImporter fileImporter;
    private final EicCodesConfiguration eicCodesConfiguration;

    public CseValidNetworkShifter(FileImporter fileImporter, EicCodesConfiguration eicCodesConfiguration) {
        this.fileImporter = fileImporter;
        this.eicCodesConfiguration = eicCodesConfiguration;
    }

    public NetworkShifter getNetworkShifterWithSplittingFactors(TTimestampWrapper timestampWrapper,
                                                                Network network,
                                                                String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(getImportCornerSplittingFactors(timestampWrapper)),
                SHIFT_TOLERANCE);
    }

    public NetworkShifter getNetworkShifterReduceToFranceAndItaly(TTimestampWrapper timestampWrapper,
                                                                  Network network,
                                                                  String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(getExportCornerSplittingFactorsMapReduceToFranceAndItaly(timestampWrapper)),
                SHIFT_TOLERANCE);
    }

    NetworkShifter getNetworkShifterWithShiftingFactors(TTimestampWrapper timestampWrapper,
                                                        Network network,
                                                        String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(getExportCornerSplittingFactors(timestampWrapper)),
                SHIFT_TOLERANCE);
    }

    Map<String, Double> getImportCornerSplittingFactors(TTimestampWrapper timestampWrapper) {
        Map<String, Double> splittingFactorsMap = timestampWrapper.getImportCornerSplittingFactors();
        splittingFactorsMap.put(eicCodesConfiguration.getItaly(), -1.);
        return splittingFactorsMap;
    }

    Map<String, Double> getExportCornerSplittingFactorsMapReduceToFranceAndItaly(TTimestampWrapper timestampWrapper) {
        Map<String, Double> result = new HashMap<>();
        double franceFactor = timestampWrapper.isExportCornerActiveForFrance() ? -1.0 : 1.0;
        result.put(eicCodesConfiguration.getFrance(), franceFactor);
        result.put(eicCodesConfiguration.getItaly(), franceFactor * -1);
        return result;
    }

    Map<String, Double> getExportCornerSplittingFactors(TTimestampWrapper timestampWrapper) {
        return timestampWrapper.getExportCornerSplittingFactors().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    stringDoubleEntry -> stringDoubleEntry.getValue() * getFactorSignOfCountry(timestampWrapper, stringDoubleEntry.getKey())
                ));
    }

    private double getFactorSignOfCountry(TTimestampWrapper timestampWrapper, String countryEic) {
        return timestampWrapper.isExportCornerActiveForCountry(countryEic) ? -1 : 1;
    }

    public void shiftNetwork(double shiftValue, Network network, TTimestampWrapper timestampWrapper, String glskUrl) {
        NetworkShifter networkShifter = getNetworkShifterWithShiftingFactors(timestampWrapper, network, glskUrl);
        try {
            networkShifter.shiftNetwork(shiftValue, network);
        } catch (GlskLimitationException | ShiftingException e) {
            throw new CseValidShiftFailureException(e.getMessage());
        }
    }
}

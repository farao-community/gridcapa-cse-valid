/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.app.exception.CseValidShiftFailureException;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Component;

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

    public NetworkShifter getNetworkShifterWithSplittingFactors(TTimestampWrapper timestampWrapper,
                                                                Network network,
                                                                String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(timestampWrapper.getImportCornerSplittingFactors()),
                SHIFT_TOLERANCE);
    }

    public NetworkShifter getNetworkShifterReduceToFranceAndItaly(TTimestampWrapper timestampWrapper,
                                                                  Network network,
                                                                  String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(timestampWrapper.getExportCornerSplittingFactorsMapReduceToFranceAndItaly()),
                SHIFT_TOLERANCE);
    }

    NetworkShifter getNetworkShifterWithShiftingFactors(TTimestampWrapper timestampWrapper,
                                                        Network network,
                                                        String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(timestampWrapper.getExportCornerSplittingFactors()),
                SHIFT_TOLERANCE);
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

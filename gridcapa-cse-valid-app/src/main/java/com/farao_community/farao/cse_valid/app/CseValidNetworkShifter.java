/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
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

    public NetworkShifter getNetworkShifterWithSplittingFactors(TSplittingFactors tSplittingFactors, Network network, String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(convertSplittingFactors(tSplittingFactors)),
                SHIFT_TOLERANCE);
    }

    public NetworkShifter getNetworkShifterWithShifttingFactors(TShiftingFactors tShiftingFactors, Network network, String glskUrl) {
        GlskDocument glskDocument = fileImporter.importGlsk(glskUrl);
        return new LinearScaler(
                glskDocument.getZonalScalable(network),
                new SplittingFactors(convertShifttingFactors(tShiftingFactors)),
                SHIFT_TOLERANCE);
    }

    private Map<String, Double> convertSplittingFactors(TSplittingFactors tSplittingFactors) {
        Map<String, Double> splittingFactorsMap = tSplittingFactors.getSplittingFactor().stream()
                .collect(Collectors.toMap(
                    f -> toEic(f.getCountry().getV()),
                    f -> f.getFactor().getV().doubleValue()
                ));
        splittingFactorsMap.put(toEic("IT"), -1.);
        return splittingFactorsMap;
    }

    private Map<String, Double> convertShifttingFactors(TShiftingFactors tShiftingFactors) {
        Map<String, Double> shifttingFactorsMap = tShiftingFactors.getShiftingFactor().stream()
                .collect(Collectors.toMap(
                    f -> toEic(f.getCountry().getV()),
                    f -> f.getFactor().getV().doubleValue()
                ));
        shifttingFactorsMap.put(toEic("IT"), -1.);
        return shifttingFactorsMap;
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }

    public void shiftNetworkWithShifttingFactors(TTimestamp timestamp, CseValidRequest cseValidRequest) {
        TShiftingFactors tShiftingFactors = timestamp.getShiftingFactors();
        String cgmFileName = cseValidRequest.getCgm().getFilename();
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        Network network = fileImporter.importNetwork(cgmFileName, cgmUrl);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        NetworkShifter networkShifter = getNetworkShifterWithShifttingFactors(tShiftingFactors, network, glskUrl);
        double shiftValue = computeShiftValue(timestamp);

        try {
            networkShifter.shiftNetwork(shiftValue, network);
        } catch (GlskLimitationException e) {
            throw new RuntimeException(e);
        } catch (ShiftingException e) {
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

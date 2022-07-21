/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.net_position;

import com.powsybl.iidm.network.*;

import java.util.Optional;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class NetPositionCalculator {

    private NetPositionCalculator() {
        throw new IllegalStateException("Utility class");
    }

    public static NetPositionReport generateNetPositionReport(Network network) {
        NetPositionReportBuilder reportBuilder = new NetPositionReportBuilder();
        network.getBranchStream()
                .filter(NetPositionCalculator::isCountryBorder)
                .forEach(branch -> addBorder(branch, reportBuilder));
        network.getDanglingLineStream()
                .filter(NetPositionCalculator::isCountryBorder)
                .forEach(danglingLine -> addBorder(danglingLine, reportBuilder));
        return reportBuilder.build();
    }

    private static void addBorder(Branch<?> branch, NetPositionReportBuilder reportBuilder) {
        String area1 = null;
        String area2 = null;

        Optional<Substation> substationTerminal1 = branch.getTerminal1().getVoltageLevel().getSubstation();
        if (substationTerminal1.isPresent()) {
            area1 = substationTerminal1.get().getNullableCountry().toString();
        }

        Optional<Substation> substationTerminal2 = branch.getTerminal2().getVoltageLevel().getSubstation();
        if (substationTerminal2.isPresent()) {
            area2 = substationTerminal2.get().getNullableCountry().toString();
        }

        double directMiddleFlow = getDirectMiddleFlow(branch);
        reportBuilder.addBorderResult(area1, area2, directMiddleFlow);
    }

    private static boolean isCountryBorder(Branch<?> branch) {
        Country countrySide1 = null;
        Country countrySide2 = null;

        Optional<Substation> substationTerminal1 = branch.getTerminal1().getVoltageLevel().getSubstation();
        if (substationTerminal1.isPresent()) {
            countrySide1 = substationTerminal1.get().getNullableCountry();
        }

        Optional<Substation> substationTerminal2 = branch.getTerminal2().getVoltageLevel().getSubstation();
        if (substationTerminal2.isPresent()) {
            countrySide2 = substationTerminal2.get().getNullableCountry();
        }

        if (countrySide1 == null || countrySide2 == null) {
            return false;
        }
        return countrySide1 != countrySide2;
    }

    private static double getDirectMiddleFlow(Branch<?> branch) {
        double flowSide1 = branch.getTerminal1().isConnected() && !Double.isNaN(branch.getTerminal1().getP()) ? branch.getTerminal1().getP() : 0;
        double flowSide2 = branch.getTerminal2().isConnected() && !Double.isNaN(branch.getTerminal2().getP()) ? branch.getTerminal2().getP() : 0;
        return (flowSide1 - flowSide2) / 2;
    }

    private static void addBorder(DanglingLine danglingLine, NetPositionReportBuilder reportBuilder) {
        String area = null;

        Optional<Substation> substation = danglingLine.getTerminal().getVoltageLevel().getSubstation();
        if (substation.isPresent()) {
            area = substation.get().getNullableCountry().toString();
        }

        double directMiddleFlow = getDirectMiddleFlow(danglingLine);
        reportBuilder.addBorderResult(area, "XX", directMiddleFlow);
    }

    private static boolean isCountryBorder(DanglingLine danglingLine) {
        Optional<Substation> substation = danglingLine.getTerminal().getVoltageLevel().getSubstation();
        return substation.map(value -> value.getCountry().isPresent()).orElse(false);
    }

    private static double getDirectMiddleFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() && !Double.isNaN(danglingLine.getTerminal().getP()) ? danglingLine.getTerminal().getP() : 0;
    }
}

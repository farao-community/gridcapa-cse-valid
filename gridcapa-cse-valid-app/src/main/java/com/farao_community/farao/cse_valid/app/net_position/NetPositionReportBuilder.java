/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.net_position;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class NetPositionReportBuilder {
    private final Map<String, Double> areasNetPosition = new TreeMap<>();
    private final Map<String, Map<String, Double>> bordersValues = new TreeMap<>();

    public void addBorderResult(String area1, String area2, double directMiddleFlow) {
        areasNetPosition.put(area1, areasNetPosition.getOrDefault(area1, 0.) + directMiddleFlow);
        areasNetPosition.put(area2, areasNetPosition.getOrDefault(area2, 0.) - directMiddleFlow);
        bordersValues.putIfAbsent(area1, new TreeMap<>());
        bordersValues.putIfAbsent(area2, new TreeMap<>());
        bordersValues.get(area1).put(area2, bordersValues.get(area1).getOrDefault(area2, 0.) + directMiddleFlow);
        bordersValues.get(area2).put(area1, bordersValues.get(area2).getOrDefault(area1, 0.) - directMiddleFlow);
    }

    public NetPositionReport build() {
        Map<String, AreaReport> areasReport = bordersValues.keySet().stream().collect(Collectors.toMap(area1 -> area1, this::buildAreaReport));
        return NetPositionReport.builder().areasReport(areasReport).build();
    }

    private AreaReport buildAreaReport(String areaId) {
        return AreaReport.builder()
            .id(areaId)
            .netPosition(areasNetPosition.get(areaId))
            .bordersExchanges(bordersValues.get(areaId))
            .build();
    }
}

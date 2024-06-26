/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import java.time.ZoneId;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public final class Constants {
    public static final String TIMESERIES_IDENTIFICATION_PATTERN = "yyyyMMdd'-0001'";
    public static final String SENDER_IDENTIFICATION = "10XFR-RTE------Q";
    public static final String RECEIVER_IDENTIFICATION = "10XFR-RTE------Q";
    public static final String DOMAIN = "10YDOM-1001A061T";
    public static final String IN_AREA = "10YIT-GRTN-----B";
    public static final String OUT_AREA = "10YDOM-1001A061T";
    public static final String PRODUCT = "8716867000016";
    public static final String ERROR_MSG_MISSING_TTC_ADJ_FILE = "Process fail during TSO validation phase: Missing TTC_adjustment file.";
    public static final String ERROR_MSG_MISSING_DATA = "Process fail during TSO validation phase: Missing datas.";
    public static final String ERROR_MSG_MISSING_SHIFTING_FACTORS = "Process fail during TSO validation phase: Missing ShiftingFactors block.";
    public static final String ERROR_MSG_MISSING_CALCULATION_DIRECTIONS = "Process fail during TSO validation phase: Missing CalculationDirections block.";
    public static final String ERROR_MSG_CONTRADICTORY_DATA = "Process fail during TSO validation phase: Contradictory datas.";
    public static final String ERROR_MSG_GENERIC = "Process fail during TSO validation phase.";
    public static final ZoneId EUROPE_BRUSSELS_ZONE_ID = ZoneId.of("Europe/Brussels");

    private Constants() { }
}

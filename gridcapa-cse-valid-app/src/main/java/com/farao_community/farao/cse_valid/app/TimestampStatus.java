/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public enum TimestampStatus {
    NO_TTC_ADJUSTMENT_ERROR,
    MISSING_DATAS,
    NO_COMPUTATION_NEEDED,
    NO_VERIFICATION_NEEDED,
    MISSING_INPUT_FILES,
    COMPUTATION_NEEDED
}

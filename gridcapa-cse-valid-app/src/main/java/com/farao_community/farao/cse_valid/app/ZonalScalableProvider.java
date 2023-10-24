/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Service
public class ZonalScalableProvider {

    private final FileImporter fileImporter;
    private final EicCodesConfiguration eicCodesConfiguration;
    private final EicCodesMapper eicCodesMapper;

    public ZonalScalableProvider(FileImporter fileImporter, EicCodesConfiguration eicCodesConfiguration, EicCodesMapper eicCodesMapper) {
        this.fileImporter = fileImporter;
        this.eicCodesConfiguration = eicCodesConfiguration;
        this.eicCodesMapper = eicCodesMapper;
    }

    public ZonalData<Scalable> get(String glskUrl, Network network, ProcessType processType) {
        ZonalData<Scalable> zonalScalable = fileImporter.importGlsk(glskUrl).getZonalScalable(network);
        eicCodesConfiguration.getCseCodes().forEach(eic -> checkCseCountryInGlsk(zonalScalable, eic));
        stackScalableOnLoads(network, zonalScalable, processType);
        return zonalScalable;
    }

    private static void checkCseCountryInGlsk(ZonalData<Scalable> zonalScalable, String eic) {
        if (!zonalScalable.getDataPerZone().containsKey(eic)) {
            throw new CseValidInvalidDataException(String.format("Area '%s' was not found in the glsk file.", eic));
        }
    }

    private void stackScalableOnLoads(Network network, ZonalData<Scalable> zonalScalable, ProcessType processType) {
        zonalScalable.getDataPerZone().forEach((eiCode, scalable) -> {
            if (processType == ProcessType.IDCC && eiCode.equals(eicCodesConfiguration.getItaly())) {
                return;
            }
            double sum = getZoneSumOfActiveLoads(network, eiCode);
            // No need to go further if a country has no active load
            if (sum == 0.0) {
                return;
            }
            Scalable stackedScalable = getStackedScalable(eiCode, scalable, network, sum);
            zonalScalable.getDataPerZone().put(eiCode, stackedScalable);
        });
    }

    private double getZoneSumOfActiveLoads(Network network, String eiCode) {
        return network.getLoadStream()
            .filter(load -> isLoadCorrespondingToTheCountry(load, eiCode))
            .map(Load::getP0)
            .reduce(0., Double::sum);
    }

    private Scalable getStackedScalable(String eiCode, Scalable scalable, Network network, double sum) {
        List<Double> percentageList = new ArrayList<>();
        List<Scalable> scalableList = new ArrayList<>();

        network.getLoadStream()
            .filter(load -> isLoadCorrespondingToTheCountry(load, eiCode))
            .forEach(load -> {
                percentageList.add((load.getP0() / sum) * 100);
                scalableList.add(Scalable.onLoad(load.getId()));
            });

        return Scalable.stack(scalable, Scalable.proportional(percentageList, scalableList));
    }

    private boolean isLoadCorrespondingToTheCountry(Load load, String eiCode) {
        return load.getTerminal().getVoltageLevel().getSubstation()
            .flatMap(Substation::getCountry)
            .map(eicCodesMapper::mapToEic)
            .map(eiCode::equals)
            .orElse(false);
    }
}

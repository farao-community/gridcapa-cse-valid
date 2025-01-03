/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.helper;

import com.farao_community.farao.cse_valid.app.exception.LimitingElementBuildException;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TArea;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCriticalBranch;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TMonitoredElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TOutage;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.io.commons.api.ElementaryCreationContext;
import com.powsybl.openrao.data.crac.io.cse.CseCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import xsd.etso_core_cmpts.TextType;

import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
public final class LimitingElementHelper {

    private LimitingElementHelper() {
        // Helper class, no instance needed
    }

    public static TLimitingElement getLimitingElement(RaoResult raoResult, CseCracCreationContext cracCreationContext, Network network) {
        TLimitingElement limitingElement = new TLimitingElement();
        FlowCnec worstCnec = getWorstCnecInMW(raoResult, cracCreationContext);
        if (worstCnec != null) {
            TCriticalBranch criticalBranch = getCriticalBranch(worstCnec, cracCreationContext, network);
            limitingElement.getCriticalBranch().add(criticalBranch);
        }
        return limitingElement;
    }

    private static FlowCnec getWorstCnecInMW(RaoResult raoResult, CseCracCreationContext cracCreationContext) {
        Crac crac = cracCreationContext.getCrac();

        FlowCnec worstCnec = null;
        double worstMargin = Double.MAX_VALUE;
        double margin;
        for (FlowCnec flowCnec : crac.getFlowCnecs()) {
            if (flowCnec.isOptimized()) {
                Instant optimizedInstant = flowCnec.getState().getInstant();
                margin = raoResult.getMargin(optimizedInstant, flowCnec, Unit.MEGAWATT);

                if (margin < worstMargin) {
                    worstMargin = margin;
                    worstCnec = flowCnec;
                }
            }
        }
        return worstCnec;
    }

    private static TCriticalBranch getCriticalBranch(FlowCnec cnec, CseCracCreationContext context, Network network) {
        TCriticalBranch criticalBranch = new TCriticalBranch();

        cnec.getState().getContingency()
                .ifPresent(contingency -> criticalBranch.setOutage(getOutage(contingency, context, network)));
        criticalBranch.setMonitoredElement(getMonitoredElement(cnec, network));

        return criticalBranch;
    }

    private static TOutage getOutage(Contingency contingency, CseCracCreationContext cracCreationContext, Network network) {
        ElementaryCreationContext outageMatchingContingencyId = cracCreationContext.getOutageCreationContexts().stream()
                .filter(outageCreationContext -> outageCreationContext.isImported()
                        && outageCreationContext.getCreatedObjectId().equals(contingency.getId()))
                .collect(toOne());

        TOutage outage = new TOutage();
        outage.setName(getTextType(outageMatchingContingencyId.getNativeObjectId()));

        contingency.getElements().forEach(contingencyElement -> {
            TElement outageElement = getElement(network, contingencyElement);
            outage.getElement().add(outageElement);
        });

        return outage;
    }

    private static TElement getElement(Network network, ContingencyElement contingencyElement) {
        Branch<?> branch = network.getBranch(contingencyElement.getId());

        TElement element = new TElement();
        element.setCode(getTextType(contingencyElement.getId()));
        element.setAreafrom(getAreaFrom(branch));
        element.setAreato(getAreaTo(branch));
        return element;
    }

    private static TElement getElement(Network network, NetworkElement networkElement) {
        Branch<?> branch = network.getBranch(networkElement.getId());

        TElement element = new TElement();
        element.setCode(getTextType(networkElement.getId()));
        element.setAreafrom(getAreaFrom(branch));
        element.setAreato(getAreaTo(branch));
        return element;
    }

    private static TextType getTextType(String value) {
        TextType textType = new TextType();
        textType.setV(value);
        return textType;
    }

    private static TArea getAreaFrom(Branch<?> branch) {
        return getArea(branch.getTerminal1());
    }

    private static TArea getAreaTo(Branch<?> branch) {
        return getArea(branch.getTerminal2());
    }

    private static TArea getArea(Terminal terminal) {
        Optional<Substation> substation = terminal.getVoltageLevel().getSubstation();
        String areaTo = substation.map(Substation::getCountry)
                .flatMap(country -> country.map(Enum::toString))
                .orElse(null);

        TArea tArea = new TArea();
        tArea.setV(areaTo);
        return tArea;
    }

    private static TMonitoredElement getMonitoredElement(FlowCnec cnec, Network network) {
        TElement element = getElement(network, cnec.getNetworkElement());
        element.setName(getTextType(cnec.getName()));

        TMonitoredElement monitoredElement = new TMonitoredElement();
        monitoredElement.getElement().add(element);
        return monitoredElement;
    }

    /**
     * This collector only allows 1 element in the stream. It returns the result.
     *
     * @param <T> Type of the element for the collector.
     * @return The value if there is exactly one in the stream.
     * It would throw an exception if there isn't exactly one element (zero or more) in the stream.
     */
    private static <T> Collector<T, ?, T> toOne() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() == 1) {
                return list.get(0);
            }
            throw new LimitingElementBuildException("Found " + list.size() + " element(s), expected exactly one.");
        });
    }
}

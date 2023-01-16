/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.exception.LimitingElementBuildException;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TArea;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCriticalBranch;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TMonitoredElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TOutage;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cse.outage.CseOutageCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import org.apache.commons.lang3.tuple.ImmutablePair;
import xsd.etso_core_cmpts.TextType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public class LimitingElementHelper {

    private LimitingElementHelper() {
        // Helper class, no instance needed
    }

    public static TLimitingElement getLimitingElement(DichotomyStepResult<RaoResponse> stepResult, CseCracCreationContext cracCreationContext, Network network, FileImporter fileImporter) {
        TLimitingElement limitingElement = new TLimitingElement();
        ImmutablePair<FlowCnec, Double> worstCnec = getWorstCnecInMW(stepResult, fileImporter);
        List<TCriticalBranch> listCriticalBranches = limitingElement.getCriticalBranch();
        TCriticalBranch criticalBranch = getCriticalBranch(worstCnec.getLeft(), cracCreationContext, network);
        listCriticalBranches.add(criticalBranch);
        return limitingElement;
    }

    private static ImmutablePair<FlowCnec, Double> getWorstCnecInMW(DichotomyStepResult<RaoResponse> stepResult, FileImporter fileImporter) {
        Crac crac = fileImporter.importCracFromJson(stepResult.getValidationData().getCracFileUrl());
        RaoResult raoResult = fileImporter.importRaoResult(stepResult.getValidationData().getRaoResultFileUrl(), crac);

        FlowCnec worstCnec = null;
        double worstMargin = Double.MAX_VALUE;
        double margin;
        for (FlowCnec flowCnec : crac.getFlowCnecs()) {
            OptimizationState optimizationState = OptimizationState.afterOptimizing(flowCnec.getState());
            margin = raoResult.getMargin(optimizationState, flowCnec, Unit.MEGAWATT);

            if (margin < worstMargin) {
                worstMargin = margin;
                worstCnec = flowCnec;
            }
        }
        return new ImmutablePair<>(worstCnec, worstMargin);
    }

    private static TCriticalBranch getCriticalBranch(FlowCnec worstCnec, CseCracCreationContext context, Network network) {
        TCriticalBranch criticalBranch = new TCriticalBranch();

        criticalBranch.setOutage(getOutage(worstCnec, context, network));
        criticalBranch.setMonitoredElement(getMonitoredElement(worstCnec, network));

        return criticalBranch;
    }

    private static TOutage getOutage(FlowCnec worstCnec, CseCracCreationContext cracCreationContext, Network network) {
        Optional<Contingency> contingency = worstCnec.getState().getContingency();

        if (worstCnec.getState().isPreventive() || contingency.isEmpty()) {
            return null; // If preventive state, no outage is associated with the critical branch
        }

        CseOutageCreationContext outageMatchingContingencyId = cracCreationContext.getOutageCreationContexts().stream()
                .filter(outageCreationContext -> outageCreationContext.isImported()
                        && outageCreationContext.getCreatedContingencyId().equals(contingency.get().getId()))
                .collect(toOne());

        TOutage outage = new TOutage();
        outage.setName(getTextType(outageMatchingContingencyId.getNativeId()));

        contingency.get().getNetworkElements().forEach(contingencyNetworkElement -> {
            TElement outageElement = getElement(network, contingencyNetworkElement);
            outage.getElement().add(outageElement);
        });

        return outage;
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

    private static TMonitoredElement getMonitoredElement(FlowCnec worstCnec, Network network) {
        TMonitoredElement monitoredElement = new TMonitoredElement();

        TElement element = getElement(network, worstCnec.getNetworkElement());
        element.setName(getTextType(worstCnec.getName()));

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
    public static <T> Collector<T, ?, T> toOne() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() == 1) {
                return list.get(0);
            }
            throw new LimitingElementBuildException("Found " + list.size() + " element(s), expected exactly one.");
        });
    }
}

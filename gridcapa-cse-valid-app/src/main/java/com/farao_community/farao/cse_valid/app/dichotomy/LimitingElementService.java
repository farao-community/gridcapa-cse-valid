/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.*;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import xsd.etso_core_cmpts.TextType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class LimitingElementService {
    private final FileImporter fileImporter;
    private Crac crac;
    private Network network;
    private RaoResult raoResult;

    public LimitingElementService(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public TLimitingElement getLimitingElement(DichotomyStepResult<RaoResponse> stepResult) {
        TLimitingElement limitingElement = new TLimitingElement();
        importFiles(stepResult);
        ImmutablePair<FlowCnec, Double> worstCnec = getWorstCnecInMW();
        List<TCriticalBranch> listCriticalBranches = limitingElement.getCriticalBranch();
        TCriticalBranch criticalBranch = fillCriticalBranch(worstCnec.getLeft());
        listCriticalBranches.add(criticalBranch);
        return limitingElement;
    }

    private void importFiles(DichotomyStepResult<RaoResponse> stepResult) {
        try {
            this.crac = fileImporter.importCracFromJson(stepResult.getValidationData().getCracFileUrl());
            this.network = fileImporter.importNetwork(stepResult.getValidationData().getNetworkWithPraFileUrl());
            this.raoResult = fileImporter.importRaoResult(stepResult.getValidationData().getRaoResultFileUrl(), crac);
        } catch (IOException e) {
            throw new CseValidInvalidDataException("Could not import result files", e);
        }
    }

    private ImmutablePair<FlowCnec, Double> getWorstCnecInMW() {
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

    private TCriticalBranch fillCriticalBranch(FlowCnec worstCnec) {
        TCriticalBranch criticalBranch = new TCriticalBranch();

        criticalBranch.setOutage(getOutage(worstCnec));
        criticalBranch.setMonitoredElement(getMonitoredElement(worstCnec));

        return criticalBranch;
    }

    private TOutage getOutage(FlowCnec worstCnec) {
        TOutage outage = new TOutage();
        List<TElement> listElements = outage.getElement();
        Optional<Contingency> contingency = worstCnec.getState().getContingency();

        if (contingency.isPresent()) {
            Contingency worstCnecContingency = contingency.get();
            outage.setName(setOutageName(worstCnecContingency.getId()));
            for (NetworkElement networkElement : worstCnecContingency.getNetworkElements()) {
                TElement element = fillElement(network.getBranch(networkElement.getId()));
                listElements.add(element);
            }
        } else {
            outage.setName(setOutageName("N Situation"));
        }

        return outage;
    }

    private TextType setOutageName(String outageName) {
        TextType outageNameTextType = new TextType();
        outageNameTextType.setV(outageName);
        return outageNameTextType;
    }

    private TMonitoredElement getMonitoredElement(FlowCnec worstCnec) {
        TMonitoredElement monitoredElement = new TMonitoredElement();
        List<TElement> listElements = monitoredElement.getElement();

        listElements.add(fillElement(network.getBranch(worstCnec.getNetworkElement().getId())));

        return monitoredElement;
    }

    private TElement fillElement(Branch<?> branch) {
        TElement element = new TElement();
        String elementName;
        String id;

        if (branch instanceof TieLine) {
            TieLine tieLine = (TieLine) branch;
            elementName = tieLine.getProperty("elementName_1", "");
            id = tieLine.getHalf1().getId();
        } else {
            elementName = branch.getProperty("elementName", "");
            id = branch.getId();
        }

        element.setName(getElementName(elementName));
        element.setCode(getCode(id));
        element.setAreafrom(getAreaFrom(branch));
        element.setAreato(getAreaTo(branch));

        return element;
    }

    private TextType getElementName(String elementName) {
        TextType tElementName = new TextType();
        tElementName.setV(elementName);
        return tElementName;
    }

    private TextType getCode(String id) {
        TextType tId = new TextType();
        tId.setV(id);
        return tId;
    }

    private TArea getAreaFrom(Branch<?> branch) {
        TArea tAreaFrom = new TArea();

        String areaFrom = null;

        Optional<Substation> substation = branch.getTerminal1().getVoltageLevel().getSubstation();

        if (substation.isPresent()) {
            Optional<Country> country = substation.get().getCountry();
            if (country.isPresent()) {
                areaFrom = country.get().toString();
            }
        }

        tAreaFrom.setV(areaFrom);
        return tAreaFrom;
    }

    private TArea getAreaTo(Branch<?> branch) {
        TArea tAreaTo = new TArea();

        String areaTo = null;

        Optional<Substation> substation = branch.getTerminal2().getVoltageLevel().getSubstation();

        if (substation.isPresent()) {
            Optional<Country> country = substation.get().getCountry();
            if (country.isPresent()) {
                areaTo = country.get().toString();
            }
        }

        tAreaTo.setV(areaTo);
        return tAreaTo;
    }
}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.helper;

import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCriticalBranch;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.outage.CseOutageCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

class LimitingElementHelperTest {

    @Test
    void getLimitingElementNoOutage() {
        // given
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);

        State state = mock(State.class);
        NetworkElement networkElement = mock(NetworkElement.class);
        FlowCnec worstCnec = getWorstCnec(state, networkElement);

        Crac crac = getCrac(worstCnec);
        RaoResult raoResult = getRaoResult(worstCnec);
        when(cracCreationContext.getCrac()).thenReturn(crac);

        when(state.isPreventive()).thenReturn(true); // outage = null

        initMocksForMonitoredElement(network, networkElement);

        // when
        TLimitingElement limitingElement = LimitingElementHelper.getLimitingElement(raoResult, cracCreationContext, network);

        // then
        Assertions.assertThat(limitingElement).isNotNull();
        Assertions.assertThat(limitingElement.getCriticalBranch())
                .isNotNull()
                .hasSize(1);

        TCriticalBranch criticalBranch = limitingElement.getCriticalBranch().get(0);
        Assertions.assertThat(criticalBranch.getOutage()).isNull();
        assertMonitoredElementIsCorrect(criticalBranch);
    }

    @Test
    void getLimitingElementWithOutage() {
        // given
        Network network = mock(Network.class);
        CseCracCreationContext cracCreationContext = mock(CseCracCreationContext.class);

        State state = mock(State.class);
        NetworkElement cnecNetworkElement = mock(NetworkElement.class);
        FlowCnec worstCnec = getWorstCnec(state, cnecNetworkElement);

        Crac crac = getCrac(worstCnec);
        RaoResult raoResult = getRaoResult(worstCnec);
        when(cracCreationContext.getCrac()).thenReturn(crac);

        initMocksForOutage(network, cracCreationContext, state);

        initMocksForMonitoredElement(network, cnecNetworkElement);

        // when
        TLimitingElement limitingElement = LimitingElementHelper.getLimitingElement(raoResult, cracCreationContext, network);

        // then
        Assertions.assertThat(limitingElement).isNotNull();
        Assertions.assertThat(limitingElement.getCriticalBranch())
                .isNotNull()
                .hasSize(1);

        TCriticalBranch criticalBranch = limitingElement.getCriticalBranch().get(0);
        assertOutageIsCorrect(criticalBranch);
        assertMonitoredElementIsCorrect(criticalBranch);
    }

    @NotNull
    private static RaoResponse getValidationData() {
        RaoResponse validationData = mock(RaoResponse.class);
        when(validationData.getRaoResultFileUrl()).thenReturn("raoResultFileUrl");
        return validationData;
    }

    @NotNull
    private static FlowCnec getWorstCnec(State state, NetworkElement networkElement) {
        FlowCnec worstCnec = mock(FlowCnec.class);
        when(worstCnec.isOptimized()).thenReturn(true);
        when(worstCnec.getName()).thenReturn("name");
        when(worstCnec.getState()).thenReturn(state);
        when(worstCnec.getNetworkElement()).thenReturn(networkElement);
        return worstCnec;
    }

    @NotNull
    private static Crac getCrac(FlowCnec worstCnec) {
        Crac crac = mock(Crac.class);
        when(crac.getFlowCnecs()).thenReturn(Set.of(worstCnec));
        return crac;
    }

    @NotNull
    private static RaoResult getRaoResult(FlowCnec worstCnec) {
        RaoResult raoResult = mock(RaoResult.class);
        when(raoResult.getMargin(any(), eq(worstCnec), eq(Unit.MEGAWATT))).thenReturn(42.3);
        return raoResult;
    }

    private static void initMocksForOutage(Network network, CseCracCreationContext cracCreationContext, State state) {
        when(state.isPreventive()).thenReturn(false);
        ContingencyElement contingencyElement = mock(ContingencyElement.class);
        when(contingencyElement.getId()).thenReturn("id2");
        Contingency contingency = getContingency(contingencyElement);
        when(state.getContingency()).thenReturn(Optional.of(contingency));

        CseOutageCreationContext cseOutageCreationContext = getCseOutageCreationContext();
        when(cracCreationContext.getOutageCreationContexts()).thenReturn(List.of(cseOutageCreationContext));

        Branch<?> branch = getBranch();
        when(network.getBranch("id2")).thenReturn(branch);
    }

    @NotNull
    private static Contingency getContingency(ContingencyElement contingencyElement) {
        Contingency contingency = mock(Contingency.class);
        when(contingency.getId()).thenReturn("contingencyId");
        when(contingency.getElements()).thenReturn(List.of(contingencyElement));
        return contingency;
    }

    @NotNull
    private static CseOutageCreationContext getCseOutageCreationContext() {
        CseOutageCreationContext cseOutageCreationContext = mock(CseOutageCreationContext.class);
        when(cseOutageCreationContext.isImported()).thenReturn(true);
        when(cseOutageCreationContext.getCreatedContingencyId()).thenReturn("contingencyId");
        when(cseOutageCreationContext.getNativeId()).thenReturn("nativeId");
        return cseOutageCreationContext;
    }

    @NotNull
    private static Branch<?> getBranch() {
        Branch<?> branch = mock(Branch.class);
        Terminal terminal1 = mockTerminalWithCountry(Country.FR);
        Terminal terminal2 = mockTerminalWithCountry(Country.IT);
        when(branch.getTerminal1()).thenReturn(terminal1);
        when(branch.getTerminal2()).thenReturn(terminal2);
        return branch;
    }

    private static void initMocksForMonitoredElement(Network network, NetworkElement networkElement) {
        Branch<?> branch = getBranch();
        when(networkElement.getId()).thenReturn("id");
        when(network.getBranch("id")).thenReturn(branch);
    }

    private static Terminal mockTerminalWithCountry(Country country) {
        Terminal terminal = mock(Terminal.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        when(terminal.getVoltageLevel()).thenReturn(voltageLevel);
        Substation substation = mock(Substation.class);
        when(voltageLevel.getSubstation()).thenReturn(Optional.of(substation));
        when(substation.getCountry()).thenReturn(Optional.of(country));

        return terminal;
    }

    private static void assertOutageIsCorrect(TCriticalBranch criticalBranch) {
        Assertions.assertThat(criticalBranch.getOutage()).isNotNull();
        Assertions.assertThat(criticalBranch.getOutage().getName()).isNotNull();
        Assertions.assertThat(criticalBranch.getOutage().getName().getV()).isEqualTo("nativeId");
        Assertions.assertThat(criticalBranch.getOutage().getElement())
                .isNotNull()
                .hasSize(1);

        TElement outageElement = criticalBranch.getOutage().getElement().get(0);
        Assertions.assertThat(outageElement.getCode()).isNotNull();
        Assertions.assertThat(outageElement.getCode().getV()).isEqualTo("id2");
        Assertions.assertThat(outageElement.getAreafrom()).isNotNull();
        Assertions.assertThat(outageElement.getAreafrom().getV()).isEqualTo("FR");
        Assertions.assertThat(outageElement.getAreato()).isNotNull();
        Assertions.assertThat(outageElement.getAreato().getV()).isEqualTo("IT");
    }

    private static void assertMonitoredElementIsCorrect(TCriticalBranch criticalBranch) {
        Assertions.assertThat(criticalBranch.getMonitoredElement()).isNotNull();
        Assertions.assertThat(criticalBranch.getMonitoredElement().getElement())
                .isNotNull()
                .hasSize(1);

        TElement monitoredElement = criticalBranch.getMonitoredElement().getElement().get(0);
        Assertions.assertThat(monitoredElement).isNotNull();
        Assertions.assertThat(monitoredElement.getCode()).isNotNull();
        Assertions.assertThat(monitoredElement.getCode().getV()).isEqualTo("id");
        Assertions.assertThat(monitoredElement.getAreafrom()).isNotNull();
        Assertions.assertThat(monitoredElement.getAreafrom().getV()).isEqualTo("FR");
        Assertions.assertThat(monitoredElement.getAreato()).isNotNull();
        Assertions.assertThat(monitoredElement.getAreato().getV()).isEqualTo("IT");
        Assertions.assertThat(monitoredElement.getName()).isNotNull();
        Assertions.assertThat(monitoredElement.getName().getV()).isEqualTo("name");
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.dichotomy;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.CseValidNetworkShifterProvider;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.TTimestampWrapper;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.helper.NetPositionHelper;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.utils.CseValidRequestTestData;
import com.farao_community.farao.cse_valid.app.utils.TimestampTestData;
import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class DichotomyRunnerTest {

    @MockitoBean
    private FileImporter fileImporter;

    @MockitoBean
    private FileExporter fileExporter;

    @MockitoBean
    private RaoRunnerClient raoRunnerClient;

    @MockitoBean
    private Logger businessLogger;

    @MockitoBean
    private CseValidNetworkShifterProvider cseValidNetworkShifterProvider;

    @Autowired
    private EicCodesConfiguration eicCodesConfiguration;

    @MockitoBean
    private EicCodesMapper eicCodesMapper;

    @MockitoSpyBean
    private DichotomyRunner dichotomyRunner;

    /* --------------- runDichotomy --------------- */

    @Test
    void runDichotomyForFullImport() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getImportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();

        TTimestamp timestamp = TimestampTestData.getTimestampWithMniiAndMibniiAndAntcfinalAndActualNtcBelowTarget();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        double minValue = 0.0;
        double maxValue = timestampWrapper.getMniiIntValue() - (timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue());

        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        NetworkValidator<RaoSuccessResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyEngine<RaoSuccessResponse> engine = mock(DichotomyEngine.class);

        when(cseValidNetworkShifterProvider.getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        doReturn(networkValidator).when(dichotomyRunner).getNetworkValidator(cseValidRequest, jsonCracUrl, raoParameterUrl);
        doReturn(engine).when(dichotomyRunner).getDichotomyEngine(minValue, maxValue, networkShifter, networkValidator);

        dichotomyRunner.runDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl, network, false);

        verify(cseValidNetworkShifterProvider, times(1)).getNetworkShifterForFullImport(timestampWrapper, network, glskUrl, processType);
        verify(dichotomyRunner, times(1)).getNetworkValidator(cseValidRequest, jsonCracUrl, raoParameterUrl);
        verify(dichotomyRunner, times(1)).getDichotomyEngine(minValue, maxValue, networkShifter, networkValidator);
        verify(engine, times(1)).run(network);
    }

    @Test
    void runDichotomyForExportCornerWithFranceInArea() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();

        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceInArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        double franceImportBeforeShifting = 400.0;
        double franceImportAfterShifting = 1000.0;
        double minValue = franceImportBeforeShifting - franceImportAfterShifting;
        double maxValue = 0.0;

        Network initialNetwork = mock(Network.class);
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        NetworkValidator<RaoSuccessResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyEngine<RaoSuccessResponse> engine = mock(DichotomyEngine.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(initialNetwork);
        when(cseValidNetworkShifterProvider.getNetworkShifterForExportCornerWithItalyFrance(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        doReturn(networkValidator).when(dichotomyRunner).getNetworkValidator(cseValidRequest, jsonCracUrl, raoParameterUrl);
        doReturn(engine).when(dichotomyRunner).getDichotomyEngine(minValue, maxValue, networkShifter, networkValidator);
        try (MockedStatic<NetPositionHelper> netPositionHelperMockedStatic = Mockito.mockStatic(NetPositionHelper.class)) {
            netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeFranceImportFromItaly(initialNetwork))
                    .thenReturn(franceImportBeforeShifting);
            netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeFranceImportFromItaly(network))
                    .thenReturn(franceImportAfterShifting);
            dichotomyRunner.runDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl, network, true);
        }

        verify(cseValidNetworkShifterProvider, times(1)).getNetworkShifterForExportCornerWithItalyFrance(timestampWrapper, network, glskUrl, processType);
        verify(dichotomyRunner, times(1)).getNetworkValidator(cseValidRequest, jsonCracUrl, raoParameterUrl);
        verify(dichotomyRunner, times(1)).getDichotomyEngine(minValue, maxValue, networkShifter, networkValidator);
        verify(engine, times(1)).run(network);
    }

    @Test
    void runDichotomyForExportCornerWithFranceOutArea() {
        CseValidRequest cseValidRequest = CseValidRequestTestData.getExportCseValidRequest(ProcessType.IDCC);
        String glskUrl = cseValidRequest.getGlsk().getUrl();
        String cgmUrl = cseValidRequest.getCgm().getUrl();
        ProcessType processType = cseValidRequest.getProcessType();

        TTimestamp timestamp = TimestampTestData.getTimestampWithFranceOutArea();
        TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestamp, eicCodesConfiguration, eicCodesMapper);

        String jsonCracUrl = "/CSE/VALID/crac.utc";
        String raoParameterUrl = "/CSE/VALID/raoParameter.utc";
        double franceImportBeforeShifting = -400.0;
        double franceImportAfterShifting = -1000.0;
        double minValue = franceImportAfterShifting - franceImportBeforeShifting;
        double maxValue = 0.0;

        Network initialNetwork = mock(Network.class);
        Network network = mock(Network.class);
        NetworkShifter networkShifter = mock(NetworkShifter.class);
        NetworkValidator<RaoSuccessResponse> networkValidator = mock(NetworkValidator.class);
        DichotomyEngine<RaoSuccessResponse> engine = mock(DichotomyEngine.class);

        when(fileImporter.importNetwork(cgmUrl)).thenReturn(initialNetwork);
        when(cseValidNetworkShifterProvider.getNetworkShifterForExportCornerWithItalyFrance(timestampWrapper, network, glskUrl, processType)).thenReturn(networkShifter);
        doReturn(networkValidator).when(dichotomyRunner).getNetworkValidator(cseValidRequest, jsonCracUrl, raoParameterUrl);
        doReturn(engine).when(dichotomyRunner).getDichotomyEngine(minValue, maxValue, networkShifter, networkValidator);
        try (MockedStatic<NetPositionHelper> netPositionHelperMockedStatic = Mockito.mockStatic(NetPositionHelper.class)) {
            netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeFranceImportFromItaly(initialNetwork))
                    .thenReturn(franceImportBeforeShifting);
            netPositionHelperMockedStatic.when(() -> NetPositionHelper.computeFranceImportFromItaly(network))
                    .thenReturn(franceImportAfterShifting);
            dichotomyRunner.runDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParameterUrl, network, true);
        }

        verify(cseValidNetworkShifterProvider, times(1)).getNetworkShifterForExportCornerWithItalyFrance(timestampWrapper, network, glskUrl, processType);
        verify(dichotomyRunner, times(1)).getNetworkValidator(cseValidRequest, jsonCracUrl, raoParameterUrl);
        verify(dichotomyRunner, times(1)).getDichotomyEngine(minValue, maxValue, networkShifter, networkValidator);
        verify(engine, times(1)).run(network);
    }
}

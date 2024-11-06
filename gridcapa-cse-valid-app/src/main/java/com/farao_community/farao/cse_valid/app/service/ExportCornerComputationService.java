/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app.service;

import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.CseValidNetworkShifterProvider;
import com.farao_community.farao.cse_valid.app.FileExporter;
import com.farao_community.farao.cse_valid.app.FileImporter;
import com.farao_community.farao.cse_valid.app.TTimestampWrapper;
import com.farao_community.farao.cse_valid.app.TcDocumentTypeWriter;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
import com.farao_community.farao.cse_valid.app.helper.LimitingElementHelper;
import com.farao_community.farao.cse_valid.app.helper.NetPositionHelper;
import com.farao_community.farao.cse_valid.app.rao.CseValidRaoRunner;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.validator.CseValidRequestValidator;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_CALCULATION_DIRECTIONS;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_SHIFTING_FACTORS;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */

@Service
public class ExportCornerComputationService {

    private final ComputationService computationService;
    private final DichotomyRunner dichotomyRunner;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final Logger businessLogger;
    private final CseValidNetworkShifterProvider cseValidNetworkShifterProvider;
    private final CseValidRaoRunner cseValidRaoRunner;

    public ExportCornerComputationService(ComputationService computationService,
                                          DichotomyRunner dichotomyRunner,
                                          FileImporter fileImporter,
                                          FileExporter fileExporter,
                                          Logger businessLogger,
                                          CseValidNetworkShifterProvider cseValidNetworkShifterProvider,
                                          CseValidRaoRunner cseValidRaoRunner) {
        this.computationService = computationService;
        this.dichotomyRunner = dichotomyRunner;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.businessLogger = businessLogger;
        this.cseValidNetworkShifterProvider = cseValidNetworkShifterProvider;
        this.cseValidRaoRunner = cseValidRaoRunner;
    }

    public void computeTimestamp(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        TTimestamp timestamp = timestampWrapper.getTimestamp();
        if (irrelevantValuesInTimestamp(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestamp, timestampWrapper.getMiecValue());
        } else if (missingDataInTimestamp(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampError(timestamp, ERROR_MSG_MISSING_DATA);
        } else if (!timestampWrapper.hasShiftingFactors()) {
            tcDocumentTypeWriter.fillTimestampError(timestamp, ERROR_MSG_MISSING_SHIFTING_FACTORS);
        } else if (!timestampWrapper.hasCalculationDirections()) {
            tcDocumentTypeWriter.fillTimestampError(timestamp, ERROR_MSG_MISSING_CALCULATION_DIRECTIONS);
        } else if (actualNtcAboveTarget(timestampWrapper)) {
            BigDecimal miecValue = timestampWrapper.getMibiecValue().subtract(timestampWrapper.getAntcfinalValue());
            tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestamp, miecValue);
        } else {
            try {
                CseValidRequestValidator.checkAllFilesExist(cseValidRequest, timestampWrapper.isFranceImportingFromItaly());

                String cgmUrl = cseValidRequest.getCgm().getUrl();
                Network network = fileImporter.importNetwork(cgmUrl);
                String glskUrl = cseValidRequest.getGlsk().getUrl();
                ProcessType processType = cseValidRequest.getProcessType();
                NetworkShifter networkShifter = cseValidNetworkShifterProvider.getNetworkShifterForExportCornerWithAllCountries(timestampWrapper, network, glskUrl, processType);
                double shiftValue = computeShiftValue(timestampWrapper);

                computationService.shiftNetwork(shiftValue, network, networkShifter);

                OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();
                String raoParametersURL = fileExporter.saveRaoParameters(processTargetDateTime, processType);

                String cracUrl = timestampWrapper.isFranceImportingFromItaly()
                        ? cseValidRequest.getExportCrac().getUrl()
                        : cseValidRequest.getImportCrac().getUrl();
                CseCracCreationContext cracCreationContext = fileImporter.importCracCreationContext(cracUrl, network);
                String jsonCracUrl = fileExporter.saveCracInJsonFormat(cracCreationContext.getCrac(), cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());

                AbstractRaoResponse raoResponse = computationService.runRao(cseValidRequest, network, jsonCracUrl, raoParametersURL);

                if (cseValidRaoRunner.isSecure(raoResponse, network)) {
                    tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestamp, timestampWrapper.getMiecValue());
                } else {
                    runDichotomy(timestampWrapper, cseValidRequest, tcDocumentTypeWriter, jsonCracUrl, raoParametersURL, network, cracCreationContext);
                }
            } catch (CseValidRequestValidatorException e) {
                businessLogger.error("Missing some input files for timestamp '{}'", timestampWrapper.getTimeValue());
                tcDocumentTypeWriter.fillTimestampError(timestamp, e.getMessage());
            }
        }
    }

    private static boolean irrelevantValuesInTimestamp(TTimestampWrapper timestampWrapper) {
        // MIEC is present but both values MiBIEC and ANTCFinal are absent or both values are equal to zero
        final boolean mibiecAndAntcfinalAbsent = timestampWrapper.getMibiec() == null && timestampWrapper.getAntcfinal() == null;
        final boolean mibiecIsZero = timestampWrapper.hasMibiec() && timestampWrapper.getMibiecIntValue() == 0;
        final boolean antcfinalIsZero = timestampWrapper.hasAntcfinal() && timestampWrapper.getAntcfinalIntValue() == 0;

        return mibiecAndAntcfinalAbsent || mibiecIsZero && antcfinalIsZero;
    }

    private static boolean missingDataInTimestamp(TTimestampWrapper timestampWrapper) {
        // MIEC is present but one of the required data (MiBIEC or ANTCFinal) is missing
        return !timestampWrapper.hasMibiec() || !timestampWrapper.hasAntcfinal();
    }

    private boolean actualNtcAboveTarget(TTimestampWrapper timestampWrapper) {
        final int actualNtc = timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue();
        final int targetNtc = timestampWrapper.getMiecIntValue();
        if (actualNtc >= targetNtc) {
            businessLogger.info("Timestamp '{}' NTC has not been augmented by adjustment process, no computation needed.", timestampWrapper.getTimestamp().getTime().getV());
            return true;
        }
        businessLogger.info("Timestamp '{}' augmented NTC must be validated.", timestampWrapper.getTimestamp().getTime().getV());
        return false;
    }

    private static double computeShiftValue(TTimestampWrapper timestampWrapper) {
        return (double) timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());
    }

    private void runDichotomy(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter, String jsonCracUrl, String raoParametersURL, Network network, CseCracCreationContext cracCreationContext) {
        DichotomyResult<RaoSuccessResponse> dichotomyResult = dichotomyRunner.runDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersURL, network, true);
        if (dichotomyResult != null && dichotomyResult.hasValidStep()) {
            String raoResultFileUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResultFileUrl();
            RaoResult raoResult = fileImporter.importRaoResult(raoResultFileUrl, cracCreationContext.getCrac());
            TLimitingElement tLimitingElement = LimitingElementHelper.getLimitingElement(raoResult, cracCreationContext, network);
            double value = computeExportCornerValue(timestampWrapper, dichotomyResult);
            tcDocumentTypeWriter.fillTimestampWithExportCornerDichotomyResponse(timestampWrapper.getTimestamp(), tLimitingElement, value, timestampWrapper.isFranceImportingFromItaly());
        } else {
            tcDocumentTypeWriter.fillDichotomyError(timestampWrapper.getTimestamp());
        }
    }

    private double computeExportCornerValue(TTimestampWrapper timestampWrapper, DichotomyResult<RaoSuccessResponse> dichotomyResult) {
        String finalNetworkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getNetworkWithPraFileUrl();
        Network network = fileImporter.importNetwork(finalNetworkWithPraUrl);
        double value = NetPositionHelper.computeFranceImportFromItaly(network);
        return timestampWrapper.isFranceImportingFromItaly() ? value : value * -1;
    }
}

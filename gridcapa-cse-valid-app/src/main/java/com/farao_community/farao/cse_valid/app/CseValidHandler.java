/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.dichotomy.LimitingElementService;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionReport;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TCalculationDirection;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.cse_valid.app.validator.CseValidRequestValidator;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import xsd.etso_core_cmpts.AreaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_CALCULATION_DIRECTIONS;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_SHIFTING_FACTORS;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class CseValidHandler {
    private final DichotomyRunner dichotomyRunner;
    private final EicCodesConfiguration eicCodesConfiguration;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final NetPositionService netPositionService;
    private final LimitingElementService limitingElementService;
    private final Logger businessLogger;
    private final CseValidRequestValidator cseValidRequestValidator;
    private final CseValidNetworkShifter cseValidNetworkShifter;

    public CseValidHandler(DichotomyRunner dichotomyRunner,
                           EicCodesConfiguration eicCodesConfiguration,
                           FileImporter fileImporter,
                           FileExporter fileExporter,
                           NetPositionService netPositionService,
                           LimitingElementService limitingElementService,
                           Logger businessLogger,
                           CseValidRequestValidator cseValidRequestValidator,
                           CseValidNetworkShifter cseValidNetworkShifter) {
        this.dichotomyRunner = dichotomyRunner;
        this.eicCodesConfiguration = eicCodesConfiguration;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.netPositionService = netPositionService;
        this.limitingElementService = limitingElementService;
        this.businessLogger = businessLogger;
        this.cseValidRequestValidator = cseValidRequestValidator;
        this.cseValidNetworkShifter = cseValidNetworkShifter;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        Instant computationStartInstant = Instant.now();
        TcDocumentType tcDocumentType = importTtcAdjustmentFile(cseValidRequest.getTtcAdjustment());
        TcDocumentTypeWriter tcDocumentTypeWriter = new TcDocumentTypeWriter(cseValidRequest);
        if (tcDocumentType != null) {
            TTimestamp timestampData = getTimestampData(cseValidRequest, tcDocumentType);
            if (timestampData != null) {
                TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestampData);
                computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
            } else {
                String ttcAdjTimestamp = formatTimestamp(cseValidRequest.getTime());
                String refCalcTimestamp = formatTimestamp(cseValidRequest.getTimestamp());
                businessLogger.warn("No timestamp available in ttc adjustment for time {} and reference calculation time {}", ttcAdjTimestamp, refCalcTimestamp);
                tcDocumentTypeWriter.fillNoTtcAdjustmentError(cseValidRequest);
            }
        } else {
            tcDocumentTypeWriter.fillNoTtcAdjustmentError(cseValidRequest);
        }
        String ttcValidationUrl = fileExporter.saveTtcValidation(tcDocumentTypeWriter, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
        Instant computationEndInstant = Instant.now();
        return new CseValidResponse(cseValidRequest.getId(), ttcValidationUrl, computationStartInstant, computationEndInstant);
    }

    private TcDocumentType importTtcAdjustmentFile(CseValidFileResource ttcAdjustmentFile) {
        return ttcAdjustmentFile != null ? fileImporter.importTtcAdjustment(ttcAdjustmentFile.getUrl()) : null;
    }

    private TTimestamp getTimestampData(CseValidRequest cseValidRequest, TcDocumentType tcDocumentType) {
        if (tcDocumentType.getAdjustmentResults().get(0) != null) {
            return tcDocumentType.getAdjustmentResults().get(0).getTimestamp().stream()
                    .filter(t -> t.getReferenceCalculationTime().getV().equals(formatTimestamp(cseValidRequest.getTimestamp()))
                            && t.getTime().getV().equals(formatTimestamp(cseValidRequest.getTime())))
                    .findFirst()
                    .orElse(null);
        } else {
            return null;
        }
    }

    private String formatTimestamp(OffsetDateTime offsetDateTime) {
        // format to "yyy-MM-ddThh:mmZ"
        return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).toString();
    }

    private boolean actualNtcAboveTargetNtc(TTimestampWrapper timestampWrapper, int actualNtc, int targetNtc) {
        if (actualNtc >= targetNtc) {
            businessLogger.info("Timestamp '{}' NTC has not been augmented by adjustment process, no computation needed.", timestampWrapper.getTimestamp().getTime().getV());
            return true;
        }
        businessLogger.info("Timestamp '{}' augmented NTC must be validated.", timestampWrapper.getTimestamp().getTime().getV());
        return false;
    }

    void computeTimestamp(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        if (timestampWrapper.hasNoneOfMniiMnieMiec()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_DATA);
        } else if (timestampWrapper.hasMultipleMniiMnieMiec()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_CONTRADICTORY_DATA);
        } else if (timestampWrapper.hasMnii()) {
            computeTimestampForFullImport(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        } else if (timestampWrapper.hasMiec()) {
            computeTimestampForExportCorner(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        } else if (timestampWrapper.hasMnie()) {
            tcDocumentTypeWriter.fillTimestampFullExportSuccess(timestampWrapper.getTimestamp(), timestampWrapper.getMnieValue());
        } else {
            throw new CseValidInvalidDataException(String.format("Unhandled data for timestamp %s and reference calculation time %s", timestampWrapper.getTimeValue(), timestampWrapper.getReferenceCalculationTimeValue()));
        }
    }

    /* --------------- IMPORT CORNER --------------- */

    private void computeTimestampForFullImport(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        if (irrelevantValuesInTimestampForFullImport(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampFullImportSuccess(timestampWrapper.getTimestamp(), timestampWrapper.getMniiValue());
        } else if (missingDataInTimestampForFullImport(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_DATA);
        } else if (actualNtcAboveTargetForFullImport(timestampWrapper)) {
            BigDecimal mniiValue = timestampWrapper.getMibniiValue().subtract(timestampWrapper.getAntcfinalValue());
            tcDocumentTypeWriter.fillTimestampFullImportSuccess(timestampWrapper.getTimestamp(), mniiValue);
        } else {
            try {
                cseValidRequestValidator.checkAllFilesExist(cseValidRequest, null);
                runDichotomyForFullImport(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
            } catch (CseValidRequestValidatorException e) {
                businessLogger.error("Missing some input files for timestamp '{}'", timestampWrapper.getTimeValue());
                tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), e.getMessage());
            }
        }
    }

    private static boolean irrelevantValuesInTimestampForFullImport(TTimestampWrapper timestampWrapper) {
        // MNII is present but both values MiBNII and ANTCFinal are absent or both values are equal to zero
        final boolean mibniiAndAntcfinalAbsent = timestampWrapper.getMibnii() == null && timestampWrapper.getAntcfinal() == null;
        final boolean mibniiIsZero = timestampWrapper.hasMibnii() && timestampWrapper.getMibniiIntValue() == 0;
        final boolean antcfinalIsZero = timestampWrapper.hasAntcfinal() && timestampWrapper.getAntcfinalIntValue() == 0;

        return mibniiAndAntcfinalAbsent || (mibniiIsZero && antcfinalIsZero);
    }

    private static boolean missingDataInTimestampForFullImport(TTimestampWrapper timestampWrapper) {
        // MNII is present but one of the required data (MiBNII or ANTCFinal) is missing
        return !timestampWrapper.hasMibnii() || !timestampWrapper.hasAntcfinal();
    }

    private boolean actualNtcAboveTargetForFullImport(TTimestampWrapper timestampWrapper) {
        final int actualNtc = timestampWrapper.getMibniiIntValue() - timestampWrapper.getAntcfinalIntValue();
        final int targetNtc = timestampWrapper.getMniiIntValue();
        return actualNtcAboveTargetNtc(timestampWrapper, actualNtc, targetNtc);
    }

    private void runDichotomyForFullImport(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runImportCornerDichotomy(cseValidRequest, timestampWrapper.getTimestamp());
        if (dichotomyResult != null && dichotomyResult.hasValidStep()) {
            TLimitingElement tLimitingElement = this.limitingElementService.getLimitingElement(dichotomyResult.getHighestValidStep());
            BigDecimal mibniiValue = timestampWrapper.getMibniiValue().subtract(timestampWrapper.getAntcfinalValue());
            BigDecimal mniiValue = computeMnii(dichotomyResult).map(Math::round).map(BigDecimal::valueOf).orElse(mibniiValue);
            tcDocumentTypeWriter.fillTimestampWithDichotomyResponse(timestampWrapper.getTimestamp(), mibniiValue, mniiValue, tLimitingElement);
        } else {
            tcDocumentTypeWriter.fillDichotomyError(timestampWrapper.getTimestamp());
        }
    }

    private Optional<Double> computeMnii(DichotomyResult<RaoResponse> dichotomyResult) {
        if (dichotomyResult.getHighestValidStep() == null) {
            return Optional.empty();
        }
        String finalNetworkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getNetworkWithPraFileUrl();
        NetPositionReport netPositionReport = netPositionService.generateNetPositionReport(finalNetworkWithPraUrl);
        Map<String, Double> italianBordersExchange = netPositionReport.getAreasReport().get("IT").getBordersExchanges();
        double italianCseNetPosition = italianBordersExchange.get("FR") +
                italianBordersExchange.get("CH") +
                italianBordersExchange.get("AT") +
                italianBordersExchange.get("SI");
        return Optional.of(-italianCseNetPosition);
    }

    /* --------------- EXPORT CORNER --------------- */

    private void computeTimestampForExportCorner(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        TTimestamp timestamp = timestampWrapper.getTimestamp();
        if (irrelevantValuesInTimestampForExportCorner(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestamp, timestampWrapper.getMiecValue());
        } else if (missingDataInTimestampForExportCorner(timestampWrapper)) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_DATA);
        } else if (!timestampWrapper.hasShiftingFactors()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_SHIFTING_FACTORS);
        } else if (!timestampWrapper.hasCalculationDirections()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_CALCULATION_DIRECTIONS);
        } else if (actualNtcAboveTargetForExportCorner(timestampWrapper)) {
            BigDecimal miecValue = timestampWrapper.getMibiecValue().subtract(timestampWrapper.getAntcfinalValue());
            tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestampWrapper.getTimestamp(), miecValue);
        } else {
            if (areAllRequiredFilesPresent(timestampWrapper, cseValidRequest, tcDocumentTypeWriter)) {
                Network network = cseValidNetworkShifter.getNetworkShiftedWithShiftingFactors(timestamp, cseValidRequest);

                runDichotomyForExportCorner(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
            }
        }
    }

    private static boolean irrelevantValuesInTimestampForExportCorner(TTimestampWrapper timestampWrapper) {
        // MIEC is present but both values MiBIEC and ANTCFinal are absent or both values are equal to zero
        final boolean mibiecAndAntcfinalAbsent = timestampWrapper.getMibiec() == null && timestampWrapper.getAntcfinal() == null;
        final boolean mibiecIsZero = timestampWrapper.hasMibiec() && timestampWrapper.getMibiecIntValue() == 0;
        final boolean antcfinalIsZero = timestampWrapper.hasAntcfinal() && timestampWrapper.getAntcfinalIntValue() == 0;

        return mibiecAndAntcfinalAbsent || (mibiecIsZero && antcfinalIsZero);
    }

    private static boolean missingDataInTimestampForExportCorner(TTimestampWrapper timestampWrapper) {
        // MIEC is present but one of the required data (MiBIEC or ANTCFinal) is missing
        return !timestampWrapper.hasMibiec() || !timestampWrapper.hasAntcfinal();
    }

    private boolean actualNtcAboveTargetForExportCorner(TTimestampWrapper timestampWrapper) {
        final int actualNtc = timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue();
        final int targetNtc = timestampWrapper.getMiecIntValue();
        return actualNtcAboveTargetNtc(timestampWrapper, actualNtc, targetNtc);
    }

    private boolean areAllRequiredFilesPresent(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        TTimestamp timestamp = timestampWrapper.getTimestamp();
        String franceEic = eicCodesConfiguration.getFrance();
        List<TCalculationDirection> calculationDirections = timestamp.getCalculationDirections().get(0).getCalculationDirection();
        try {
            if (isCountryInArea(franceEic, calculationDirections)) {
                cseValidRequestValidator.checkAllFilesExist(cseValidRequest, true);
            } else if (isCountryOutArea(franceEic, calculationDirections)) {
                cseValidRequestValidator.checkAllFilesExist(cseValidRequest, false);
            } else {
                throw new CseValidInvalidDataException("France must appear in InArea or OutArea");
            }
        } catch (CseValidRequestValidatorException e) {
            businessLogger.error("Missing some input files for timestamp '{}'", timestampWrapper.getTimeValue());
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean isCountryInArea(String countryEic, List<TCalculationDirection> calculationDirections) {
        Optional<AreaType> countryInArea = calculationDirections.stream()
                .map(TCalculationDirection::getInArea)
                .filter(areaType -> areaType.getV().equals(countryEic))
                .findFirst();
        return countryInArea.isPresent();
    }

    public static boolean isCountryOutArea(String countryEic, List<TCalculationDirection> calculationDirections) {
        Optional<AreaType> countryOutArea = calculationDirections.stream()
                .map(TCalculationDirection::getOutArea)
                .filter(areaType -> areaType.getV().equals(countryEic))
                .findFirst();
        return countryOutArea.isPresent();
    }

    private void runDichotomyForExportCorner(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        TTimestamp timestamp = timestampWrapper.getTimestamp();
        String franceEic = eicCodesConfiguration.getFrance();
        List<TCalculationDirection> calculationDirections = timestamp.getCalculationDirections().get(0).getCalculationDirection();
        boolean isExportCornerActive = isCountryInArea(franceEic, calculationDirections);
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runExportCornerDichotomy(cseValidRequest, timestampWrapper.getTimestamp(), isExportCornerActive);
        // TODO
    }
}

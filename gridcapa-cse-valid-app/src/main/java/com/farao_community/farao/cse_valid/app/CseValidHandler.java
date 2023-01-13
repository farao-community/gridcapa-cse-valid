/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInvalidDataException;
import com.farao_community.farao.cse_valid.api.resource.CseValidFileResource;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.api.resource.CseValidResponse;
import com.farao_community.farao.cse_valid.api.resource.ProcessType;
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.cse_valid.app.dichotomy.LimitingElementService;
import com.farao_community.farao.cse_valid.app.exception.CseValidRequestValidatorException;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionReport;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
import com.farao_community.farao.cse_valid.app.rao.CseValidRaoValidator;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import com.farao_community.farao.cse_valid.app.validator.CseValidRequestValidator;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cse.CseCrac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_CALCULATION_DIRECTIONS;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_SHIFTING_FACTORS;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class CseValidHandler {
    private final DichotomyRunner dichotomyRunner;
    private final EicCodesConfiguration eicCodesConfiguration;
    private final EicCodesMapper eicCodesMapper;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final NetPositionService netPositionService;
    private final LimitingElementService limitingElementService;
    private final Logger businessLogger;
    private final CseValidRequestValidator cseValidRequestValidator;
    private final CseValidNetworkShifter cseValidNetworkShifter;
    private final CseValidRaoValidator cseValidRaoValidator;

    public CseValidHandler(DichotomyRunner dichotomyRunner,
                           EicCodesConfiguration eicCodesConfiguration,
                           EicCodesMapper eicCodesMapper,
                           FileImporter fileImporter,
                           FileExporter fileExporter,
                           NetPositionService netPositionService,
                           LimitingElementService limitingElementService,
                           Logger businessLogger,
                           CseValidRequestValidator cseValidRequestValidator,
                           CseValidNetworkShifter cseValidNetworkShifter,
                           CseValidRaoValidator cseValidRaoValidator) {
        this.dichotomyRunner = dichotomyRunner;
        this.eicCodesConfiguration = eicCodesConfiguration;
        this.eicCodesMapper = eicCodesMapper;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.netPositionService = netPositionService;
        this.limitingElementService = limitingElementService;
        this.businessLogger = businessLogger;
        this.cseValidRequestValidator = cseValidRequestValidator;
        this.cseValidNetworkShifter = cseValidNetworkShifter;
        this.cseValidRaoValidator = cseValidRaoValidator;
    }

    public CseValidResponse handleCseValidRequest(CseValidRequest cseValidRequest) {
        Instant computationStartInstant = Instant.now();
        TcDocumentType tcDocumentType = importTtcAdjustmentFile(cseValidRequest.getTtcAdjustment());
        TcDocumentTypeWriter tcDocumentTypeWriter = new TcDocumentTypeWriter(cseValidRequest);
        if (tcDocumentType != null) {
            TTimestamp timestampData = getTimestampData(cseValidRequest, tcDocumentType);
            if (timestampData != null) {
                TTimestampWrapper timestampWrapper = new TTimestampWrapper(timestampData, eicCodesConfiguration, eicCodesMapper);
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

    private String getJsonCracUrl(CseValidRequest cseValidRequest, Network network, String cracUrl) {
        CseCrac cseCrac = fileImporter.importCseCrac(cracUrl);
        Crac crac = fileImporter.importCrac(cseCrac, cseValidRequest.getTimestamp(), network);
        return fileExporter.saveCracInJsonFormat(crac, cseValidRequest.getTimestamp(), cseValidRequest.getProcessType());
    }

    private String generateScaledNetworkDirPath(Network network, OffsetDateTime processTargetDateTime, ProcessType processType) {
        String basePath = fileExporter.makeDestinationMinioPath(processTargetDateTime, processType, FileExporter.FileKind.ARTIFACTS);
        String variantName = network.getVariantManager().getWorkingVariantId();
        return basePath + variantName;
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

    /* --------------- FULL IMPORT --------------- */

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
                cseValidRequestValidator.checkAllFilesExist(cseValidRequest, false);

                String cgmUrl = cseValidRequest.getCgm().getUrl();
                Network network = fileImporter.importNetwork(cgmUrl);

                String cracUrl = cseValidRequest.getImportCrac().getUrl();
                String jsonCracUrl = getJsonCracUrl(cseValidRequest, network, cracUrl);

                ProcessType processType = cseValidRequest.getProcessType();
                OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();
                String raoParametersURL = fileExporter.saveRaoParameters(processTargetDateTime, processType);

                runDichotomyForFullImport(timestampWrapper, cseValidRequest, tcDocumentTypeWriter, jsonCracUrl, raoParametersURL);
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

    private void runDichotomyForFullImport(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter, String jsonCracUrl, String raoParametersURL) {
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runImportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersURL);
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
            try {
                cseValidRequestValidator.checkAllFilesExist(cseValidRequest, timestampWrapper.isFranceImportingFromItaly());
                String cgmUrl = cseValidRequest.getCgm().getUrl();
                Network network = fileImporter.importNetwork(cgmUrl);
                double shiftValue = computeShiftValue(timestampWrapper);
                String glskUrl = cseValidRequest.getGlsk().getUrl();
                cseValidNetworkShifter.shiftNetwork(shiftValue, network, timestampWrapper, glskUrl);

                ProcessType processType = cseValidRequest.getProcessType();
                OffsetDateTime processTargetDateTime = cseValidRequest.getTimestamp();
                String raoParametersURL = fileExporter.saveRaoParameters(processTargetDateTime, processType);

                String cracUrl = timestampWrapper.isFranceImportingFromItaly()
                        ? cseValidRequest.getExportCrac().getUrl()
                        : cseValidRequest.getImportCrac().getUrl();
                String jsonCracUrl = getJsonCracUrl(cseValidRequest, network, cracUrl);

                String scaledNetworkDirPath = generateScaledNetworkDirPath(network, processTargetDateTime, processType);
                String scaledNetworkName = network.getNameOrId() + ".xiidm";
                String networkFilePath = scaledNetworkDirPath + scaledNetworkName;
                String networkFiledUrl = fileExporter.saveNetworkInArtifact(network, networkFilePath, "", processTargetDateTime, processType);
                String resultsDestination = "CSE/VALID/" + scaledNetworkDirPath;

                RaoResponse raoResponse = cseValidRaoValidator.runRao(cseValidRequest, networkFiledUrl, jsonCracUrl, raoParametersURL, resultsDestination);

                if (cseValidRaoValidator.isSecure(raoResponse)) {
                    tcDocumentTypeWriter.fillTimestampExportCornerSuccess(timestamp, timestampWrapper.getMiecValue());
                } else {
                    runDichotomyForExportCorner(timestampWrapper, cseValidRequest, tcDocumentTypeWriter, jsonCracUrl, raoParametersURL);
                }
            } catch (CseValidRequestValidatorException e) {
                businessLogger.error("Missing some input files for timestamp '{}'", timestampWrapper.getTimeValue());
                tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), e.getMessage());
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

    private double computeShiftValue(TTimestampWrapper timestampWrapper) {
        return (double) timestampWrapper.getMiecIntValue() - (timestampWrapper.getMibiecIntValue() - timestampWrapper.getAntcfinalIntValue());
    }

    private void runDichotomyForExportCorner(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter, String jsonCracUrl, String raoParametersURL) {
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.runExportCornerDichotomy(timestampWrapper, cseValidRequest, jsonCracUrl, raoParametersURL);
        // TODO
    }
}

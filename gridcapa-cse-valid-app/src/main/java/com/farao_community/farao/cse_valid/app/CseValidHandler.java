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
import com.farao_community.farao.cse_valid.app.configuration.EicCodesConfiguration;
import com.farao_community.farao.cse_valid.app.mapper.EicCodesMapper;
import com.farao_community.farao.cse_valid.app.service.ExportCornerComputationService;
import com.farao_community.farao.cse_valid.app.service.FullImportComputationService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_CONTRADICTORY_DATA;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_DATA;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class CseValidHandler {
    private final EicCodesConfiguration eicCodesConfiguration;
    private final EicCodesMapper eicCodesMapper;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final Logger businessLogger;
    private final FullImportComputationService fullImportComputationService;
    private final ExportCornerComputationService exportCornerComputationService;

    public CseValidHandler(EicCodesConfiguration eicCodesConfiguration,
                           EicCodesMapper eicCodesMapper,
                           FileImporter fileImporter,
                           FileExporter fileExporter,
                           Logger businessLogger,
                           FullImportComputationService fullImportComputationService,
                           ExportCornerComputationService exportCornerComputationService) {
        this.eicCodesConfiguration = eicCodesConfiguration;
        this.eicCodesMapper = eicCodesMapper;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.businessLogger = businessLogger;
        this.fullImportComputationService = fullImportComputationService;
        this.exportCornerComputationService = exportCornerComputationService;
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

    void computeTimestamp(TTimestampWrapper timestampWrapper, CseValidRequest cseValidRequest, TcDocumentTypeWriter tcDocumentTypeWriter) {
        if (timestampWrapper.hasNoneOfMniiMnieMiec()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_MISSING_DATA);
        } else if (timestampWrapper.hasMultipleMniiMnieMiec()) {
            tcDocumentTypeWriter.fillTimestampError(timestampWrapper.getTimestamp(), ERROR_MSG_CONTRADICTORY_DATA);
        } else if (timestampWrapper.hasMnii()) {
            fullImportComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        } else if (timestampWrapper.hasMiec()) {
            exportCornerComputationService.computeTimestamp(timestampWrapper, cseValidRequest, tcDocumentTypeWriter);
        } else if (timestampWrapper.hasMnie()) {
            tcDocumentTypeWriter.fillTimestampFullExportSuccess(timestampWrapper.getTimestamp(), timestampWrapper.getMnieValue());
        } else {
            throw new CseValidInvalidDataException(String.format("Unhandled data for timestamp %s and reference calculation time %s", timestampWrapper.getTimeValue(), timestampWrapper.getReferenceCalculationTimeValue()));
        }
    }
}

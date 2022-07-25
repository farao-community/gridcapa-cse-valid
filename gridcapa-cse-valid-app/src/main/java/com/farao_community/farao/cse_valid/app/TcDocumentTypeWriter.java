/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionReport;
import com.farao_community.farao.cse_valid.app.net_position.NetPositionService;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.*;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xsd.etso_code_lists.*;
import xsd.etso_core_cmpts.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.farao_community.farao.cse_valid.app.Constants.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class TcDocumentTypeWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TcDocumentTypeWriter.class);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
    private final CseValidRequest processStartRequest;
    private final TcDocumentType tcDocumentType;
    private final LongIdentificationType documentIdentification;
    private final VersionType versionType;
    private final MessageType messageTypedocumentType;
    private final ProcessType processType;
    private final PartyType senderIdentificationPartyType;
    private final PartyType receiverIdentificationPartyType;
    private final RoleType senderRoleRoleType;
    private final RoleType receiverRoleRoleType;
    private final MessageDateTimeType creationTime;
    private final AreaType domainAreaType;
    private final TimeIntervalType timeIntervalType;
    private final NetPositionService netPositionService;

    public TcDocumentTypeWriter(CseValidRequest processRequest, NetPositionService netPositionService) {
        this.processStartRequest = processRequest;
        this.netPositionService = netPositionService;
        this.tcDocumentType = new TcDocumentType();
        this.documentIdentification = new LongIdentificationType();
        this.versionType = new VersionType();
        this.messageTypedocumentType = new MessageType();
        this.processType = new ProcessType();
        this.senderIdentificationPartyType = new PartyType();
        this.receiverIdentificationPartyType = new PartyType();
        this.senderRoleRoleType = new RoleType();
        this.receiverRoleRoleType = new RoleType();
        this.creationTime = new MessageDateTimeType();
        this.domainAreaType = new AreaType();
        this.timeIntervalType = new TimeIntervalType();
        fillHeaders();
    }

    public InputStream buildTcDocumentType() {
        StringWriter sw = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(TcDocumentType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            JAXBElement<TcDocumentType> root = new JAXBElement<>(new QName("TTC_rtevalidation_document"), TcDocumentType.class, tcDocumentType);
            jaxbMarshaller.marshal(root, sw);
        } catch (JAXBException e) {
            LOGGER.error("Error while writing TTC validation result document ", e);
            throw new CseValidInternalException("Error while writing TTC validation result document ", e);
        }
        return new ByteArrayInputStream(sw.toString().getBytes());
    }

    private void fillHeaders() {
        initializeHeadersData();
        tcDocumentType.setDocumentIdentification(documentIdentification);
        tcDocumentType.setDocumentVersion(versionType);
        tcDocumentType.setDocumentType(messageTypedocumentType);
        tcDocumentType.setProcessType(processType);
        tcDocumentType.setSenderIdentification(senderIdentificationPartyType);
        tcDocumentType.setSenderRole(senderRoleRoleType);
        tcDocumentType.setReceiverIdentification(receiverIdentificationPartyType);
        tcDocumentType.setReceiverRole(receiverRoleRoleType);
        tcDocumentType.setCreationDateTime(creationTime);
        tcDocumentType.setResultTimeInterval(timeIntervalType);
        tcDocumentType.setDomain(domainAreaType);
    }

    private void initializeHeadersData() {
        tcDocumentType.setDtdVersion("1");
        tcDocumentType.setDtdRelease("1");

        documentIdentification.setV(getDocumentIdentification());

        versionType.setV(1);

        messageTypedocumentType.setV(MessageTypeList.A_32);

        processType.setV(ProcessTypeList.A_15);

        senderIdentificationPartyType.setV(SENDER_IDENTIFICATION);
        senderIdentificationPartyType.setCodingScheme(CodingSchemeType.A_01);

        receiverIdentificationPartyType.setV(RECEIVER_IDENTIFICATION);
        receiverIdentificationPartyType.setCodingScheme(CodingSchemeType.A_01);

        senderRoleRoleType.setV(RoleTypeList.A_04);
        receiverRoleRoleType.setV(RoleTypeList.A_04);

        domainAreaType.setCodingScheme(CodingSchemeType.A_01);
        domainAreaType.setV(DOMAIN);

        timeIntervalType.setV(getTimeInterval());
        creationTime.setV(calendarFromDateTime(OffsetDateTime.now()));
    }

    private String getDocumentIdentification() {
        String pattern = String.format("'TTC_RTEValidation_'yyyyMMdd'_%s'e", processStartRequest.getProcessType().getCode());
        return processStartRequest.getTimestamp().format(DateTimeFormatter.ofPattern(pattern, Locale.FRANCE));
    }

    private String getTimeInterval() {
        OffsetDateTime startTime = processStartRequest.getTimestamp().toLocalDate().atStartOfDay().atZone(EUROPE_BRUSSELS_ZONE_ID).toOffsetDateTime();
        OffsetDateTime endTime = startTime.plusDays(1);
        return String.format("%s/%s", startTime.format(DateTimeFormatter.ISO_INSTANT), endTime.format(DateTimeFormatter.ISO_INSTANT));
    }

    private XMLGregorianCalendar calendarFromDateTime(OffsetDateTime offsetDateTime) {
        try {
            GregorianCalendar calendar = GregorianCalendar.from(offsetDateTime.toZonedDateTime());
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            throw new CseValidInternalException("Internal date-time conversion error", e);
        }
    }

    public void fillTimestampWithMissingInputFiles(TTimestamp timestampData, String redFlagError) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = initializeTimestampResult(timestampData);

        TNumber status = new TNumber();
        status.setV(BigInteger.ZERO);
        ts.setSTATUS(status);

        TextType errorMessage = new TextType();
        errorMessage.setV(redFlagError);
        ts.setRedFlagReason(errorMessage);

        listTimestamps.add(ts);
    }

    public void fillNoTtcAdjustmentError(CseValidRequest cseValidRequest) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = new TTimestamp();

        TTime time = new TTime();
        time.setV(cseValidRequest.getTimestamp().toLocalDateTime().atZone(EUROPE_BRUSSELS_ZONE_ID).toOffsetDateTime().format(DATE_TIME_FORMATTER));

        TimeIntervalType timeInterval = new TimeIntervalType();
        timeInterval.setV(cseValidRequest.getTimestamp().withMinute(0).format(DATE_TIME_FORMATTER) + "/" + cseValidRequest.getTimestamp().withMinute(0).plusHours(1).format(DATE_TIME_FORMATTER));

        ts.setReferenceCalculationTime(time);

        ts.setTimeInterval(timeInterval);
        ts.setTime(time);

        TextType textTypeRedFlagReason = new TextType();
        textTypeRedFlagReason.setV(STATUS_ERROR_MESSAGE);

        TNumber statusNumber = new TNumber();
        statusNumber.setV(BigInteger.ZERO);

        ts.setSTATUS(statusNumber);
        ts.setRedFlagReason(textTypeRedFlagReason);

        listTimestamps.add(ts);
    }

    public void fillTimestampNoComputationNeeded(TTimestamp initialTs) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = new TTimestamp();
        ts.setReferenceCalculationTime(initialTs.getReferenceCalculationTime());

        QuantityType mnii = new QuantityType();
        mnii.setV(initialTs.getMiBNII().getV().subtract(initialTs.getANTCFinal().getV()));

        ts.setTimeInterval(initialTs.getTimeInterval());
        ts.setTime(initialTs.getTime());

        TNumber status = new TNumber();
        status.setV(BigInteger.TWO);
        ts.setSTATUS(status);
        ts.setMNII(mnii);
        ts.setTTCLimitedBy(initialTs.getTTCLimitedBy());
        ts.setCRACfile(initialTs.getCRACfile());
        ts.setCGMfile(initialTs.getCGMfile());
        ts.setGSKfile(initialTs.getGSKfile());
        ts.setBASECASEfile(initialTs.getBASECASEfile());
        ts.setLimitingElement(initialTs.getLimitingElement());

        listTimestamps.add(ts);
    }

    private void fillEmptyValidationResults() {
        List<TResultTimeseries> listResultTimeseries = tcDocumentType.getValidationResults();
        listResultTimeseries.clear();
        TResultTimeseries tResultTimeseries = new TResultTimeseries();

        IdentificationType timeSeriesIdentification = new IdentificationType();
        timeSeriesIdentification.setV(processStartRequest.getTimestamp().format(DateTimeFormatter.ofPattern(TIMESERIES_IDENTIFICATION_PATTERN, Locale.FRANCE)));

        BusinessType businessType = new BusinessType();
        businessType.setV(BusinessTypeList.A_81);

        EnergyProductType energyProductType = new EnergyProductType();
        energyProductType.setV(PRODUCT);

        AreaType inArea = new AreaType();
        inArea.setV(IN_AREA);
        inArea.setCodingScheme(CodingSchemeType.A_01);

        AreaType outArea = new AreaType();
        outArea.setV(OUT_AREA);
        outArea.setCodingScheme(CodingSchemeType.A_01);

        UnitOfMeasureType unitOfMeasureType = new UnitOfMeasureType();
        unitOfMeasureType.setV(UnitOfMeasureTypeList.MAW);

        tResultTimeseries.setTimeSeriesIdentification(timeSeriesIdentification);
        tResultTimeseries.setBusinessType(businessType);
        tResultTimeseries.setProduct(energyProductType);
        tResultTimeseries.setInArea(inArea);
        tResultTimeseries.setOutArea(outArea);
        tResultTimeseries.setMeasureUnit(unitOfMeasureType);

        listResultTimeseries.add(tResultTimeseries);
    }

    private TTimestamp initializeTimestampResult(TTimestamp timestampData) {
        TTimestamp ts = new TTimestamp();

        TTime time = new TTime();
        time.setV(timestampData.getTime().getV());

        TimeIntervalType timeInterval = new TimeIntervalType();
        timeInterval.setV(timestampData.getTimeInterval().getV());

        ts.setReferenceCalculationTime(timestampData.getReferenceCalculationTime());

        ts.setTimeInterval(timeInterval);
        ts.setTime(time);

        return ts;
    }

    public void fillTimestampWithDichotomyResponse(TTimestamp timestampData, DichotomyResult<RaoResponse> dichotomyResult, TLimitingElement tLimitingElement) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();

        TTimestamp ts = initializeTimestampResult(timestampData);

        TNumber status = new TNumber();
        status.setV(BigInteger.TWO);

        BigDecimal mibniiValue = timestampData.getMiBNII().getV().subtract(timestampData.getANTCFinal().getV());
        QuantityType mibnii = new QuantityType();
        mibnii.setV(mibniiValue);

        BigDecimal mniiValue = computeMnii(dichotomyResult).map(Math::round).map(BigDecimal::valueOf).orElse(mibniiValue);
        QuantityType mnii = new QuantityType();
        mnii.setV(mniiValue);

        ts.setSTATUS(status);
        ts.setMNII(mnii);
        ts.setMiBNII(mibnii);
        ts.setTTCLimitedBy(timestampData.getTTCLimitedBy());
        ts.setCRACfile(timestampData.getCRACfile());
        ts.setCGMfile(timestampData.getCGMfile());
        ts.setGSKfile(timestampData.getGSKfile());

        ts.setLimitingElement(tLimitingElement);

        listTimestamps.add(ts);

        listTimestamps.sort(Comparator.comparing(c -> OffsetDateTime.parse(c.getTime().getV())));
    }

    public void fillDichotomyError(TTimestamp timestampData) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();

        TTimestamp ts = initializeTimestampResult(timestampData);

        TNumber status = new TNumber();
        status.setV(BigInteger.ONE);
        ts.setSTATUS(status);

        TextType errorMessage = new TextType();
        errorMessage.setV("Process fail during TSO validation phase.");
        ts.setRedFlagReason(errorMessage);

        listTimestamps.add(ts);
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
}

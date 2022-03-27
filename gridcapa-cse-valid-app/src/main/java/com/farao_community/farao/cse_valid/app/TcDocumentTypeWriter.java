/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.api.resource.CseValidRequest;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TLimitingElement;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TNumber;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TResultTimeseries;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTime;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xsd.etso_code_lists.BusinessTypeList;
import xsd.etso_code_lists.CodingSchemeType;
import xsd.etso_code_lists.MessageTypeList;
import xsd.etso_code_lists.ProcessTypeList;
import xsd.etso_code_lists.RoleTypeList;
import xsd.etso_code_lists.UnitOfMeasureTypeList;
import xsd.etso_core_cmpts.AreaType;
import xsd.etso_core_cmpts.BusinessType;
import xsd.etso_core_cmpts.EnergyProductType;
import xsd.etso_core_cmpts.IdentificationType;
import xsd.etso_core_cmpts.LongIdentificationType;
import xsd.etso_core_cmpts.MessageDateTimeType;
import xsd.etso_core_cmpts.MessageType;
import xsd.etso_core_cmpts.PartyType;
import xsd.etso_core_cmpts.ProcessType;
import xsd.etso_core_cmpts.QuantityType;
import xsd.etso_core_cmpts.RoleType;
import xsd.etso_core_cmpts.TextType;
import xsd.etso_core_cmpts.TimeIntervalType;
import xsd.etso_core_cmpts.UnitOfMeasureType;
import xsd.etso_core_cmpts.VersionType;

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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import static com.farao_community.farao.cse_valid.app.Constants.DOMAIN;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_GENERIC;
import static com.farao_community.farao.cse_valid.app.Constants.ERROR_MSG_MISSING_TTC_ADJ_FILE;
import static com.farao_community.farao.cse_valid.app.Constants.EUROPE_BRUSSELS_ZONE_ID;
import static com.farao_community.farao.cse_valid.app.Constants.IN_AREA;
import static com.farao_community.farao.cse_valid.app.Constants.OUT_AREA;
import static com.farao_community.farao.cse_valid.app.Constants.PRODUCT;
import static com.farao_community.farao.cse_valid.app.Constants.RECEIVER_IDENTIFICATION;
import static com.farao_community.farao.cse_valid.app.Constants.SENDER_IDENTIFICATION;
import static com.farao_community.farao.cse_valid.app.Constants.TIMESERIES_IDENTIFICATION_PATTERN;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class TcDocumentTypeWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TcDocumentTypeWriter.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

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

    public TcDocumentTypeWriter(CseValidRequest processRequest) {
        this.processStartRequest = processRequest;
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

    public void fillNoTtcAdjustmentError(CseValidRequest cseValidRequest) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = new TTimestamp();

        TTime time = new TTime();
        time.setV(cseValidRequest.getTime().withOffsetSameInstant(ZoneOffset.UTC).format(DATE_TIME_FORMATTER));

        TimeIntervalType timeInterval = new TimeIntervalType();
        timeInterval.setV(cseValidRequest.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC).withMinute(0).format(DATE_TIME_FORMATTER)
                + "/" + cseValidRequest.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC).withMinute(0).plusHours(1).format(DATE_TIME_FORMATTER));

        ts.setTimeInterval(timeInterval);
        ts.setTime(time);
        ts.setReferenceCalculationTime(time);

        completeFillingWithError(ts, BigInteger.ZERO, ERROR_MSG_MISSING_TTC_ADJ_FILE);

        listTimestamps.add(ts);
    }

    public void fillTimestampError(TTimestamp initialTs, String errorMsg) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = initializeNewTimestampWithExistingTimeData(initialTs);

        completeFillingWithError(ts, BigInteger.ZERO, errorMsg);

        listTimestamps.add(ts);
    }

    public void fillTimestampFullImportSuccess(TTimestamp initialTs, BigDecimal mniiValue) {
        fillTimestampSuccess(initialTs, (ts, value) -> ts.setMNII(buildQuantityType(value)), mniiValue);
    }

    public void fillTimestampFullExportSuccess(TTimestamp initialTs, BigDecimal mnieValue) {
        fillTimestampSuccess(initialTs, (ts, value) -> ts.setMNIE(buildQuantityType(value)), mnieValue);
    }

    public void fillTimestampExportCornerSuccess(TTimestamp initialTs, BigDecimal miecValue) {
        fillTimestampSuccess(initialTs, (ts, value) -> ts.setMIEC(buildQuantityType(value)), miecValue);
    }

    private void fillTimestampSuccess(TTimestamp initialTs, BiConsumer<TTimestamp, BigDecimal> valueTimestampFiller, BigDecimal value) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = initializeNewTimestampWithExistingTimeData(initialTs);

        valueTimestampFiller.accept(ts, value);

        completeFillingWithStatusSuccess(ts, initialTs);

        listTimestamps.add(ts);
    }

    public void fillTimestampWithDichotomyResponse(TTimestamp initialTs, BigDecimal mibniiValue, BigDecimal mniiValue, TLimitingElement tLimitingElement) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = initializeNewTimestampWithExistingTimeData(initialTs);

        ts.setMiBNII(buildQuantityType(mibniiValue));
        ts.setMNII(buildQuantityType(mniiValue));

        completeFillingWithStatusSuccess(ts, initialTs);
        ts.setLimitingElement(tLimitingElement); // override initial value set in completeFillingWithStatusSuccess by default

        listTimestamps.add(ts);

        listTimestamps.sort(Comparator.comparing(c -> OffsetDateTime.parse(c.getTime().getV())));
    }

    public void fillDichotomyError(TTimestamp initialTs) {
        fillEmptyValidationResults();
        List<TTimestamp> listTimestamps = tcDocumentType.getValidationResults().get(0).getTimestamp();
        TTimestamp ts = initializeNewTimestampWithExistingTimeData(initialTs);

        completeFillingWithError(ts, BigInteger.ONE, ERROR_MSG_GENERIC);

        listTimestamps.add(ts);
    }

    private static QuantityType buildQuantityType(BigDecimal value) {
        QuantityType quantityType = new QuantityType();
        quantityType.setV(value);
        return quantityType;
    }

    private static TTimestamp initializeNewTimestampWithExistingTimeData(TTimestamp initialTs) {
        TTimestamp ts = new TTimestamp();

        ts.setTimeInterval(initialTs.getTimeInterval());
        ts.setTime(initialTs.getTime());
        ts.setReferenceCalculationTime(initialTs.getReferenceCalculationTime());

        return ts;
    }

    private static void completeFillingWithError(TTimestamp ts, BigInteger statusNumber, String errorMsg) {
        TNumber status = new TNumber();
        status.setV(statusNumber);
        ts.setSTATUS(status);

        TextType redFlagReason = new TextType();
        redFlagReason.setV(errorMsg);
        ts.setRedFlagReason(redFlagReason);
    }

    private static void completeFillingWithStatusSuccess(TTimestamp ts, TTimestamp initialTs) {
        TNumber status = new TNumber();
        status.setV(BigInteger.TWO);
        ts.setSTATUS(status);
        ts.setCRACfile(initialTs.getCRACfile());
        ts.setCGMfile(initialTs.getCGMfile());
        ts.setGSKfile(initialTs.getGSKfile());
        ts.setTTCLimitedBy(initialTs.getTTCLimitedBy());
        ts.setLimitingElement(initialTs.getLimitingElement());
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
}

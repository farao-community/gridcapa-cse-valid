package com.farao_community.farao.cse_valid.app;

import com.farao_community.farao.cse_valid.api.exception.CseValidInternalException;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TTimestamp;
import com.farao_community.farao.cse_valid.app.ttc_adjustment.TcDocumentType;
import xsd.etso_code_lists.CodingSchemeType;
import xsd.etso_code_lists.MessageTypeList;
import xsd.etso_code_lists.ProcessTypeList;
import xsd.etso_code_lists.RoleTypeList;
import xsd.etso_core_cmpts.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class TcDocumentTypeWriter {
    public static final String SENDER_IDENTIFICATION = "10XFR-RTE------Q";
    public static final String RECEIVER_IDENTIFICATION = "10XFR-RTE------Q";
    public static final String DOMAIN = "10YDOM-1001A061T";
    private final TcDocumentType tcDocumentType;
    private final DateTimeFormatter isoInstantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
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

    public TcDocumentTypeWriter() {
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

        documentIdentification.setV("document identification"); // todo

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

        timeIntervalType.setV("todo"); // todo
        creationTime.setV(calendarFromDateTime(OffsetDateTime.now()));
    }

    private XMLGregorianCalendar calendarFromDateTime(OffsetDateTime offsetDateTime) {
        try {
            GregorianCalendar calendar = GregorianCalendar.from(offsetDateTime.toZonedDateTime());
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            throw new CseValidInternalException("Internal date-time conversion error", e);
        }
    }

    public void writeTimestamp(TTimestamp timestamp, TimestampStatus status) {

    }



    private void writeTimestampNoTTCAdjustmentFile() {
        TcDocumentTypeWriter output = new TcDocumentTypeWriter();
    }

    private void writeTimestampMissingInputsFiles(TTimestamp timestamp) {

    }

    private void writeTimestampMissingDatas(TTimestamp timestamp) {

    }

    private void writeTimestampNoComputationNeeded(TTimestamp timestamp) {

    }
}

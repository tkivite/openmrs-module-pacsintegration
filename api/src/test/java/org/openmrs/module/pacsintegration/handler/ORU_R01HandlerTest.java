package org.openmrs.module.pacsintegration.handler;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v23.message.ACK;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.module.emr.EmrProperties;
import org.openmrs.module.emr.radiology.RadiologyOrder;
import org.openmrs.module.emr.radiology.RadiologyService;
import org.openmrs.module.pacsintegration.PacsIntegrationGlobalProperties;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ORU_R01HandlerTest {

    private ORU_R01Handler handler;

    private AdministrationService adminService;

    private PatientService patientService;

    private EncounterService encounterService;

    private RadiologyService radiologyService;

    private EmrProperties emrProperties;

    private PatientIdentifierType primaryIdentifierType = new PatientIdentifierType();

    @Before
    public void setup() {
        adminService = mock(AdministrationService.class);
        when(adminService.getGlobalProperty(PacsIntegrationGlobalProperties.SENDING_FACILITY)).thenReturn("openmrs_mirebalais");

        patientService = mock(PatientService.class);
        radiologyService = mock(RadiologyService.class);
        encounterService = mock(EncounterService.class);

        emrProperties = mock(EmrProperties.class);
        when(emrProperties.getPrimaryIdentifierType()).thenReturn(primaryIdentifierType);

        handler = new ORU_R01Handler();
        handler.setAdminService(adminService);
        handler.setPatientService(patientService);
        handler.setEncounterService(encounterService);
        handler.setRadiologyService(radiologyService);
        handler.setEmrProperties(emrProperties);
    }

    @Test
    public void shouldReturnErrorACKIfNoPatientIdentifierInResponse() throws HL7Exception, ApplicationException {

        String message = "MSH|^~\\&|HMI||RAD|REPORTS|20130228174643||ORU^R01|RTS01CE16057B105AC0|P|2.3|\r" +
                "PID|1||||Patient^Test^||19770222|M||||||||||\r" +
                "PV1|1||||||||||||||||||\r" +
                "OBR|1||0000001297|36554-4^CHEST|||20130228170350||||||||||||MBL^CR||||||P|||||||&Goodrich&Mark&&&&^||||20130228170350\r" +
                "OBX|1|RP|||||||||F\r" +
                "OBX|2|TX|EventType^EventType|1|REVIEWED\r" +
                "OBX|3|CN|Technologist^Technologist|1|1435^Duck^Donald\r" +
                "OBX|4|TX|ExamRoom^ExamRoom|1|100AcreWoods\r" +
                "OBX|5|TS|StartDateTime^StartDateTime|1|20111009215317\r" +
                "OBX|6|TS|StopDateTime^StopDateTime|1|20111009215817\r" +
                "ZDS|2.16.840.1.113883.3.234.1.3.101.1.2.1013.2011.15607503.2^HMI^Application^DICOM\r";

        ACK ack = (ACK) handler.processMessage(parseMessage(message));

        assertThat(ack.getMSA().getAcknowledgementCode().getValue(), is("AR"));
        assertThat(ack.getMSA().getTextMessage().getValue(), is("Cannot import ORU_R01 message. No patient identifier specified."));
    }

    @Test
    public void shouldReturnErrorACKIfNoPatientWithIdentifier() throws HL7Exception, ApplicationException {

        when(patientService.getPatients(null, "GG2F98", Collections.singletonList(primaryIdentifierType), true))
                .thenReturn(new ArrayList<Patient>());

        String message = "MSH|^~\\&|HMI||RAD|REPORTS|20130228174643||ORU^R01|RTS01CE16057B105AC0|P|2.3|\r" +
                "PID|1||GG2F98||Patient^Test^||19770222|M||||||||||\r" +
                "PV1|1||||||||||||||||||\r" +
                "OBR|1||0000001297|36554-4^CHEST|||20130228170350||||||||||||MBL^CR||||||P|||||||&Goodrich&Mark&&&&^||||20130228170350\r" +
                "OBX|1|RP|||||||||F\r" +
                "OBX|2|TX|EventType^EventType|1|REVIEWED\r" +
                "OBX|3|CN|Technologist^Technologist|1|1435^Duck^Donald\r" +
                "OBX|4|TX|ExamRoom^ExamRoom|1|100AcreWoods\r" +
                "OBX|5|TS|StartDateTime^StartDateTime|1|20111009215317\r" +
                "OBX|6|TS|StopDateTime^StopDateTime|1|20111009215817\r" +
                "ZDS|2.16.840.1.113883.3.234.1.3.101.1.2.1013.2011.15607503.2^HMI^Application^DICOM\r";

        ACK ack = (ACK) handler.processMessage(parseMessage(message));

        assertThat(ack.getMSA().getAcknowledgementCode().getValue(), is("AR"));
        assertThat(ack.getMSA().getTextMessage().getValue(), is("Cannot import ORU_R01 message. No patient with identifier GG2F98"));
    }

    @Test
    public void shouldReturnErrorACKIfPatientIdentifierAndAccessionNumberDontMatchSamePatient() throws HL7Exception, ApplicationException {

        Patient patient = new Patient(1);
        Patient anotherPatient = new Patient(2);

        RadiologyOrder radiologyOrder = new RadiologyOrder();
        radiologyOrder.setPatient(anotherPatient);

        when(patientService.getPatients(null, "GG2F98", Collections.singletonList(primaryIdentifierType), true))
                .thenReturn(Collections.singletonList(patient));

        when(radiologyService.getRadiologyOrderByAccessionNumber("0000001297")).thenReturn(radiologyOrder);

        String message = "MSH|^~\\&|HMI||RAD|REPORTS|20130228174643||ORU^R01|RTS01CE16057B105AC0|P|2.3|\r" +
                "PID|1||GG2F98||Patient^Test^||19770222|M||||||||||\r" +
                "PV1|1||||||||||||||||||\r" +
                "OBR|1||0000001297|36554-4^CHEST|||20130228170350||||||||||||MBL^CR||||||P|||||||&Goodrich&Mark&&&&^||||20130228170350\r" +
                "OBX|1|RP|||||||||F\r" +
                "OBX|2|TX|EventType^EventType|1|REVIEWED\r" +
                "OBX|3|CN|Technologist^Technologist|1|1435^Duck^Donald\r" +
                "OBX|4|TX|ExamRoom^ExamRoom|1|100AcreWoods\r" +
                "OBX|5|TS|StartDateTime^StartDateTime|1|20111009215317\r" +
                "OBX|6|TS|StopDateTime^StopDateTime|1|20111009215817\r" +
                "ZDS|2.16.840.1.113883.3.234.1.3.101.1.2.1013.2011.15607503.2^HMI^Application^DICOM\r";

        ACK ack = (ACK) handler.processMessage(parseMessage(message));

        assertThat(ack.getMSA().getAcknowledgementCode().getValue(), is("AR"));
        assertThat(ack.getMSA().getTextMessage().getValue(), is("Cannot import ORU_R01 message. Patient referenced in message different from patient attached to existing order."));
    }

    @Test
    public void shouldReturnACKButNotSaveEncounterIfMessageNotStudyCompleteMessage() throws HL7Exception, ApplicationException {

        Patient patient = new Patient(1);
        RadiologyOrder radiologyOrder = new RadiologyOrder();
        radiologyOrder.setPatient(patient);

        when(patientService.getPatients(null, "GG2F98", Collections.singletonList(primaryIdentifierType), true))
                .thenReturn(Collections.singletonList(patient));

        when(radiologyService.getRadiologyOrderByAccessionNumber("0000001297")).thenReturn(radiologyOrder);

        String message = "MSH|^~\\&|HMI||RAD|REPORTS|20130228174643||ORU^R01|RTS01CE16057B105AC0|P|2.3|\r" +
                "PID|1||GG2F98||Patient^Test^||19770222|M||||||||||\r" +
                "PV1|1||||||||||||||||||\r" +
                "OBR|1||0000001297|36554-4^CHEST|||20130228170350||||||||||||MBL^CR||||||P|||||||&Goodrich&Mark&&&&^||||20130228170350\r" +
                "OBX|1|RP|||||||||F\r" +
                "OBX|2|TX|EventType^EventType|1|REVIEWED\r" +
                "OBX|3|CN|Technologist^Technologist|1|1435^Duck^Donald\r" +
                "OBX|4|TX|ExamRoom^ExamRoom|1|100AcreWoods\r" +
                "OBX|5|TS|StartDateTime^StartDateTime|1|20111009215317\r" +
                "OBX|6|TS|StopDateTime^StopDateTime|1|20111009215817\r" +
                "ZDS|2.16.840.1.113883.3.234.1.3.101.1.2.1013.2011.15607503.2^HMI^Application^DICOM\r";

        ACK ack = (ACK) handler.processMessage(parseMessage(message));

        assertThat(ack.getMSA().getAcknowledgementCode().getValue(), is("AA"));
        verify(encounterService, never()).saveEncounter(any(Encounter.class));
    }

    @Test
    public void shouldSaveStudyCompleteEncounter() throws HL7Exception, ApplicationException {

        Patient patient = new Patient(1);
        RadiologyOrder radiologyOrder = new RadiologyOrder();
        radiologyOrder.setPatient(patient);

        when(patientService.getPatients(null, "GG2F98", Collections.singletonList(primaryIdentifierType), true))
                .thenReturn(Collections.singletonList(patient));

        when(radiologyService.getRadiologyOrderByAccessionNumber("0000001297")).thenReturn(radiologyOrder);

        String message = "MSH|^~\\&|HMI||RAD|REPORTS|20130228174643||ORU^R01|RTS01CE16057B105AC0|P|2.3|\r" +
                "PID|1||GG2F98||Patient^Test^||19770222|M||||||||||\r" +
                "PV1|1||||||||||||||||||\r" +
                "OBR|1||0000001297|36554-4^CHEST|||20130228170350||||||||||||MBL^CR||||||P|||||||&Goodrich&Mark&&&&^||||20130228170350\r" +
                "OBX|1|RP|||||||||F\r" +
                "OBX|2|TX|EventType^EventType|1|StudyComplete\r" +
                "OBX|3|CN|Technologist^Technologist|1|1435^Duck^Donald\r" +
                "OBX|4|TX|ExamRoom^ExamRoom|1|100AcreWoods\r" +
                "OBX|5|TS|StartDateTime^StartDateTime|1|20111009215317\r" +
                "OBX|6|TS|StopDateTime^StopDateTime|1|20111009215817\r" +
                "ZDS|2.16.840.1.113883.3.234.1.3.101.1.2.1013.2011.15607503.2^HMI^Application^DICOM\r";

        ACK ack = (ACK) handler.processMessage(parseMessage(message));

        assertThat(ack.getMSA().getAcknowledgementCode().getValue(), is("AA"));

       // TODO: change this to a more specific expected match (once we set that up)
        verify(encounterService).saveEncounter(any(Encounter.class));
    }

    private Message parseMessage(String message) throws HL7Exception {
        Parser parser = new PipeParser();
        return parser.parse(message);
    }
}

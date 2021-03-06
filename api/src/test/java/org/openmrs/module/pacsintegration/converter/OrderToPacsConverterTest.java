/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.pacsintegration.converter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.radiologyapp.RadiologyOrder;
import org.openmrs.module.radiologyapp.RadiologyProperties;
import org.openmrs.module.pacsintegration.PacsIntegrationConstants;
import org.openmrs.module.pacsintegration.PacsIntegrationProperties;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class OrderToPacsConverterTest {

    private OrderToPacsConverter converter;

    private PatientIdentifierType patientIdentifierType = new PatientIdentifierType();

    private Concept testXrayConcept;

    private Concept testXrayConceptSet;

    private EncounterRole clinicialEncounterRole;

    private Location examLocation;

    // TODO: test some error cases

    @Before
    public void setup() {
        ConceptMapType sameAsConceptMapType = new ConceptMapType();
        sameAsConceptMapType.setName("SAME-AS");
        sameAsConceptMapType.setUuid(UUID.randomUUID().toString());

        ConceptSource procedureCodeConceptSource = new ConceptSource();
        procedureCodeConceptSource.setUuid(UUID.randomUUID().toString());

        LocationAttributeType locationCodeAttributeType = new LocationAttributeType();
        locationCodeAttributeType.setUuid(UUID.randomUUID().toString());

        ConceptName testXrayConceptName = new ConceptName();
        testXrayConceptName.setName("Left-hand x-ray");
        testXrayConceptName.setLocale(new Locale("en"));

        ConceptReferenceTerm testXrayConceptReferenceTerm = new ConceptReferenceTerm();
        testXrayConceptReferenceTerm.setCode("123ABC");
        testXrayConceptReferenceTerm.setConceptSource(procedureCodeConceptSource);

        ConceptMap sameAsConceptMap = new ConceptMap();
        sameAsConceptMap.setConceptMapType(sameAsConceptMapType);
        sameAsConceptMap.setConceptReferenceTerm(testXrayConceptReferenceTerm);

        testXrayConcept = new Concept();
        testXrayConcept.addName(testXrayConceptName);
        testXrayConcept.addConceptMapping(sameAsConceptMap);

        testXrayConceptSet = new Concept();
        testXrayConceptSet.addSetMember(testXrayConcept);

        clinicialEncounterRole = new EncounterRole();

        examLocation = new Location();
        examLocation.setId(2);
        examLocation.setName("Radiology");

        LocationAttribute locationCode = new LocationAttribute();
        locationCode.setValue("ABCDEF");
        locationCode.setAttributeType(locationCodeAttributeType);
        examLocation.setAttribute(locationCode);

        User authenticatedUser = new User();

        mockStatic(Context.class);
        PatientService patientService = mock(PatientService.class);
        AdministrationService administrationService = mock(AdministrationService.class);
        ConceptService conceptService = mock(ConceptService.class);
        LocationService locationService = mock(LocationService.class);
        RadiologyProperties radiologyProperties = mock(RadiologyProperties.class);
        EmrApiProperties emrProperties = mock(EmrApiProperties.class);
        PacsIntegrationProperties pacsIntegrationProperties = mock(PacsIntegrationProperties.class);

        when(Context.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(patientService.getPatientIdentifierTypeByUuid(anyString())).thenReturn(patientIdentifierType);

        when(administrationService.getGlobalProperty(PacsIntegrationConstants.GP_SENDING_FACILITY)).thenReturn("openmrs_mirebalais");
        when(administrationService.getGlobalProperty(PacsIntegrationConstants.GP_PROCEDURE_CODE_CONCEPT_SOURCE_UUID)).thenReturn(procedureCodeConceptSource.getUuid());
        when(administrationService.getGlobalProperty(PacsIntegrationConstants.GP_LOCATION_CODE_ATTRIBUTE_TYPE_UUID)).thenReturn(locationCodeAttributeType.getUuid());

        when(conceptService.getConceptMapTypeByUuid(PacsIntegrationConstants.SAME_AS_CONCEPT_MAP_TYPE_UUID)).thenReturn(sameAsConceptMapType);
        when(pacsIntegrationProperties.getProcedureCodesConceptSource()).thenReturn(procedureCodeConceptSource);

        when(locationService.getLocationAttributeTypeByUuid(locationCodeAttributeType.getUuid())).thenReturn(locationCodeAttributeType);

        when(emrProperties.getOrderingProviderEncounterRole()).thenReturn(clinicialEncounterRole);
        when(radiologyProperties.getXrayOrderablesConcept()).thenReturn(testXrayConceptSet);

        converter = new OrderToPacsConverter();
        converter.setPatientService(patientService);
        converter.setAdminService(administrationService);
        converter.setConceptService(conceptService);
        converter.setLocationService(locationService);
        converter.setRadiologyProperties(radiologyProperties);
        converter.setEmrApiProperties(emrProperties);
        converter.setPacsIntegrationProperties(pacsIntegrationProperties);
    }

    @Test
    public void shouldGenerateMessageFromAnOrder() throws Exception {
        RadiologyOrder order = new RadiologyOrder();
        UUID uuid = UUID.randomUUID();
        order.setAccessionNumber(uuid.toString());
        order.setStartDate(new SimpleDateFormat("MM-dd-yyyy").parse("08-08-2012"));
        order.setPatient(createPatient());
        order.setConcept(testXrayConcept);
        order.setUrgency(Order.Urgency.STAT);
        order.setClinicalHistory("Patient fell off horse\r\nAnd broke back");
        order.setExamLocation(examLocation);

        order.setEncounter(createEncounter());
        order.getEncounter().addProvider(clinicialEncounterRole, createProvider());

        String hl7Message = converter.convertToPacsFormat(order, "SC");

        assertThat(hl7Message, startsWith("MSH|^~\\&||openmrs_mirebalais|||"));
        // TODO: test that a valid date is passed
        assertThat(hl7Message, containsString("||ORM^O01||P|2.3\r"));
        assertThat(hl7Message, containsString("PID|||6TS-4||Chebaskwony^Collet||19760825000000|F\r"));
        assertThat(hl7Message, containsString("PV1|||ABCDEF^^^^^^^^Radiology|||||123^Joseph^Wayne"));
        assertThat(hl7Message, containsString("ORC|SC\r"));
        assertThat(hl7Message, endsWith("OBR|||" + uuid.toString() + "|123ABC^Left-hand x-ray|||||||||||||||CR||||||||^^^^^STAT||||^Patient fell off horse~^And broke back|||||20120808000000\r"));
    }

    @Test
    public void shouldGenerateMessageForAnonymousPatient() throws Exception {
        RadiologyOrder order = new RadiologyOrder();
        UUID uuid = UUID.randomUUID();
        order.setAccessionNumber(uuid.toString());
        order.setStartDate(new SimpleDateFormat("MM-dd-yyyy").parse("08-08-2012"));
        order.setPatient(createAnonymousPatient());
        order.setConcept(testXrayConcept);
        order.setUrgency(Order.Urgency.ROUTINE);
        order.setClinicalHistory("Patient fell off horse");

        order.setEncounter(createEncounter());

        String hl7Message = converter.convertToPacsFormat(order, "SC");

        assertThat(hl7Message, startsWith("MSH|^~\\&||openmrs_mirebalais|||"));
        // TODO: test that a valid date is passed
        assertThat(hl7Message, containsString("||ORM^O01||P|2.3\r"));
        assertThat(hl7Message, containsString("PID|||6TS-4||UNKNOWN^UNKNOWN|||F\r"));
        assertThat(hl7Message, containsString("ORC|SC\r"));
        assertThat(hl7Message, endsWith("OBR|||" + uuid.toString() + "|123ABC^Left-hand x-ray|||||||||||||||CR||||||||||||^Patient fell off horse|||||20120808000000\r"));
    }


    private Patient createPatient() throws ParseException {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setIdentifier("6TS-4");
        patientIdentifier.setIdentifierType(patientIdentifierType);

        PersonName patientName = new PersonName();
        patientName.setFamilyName("Chebaskwony");
        patientName.setGivenName("Collet");

        Patient patient = new Patient();
        patient.addIdentifier(patientIdentifier);
        patient.addName(patientName);
        Date birthDate = new SimpleDateFormat("MM-dd-yyyy").parse("08-25-1976");
        patient.setBirthdate(birthDate);
        patient.setGender("F");
        return patient;
    }

    private Patient createAnonymousPatient() throws ParseException {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setIdentifier("6TS-4");
        patientIdentifier.setIdentifierType(patientIdentifierType);

        PersonName patientName = new PersonName();
        patientName.setFamilyName("UNKNOWN");
        patientName.setGivenName("UNKNOWN");

        Patient patient = new Patient();
        patient.addIdentifier(patientIdentifier);
        patient.addName(patientName);

        patient.setGender("F");
        return patient;
    }

    private Provider createProvider() {
        Provider provider = new Provider();

        Person person = new Person();
        PersonName providerName = new PersonName();
        providerName.setFamilyName("Joseph");
        providerName.setGivenName("Wayne");
        person.addName(providerName);

        provider.setPerson(person);
        provider.setIdentifier("123");
        return provider;
    }

    private Encounter createEncounter() {
        return new Encounter();
    }

}

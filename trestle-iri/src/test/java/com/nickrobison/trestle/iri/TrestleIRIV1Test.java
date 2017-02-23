package com.nickrobison.trestle.iri;

import com.nickrobison.trestle.iri.exceptions.IRIParseException;
import com.nickrobison.trestle.iri.exceptions.IRIVersionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 1/23/17.
 */
public class TrestleIRIV1Test {

    public static final String OBJECT_ID = "test-object";
    public static final String TEST_PREFIX = "http://nickrobison.com/test.owl#";
    public static final String OBJECT_FACT = "test_fact";
    public static final OffsetDateTime TEST_DATE = OffsetDateTime.of(LocalDateTime.of(1983, 3, 26, 0, 0, 0), ZoneOffset.UTC);
    public static final OffsetDateTime TEST_DB_TEMPORAL = OffsetDateTime.of(LocalDateTime.of(2016, 3, 26, 0, 0, 0), ZoneOffset.UTC);
    public static IRI objectNoFactIRI;
    public static IRI objectFactIRI;
    public static IRI malformedIRIVersion;
    public static IRI malformedIRIStructure;

    @BeforeAll
    public static void setupTestStrings() {
        objectNoFactIRI = IRI.create(TEST_PREFIX, String.format("V1:%s:%s:%s", OBJECT_ID, TEST_DATE.toEpochSecond(), TEST_DB_TEMPORAL.toEpochSecond()));
        objectFactIRI = IRI.create(TEST_PREFIX, String.format("V1:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toEpochSecond(), TEST_DB_TEMPORAL.toEpochSecond()));
        malformedIRIVersion = IRI.create(TEST_PREFIX, String.format("V9:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toEpochSecond(), TEST_DB_TEMPORAL.toEpochSecond()));
        malformedIRIStructure = IRI.create(TEST_PREFIX, String.format("V1;%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toEpochSecond(), TEST_DB_TEMPORAL.toEpochSecond()));
    }

    @Test
    public void testEncode() {
//        V1 Encode
        final TrestleIRI objectIRI = IRIBuilder.encodeIRI(IRIVersion.V1, TEST_PREFIX, OBJECT_ID, null, TEST_DATE, TEST_DB_TEMPORAL);
        final TrestleIRI factIRI = IRIBuilder.encodeIRI(IRIVersion.V1, TEST_PREFIX, OBJECT_ID, OBJECT_FACT, TEST_DATE, TEST_DB_TEMPORAL);
        assertAll(() -> assertEquals(objectNoFactIRI, objectIRI.getIRI(), "Object (no fact) is incorrectly encoded"),
                () -> assertEquals(objectFactIRI, factIRI.getIRI(), "Object (with fact) is incorrectly encoded"));
    }

    @Test
    public void testDecode() {
        assertAll(() -> assertEquals(OBJECT_ID, IRIBuilder.getObjectID(objectFactIRI), "Object (with fact) has incorrectly decoded ID"),
                () -> assertEquals(OBJECT_ID, IRIBuilder.getObjectID(objectNoFactIRI), "Object (without fact) has incorrectly decoded ID"),
                () -> assertEquals(OBJECT_FACT, IRIBuilder.getObjectFact(objectFactIRI).orElse(""), "Object (with fact) has incorrectly decoded Fact ID"),
                () -> assertEquals("", IRIBuilder.getObjectFact(objectNoFactIRI).orElse(""), "Object (without fact) has incorrectly decoded Fact ID"),
                () -> assertEquals(TEST_DATE, IRIBuilder.getObjectTemporal(objectFactIRI).get(), "Object (with fact) has incorrectly decoded Object Temporal"),
                () -> assertEquals(TEST_DATE, IRIBuilder.getObjectTemporal(objectNoFactIRI).get(), "Object (no fact) has incorrectly decoded Object Temporal"),
                () -> assertEquals(TEST_DB_TEMPORAL, IRIBuilder.getDatabaseTemporal(objectFactIRI).get(), "Object (with fact) has incorrectly decoded Database Temporal"),
                () -> assertEquals(TEST_DB_TEMPORAL, IRIBuilder.getDatabaseTemporal(objectNoFactIRI).get(), "Object (no fact) has incorrectly decoded Database Temporal"));
    }

    @Test
    public void testTZ() {
        final OffsetDateTime PDTTime = OffsetDateTime.of(LocalDateTime.of(1989, 3, 26, 8, 15, 15), ZoneOffset.ofHours(-8));
        final OffsetDateTime everestTime = OffsetDateTime.of(LocalDateTime.of(1989, 3, 26, 8, 15, 15), ZoneOffset.ofHoursMinutes(5, 45));
        final IRI timeZoneTestIRI = IRIBuilder.encodeIRI(IRIVersion.V1, TEST_PREFIX, OBJECT_ID, null, PDTTime, everestTime).getIRI();
        assertAll(() -> assertTrue(PDTTime.atZoneSameInstant(ZoneOffset.UTC).toString().equals(IRIBuilder.getObjectTemporal(timeZoneTestIRI).get().toString()), "Object temporals don't match at UTC"),
                () -> assertTrue(everestTime.atZoneSameInstant(ZoneOffset.UTC).toString().equals(IRIBuilder.getDatabaseTemporal(timeZoneTestIRI).get().toString()), "Database temporals don't match at UTC"));
    }

    @Test
    public void testOtherVersions() {
        assertAll(() -> assertThrows(IRIVersionException.class, () -> IRIBuilder.encodeIRI(IRIVersion.V2, TEST_PREFIX, OBJECT_ID, null, null, null)),
                () -> assertThrows(IRIVersionException.class, () -> IRIBuilder.encodeIRI(IRIVersion.V3, TEST_PREFIX, OBJECT_ID, null, null, null)));
    }

    @Test
    public void testExpandedString() {
        final TrestleIRI expandedIRITest = IRIBuilder.encodeIRI(IRIVersion.V1, TEST_PREFIX, IRI.create(TEST_PREFIX, OBJECT_ID).getIRIString(), IRI.create(TEST_PREFIX, "testFact").toString(), null, null);
        assertAll(() -> assertEquals(OBJECT_ID, expandedIRITest.getObjectID(), "Object ID should not be fully expanded"),
                () -> assertEquals("testFact", expandedIRITest.getObjectFact().get(), "Object Fact should not be fully expanded"));

    }

    @Test
    public void testIRIParsing() {
        final TrestleIRI objectTIRI = IRIBuilder.parseIRIToTrestleIRI(objectFactIRI);
        assertAll(() -> assertEquals(OBJECT_ID, objectTIRI.getObjectID(), "Parsed IRI should have the same ObjectID"),
                () -> assertTrue(objectTIRI.getObjectFact().isPresent(), "Parsed IRI should have ObjectFact"),
                () -> assertEquals(OBJECT_FACT, objectTIRI.getObjectFact().get(), "Parsed IRI should have same ObjectFact"),
                () -> assertTrue(objectTIRI.getObjectTemporal().isPresent(), "Parsed IRI should have object temporal"),
                () -> assertEquals(TEST_DATE.toEpochSecond(), objectTIRI.getObjectTemporal().get().toEpochSecond(), "Parsed IRI should have the same Object temporal"),
                () -> assertTrue(objectTIRI.getDbTemporal().isPresent(), "Parsed IRI should have database temporal"),
                () -> assertEquals(TEST_DB_TEMPORAL.toEpochSecond(), objectTIRI.getDbTemporal().get().toEpochSecond(), "Parsed IRI should have same DB temporal"));

//        Test malformed IRI
        assertAll(() -> assertThrows(IRIParseException.class, () -> IRIBuilder.parseIRIToTrestleIRI(malformedIRIVersion)),
                () -> assertThrows(IRIParseException.class, () -> IRIBuilder.parseIRIToTrestleIRI(malformedIRIStructure)));
    }
}

package com.nickrobison.trestle.iri;

import com.nickrobison.trestle.iri.exceptions.IRIParseException;
import com.nickrobison.trestle.iri.exceptions.IRIVersionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.semanticweb.owlapi.model.IRI;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 1/23/17.
 */
@SuppressWarnings({"initialization.fields.uninitialized", "squid:S2187"})
public class TrestleIRIV1Test {

    public static final String OBJECT_ID = "test-object";
    public static final String OBJECT_FACT = "test_fact";
    public static final OffsetDateTime TEST_DATE = OffsetDateTime.of(LocalDateTime.of(1983, 3, 26, 0, 0, 0), ZoneOffset.UTC);
    public static final OffsetDateTime TEST_DB_TEMPORAL = OffsetDateTime.of(LocalDateTime.of(2016, 3, 26, 0, 0, 0), ZoneOffset.UTC);

    @PrefixParams
    @DisplayName("Test Encoding")
    public void testEncode(String prefix) {
        final IRI objectNoFactIRI = IRI.create(prefix, String.format("V1:%s:%s:%s", OBJECT_ID, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));
        final IRI objectFactIRI = IRI.create(prefix, String.format("V1:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));

//        V1 Encode
        final TrestleIRI objectIRI = IRIBuilder.encodeIRI(IRIVersion.V1, prefix, OBJECT_ID, null, TEST_DATE, TEST_DB_TEMPORAL);
        final TrestleIRI factIRI = IRIBuilder.encodeIRI(IRIVersion.V1, prefix, OBJECT_ID, OBJECT_FACT, TEST_DATE, TEST_DB_TEMPORAL);
        assertAll(() -> assertEquals(objectNoFactIRI, objectIRI.getIRI(), "Object (no fact) is incorrectly encoded"),
                () -> assertEquals(objectFactIRI, factIRI.getIRI(), "Object (with fact) is incorrectly encoded"));
    }

    @PrefixParams
    @DisplayName("Test Decoding")
    public void testDecode(String prefix) {
        final IRI objectNoFactIRI = IRI.create(prefix, String.format("V1:%s:%s:%s", OBJECT_ID, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));
        final IRI objectFactIRI = IRI.create(prefix, String.format("V1:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));

//        V1 Decode
        assertAll(() -> assertEquals(OBJECT_ID, IRIBuilder.getObjectID(objectFactIRI), "Object (with fact) has incorrectly decoded ID"),
                () -> assertEquals(OBJECT_ID, IRIBuilder.getObjectID(objectNoFactIRI), "Object (without fact) has incorrectly decoded ID"),
                () -> assertEquals(OBJECT_FACT, IRIBuilder.getObjectFact(objectFactIRI).orElse(""), "Object (with fact) has incorrectly decoded Fact ID"),
                () -> assertEquals("", IRIBuilder.getObjectFact(objectNoFactIRI).orElse(""), "Object (without fact) has incorrectly decoded Fact ID"),
                () -> assertEquals(TEST_DATE, IRIBuilder.getObjectTemporal(objectFactIRI).get(), "Object (with fact) has incorrectly decoded Object Temporal"),
                () -> assertEquals(TEST_DATE, IRIBuilder.getObjectTemporal(objectNoFactIRI).get(), "Object (no fact) has incorrectly decoded Object Temporal"),
                () -> assertEquals(TEST_DB_TEMPORAL, IRIBuilder.getDatabaseTemporal(objectFactIRI).get(), "Object (with fact) has incorrectly decoded Database Temporal"),
                () -> assertEquals(TEST_DB_TEMPORAL, IRIBuilder.getDatabaseTemporal(objectNoFactIRI).get(), "Object (no fact) has incorrectly decoded Database Temporal"));
    }

    @PrefixParams
    @DisplayName("Test Timezone")
    public void testTZ(String prefix) {


        final OffsetDateTime PDTTime = OffsetDateTime.of(LocalDateTime.of(1989, 3, 26, 8, 15, 15), ZoneOffset.ofHours(-8));
        final OffsetDateTime everestTime = OffsetDateTime.of(LocalDateTime.of(1989, 3, 26, 8, 15, 15), ZoneOffset.ofHoursMinutes(5, 45));
        final IRI timeZoneTestIRI = IRIBuilder.encodeIRI(IRIVersion.V1, prefix, OBJECT_ID, null, PDTTime, everestTime).getIRI();
        assertAll(() -> assertTrue(PDTTime.atZoneSameInstant(ZoneOffset.UTC).toString().equals(IRIBuilder.getObjectTemporal(timeZoneTestIRI).get().toString()), "Object temporals don't match at UTC"),
                () -> assertTrue(everestTime.atZoneSameInstant(ZoneOffset.UTC).toString().equals(IRIBuilder.getDatabaseTemporal(timeZoneTestIRI).get().toString()), "Database temporals don't match at UTC"));
    }

    @PrefixParams
    @DisplayName("Test Timestamp Conflicts")
    public void testTimestampConflicts(String prefix) {
        final OffsetDateTime PDTTime = OffsetDateTime.of(LocalDateTime.of(1989, 3, 26, 8, 15, 15, 5000), ZoneOffset.ofHours(-8));
        final OffsetDateTime everestTime = OffsetDateTime.of(LocalDateTime.of(1989, 3, 26, 8, 15, 15, 50000), ZoneOffset.ofHoursMinutes(5, 45));
        assertNotEquals(IRIBuilder.encodeIRI(IRIVersion.V1, prefix, OBJECT_ID, null, PDTTime, null), IRIBuilder.encodeIRI(IRIVersion.V1, prefix, OBJECT_ID, null, everestTime, null), "sub-second precision should not have equal results");
    }

    @PrefixParams
    @DisplayName("Test Other Versions")
    public void testOtherVersions(String prefix) {
        assertAll(() -> assertThrows(IRIVersionException.class, () -> IRIBuilder.encodeIRI(IRIVersion.V2, prefix, OBJECT_ID, null, null, null)),
                () -> assertThrows(IRIVersionException.class, () -> IRIBuilder.encodeIRI(IRIVersion.V3, prefix, OBJECT_ID, null, null, null)));
    }

    @PrefixParams
    @DisplayName("Test Expanded Strings")
    public void testExpandedString(String prefix) {
        final TrestleIRI expandedIRITest = IRIBuilder.encodeIRI(IRIVersion.V1, prefix,
                IRI.create(prefix, OBJECT_ID).getIRIString(),
                IRI.create(prefix, "testFact").toString(),
                null, null);
        assertAll(() -> assertEquals(OBJECT_ID, expandedIRITest.getObjectID(), "Object ID should not be fully expanded"),
                () -> assertEquals("testFact", expandedIRITest.getObjectFact().get(), "Object Fact should not be fully expanded"));

    }

    @PrefixParams
    @DisplayName("Test IRI Parsing")
    public void testIRIParsing(String prefix) {

        final IRI objectFactIRI = IRI.create(prefix, String.format("V1:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));
        final IRI malformedIRIVersion = IRI.create(prefix, String.format("V9:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));
        final IRI malformedIRIStructure = IRI.create(prefix, String.format("V1;%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));

//        V1 parsing
        final TrestleIRI objectTIRI = IRIBuilder.parseIRIToTrestleIRI(objectFactIRI);
        assertAll(() -> assertEquals(OBJECT_ID, objectTIRI.getObjectID(), "Parsed IRI should have the same ObjectID"),
                () -> assertTrue(objectTIRI.getObjectFact().isPresent(), "Parsed IRI should have ObjectFact"),
                () -> assertEquals(OBJECT_FACT, objectTIRI.getObjectFact().get(), "Parsed IRI should have same ObjectFact"),
                () -> assertTrue(objectTIRI.getObjectTemporal().isPresent(), "Parsed IRI should have object temporal"),
                () -> assertEquals(TEST_DATE.toInstant().toEpochMilli(), objectTIRI.getObjectTemporal().get().toInstant().toEpochMilli(), "Parsed IRI should have the same Object temporal"),
                () -> assertTrue(objectTIRI.getDbTemporal().isPresent(), "Parsed IRI should have database temporal"),
                () -> assertEquals(TEST_DB_TEMPORAL.toInstant().toEpochMilli(), objectTIRI.getDbTemporal().get().toInstant().toEpochMilli(), "Parsed IRI should have same DB temporal"));

//        Test malformed IRI
        assertAll(() -> assertThrows(IRIParseException.class, () -> IRIBuilder.parseIRIToTrestleIRI(malformedIRIVersion)),
                () -> assertThrows(IRIParseException.class, () -> IRIBuilder.parseIRIToTrestleIRI(malformedIRIStructure)));
    }

    @PrefixParams
    @DisplayName("Test Partials")
    public void testPartials(String prefix) {
        final IRI objectFactIRI = IRI.create(prefix, String.format("V1:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toInstant().toEpochMilli(), TEST_DB_TEMPORAL.toInstant().toEpochMilli()));

        final IRI withoutDatabase = IRI.create(prefix, String.format("V1:%s@%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toInstant().toEpochMilli()));
        final TrestleIRI objectTIRI = IRIBuilder.parseIRIToTrestleIRI(objectFactIRI);
        assertEquals(IRIBuilder.parseIRIToTrestleIRI(withoutDatabase), objectTIRI.withoutDatabase(), "Should property remove the database temporal");
    }

    @Test
    @DisplayName("Test Empty IRI")
    public void testMalformedIRI() {
        assertThrows(IRIParseException.class, () -> IRIBuilder.parseIRIToTrestleIRI(IRI.create("http://something-something.com/V1")));
    }


    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "Prefix: {0}")
    @ValueSource(strings = {"http://nickrobison.com/test.owl/", "http://nickrobison.com/test.owl#"})
    @interface PrefixParams {
    }
}

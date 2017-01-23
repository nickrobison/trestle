package com.nickrobison.trestle.iri;

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
public class TrestleIRITest {

    public static final String OBJECT_ID = "test-object";
    public static final String TEST_PREFIX = "http://nickrobison.com/test.owl#";
    public static final String OBJECT_FACT = "test_fact";
    public static final OffsetDateTime TEST_DATE = OffsetDateTime.of(LocalDateTime.of(1983, 3, 26, 0, 0, 0), ZoneOffset.UTC);
    public static final OffsetDateTime TEST_DB_TEMPORAL = OffsetDateTime.of(LocalDateTime.of(2016, 3, 26, 0, 0, 0), ZoneOffset.UTC);
    public static IRI objectNoFactIRI;
    public static IRI objectFactIRI;

    @BeforeAll
    public static void setupTestStrings() {
        objectNoFactIRI = IRI.create(TEST_PREFIX, String.format("V1:%s:%s:%s", OBJECT_ID, TEST_DATE.toEpochSecond(), TEST_DB_TEMPORAL.toEpochSecond()));
        objectFactIRI = IRI.create(TEST_PREFIX, String.format("V1:%s@%s:%s:%s", OBJECT_ID, OBJECT_FACT, TEST_DATE.toEpochSecond(), TEST_DB_TEMPORAL.toEpochSecond()));
    }

    @Test
    public void testEncode() {
        final ITrestleIRIBuilder iriBuilder = TrestleIRIFactory.getIRIBuilder(IRIVersion.V1);
        final IRI objectIRI = iriBuilder.encodeIRI(TEST_PREFIX, OBJECT_ID, null, TEST_DATE, TEST_DB_TEMPORAL);
        final IRI factIRI = iriBuilder.encodeIRI(TEST_PREFIX, OBJECT_ID, OBJECT_FACT, TEST_DATE, TEST_DB_TEMPORAL);
        assertAll(() -> assertEquals(objectNoFactIRI, objectIRI, "Object (no fact) is incorrectly encoded"),
                () -> assertEquals(objectFactIRI, factIRI, "Object (with fact) is incorrectly encoded"));
    }

    @Test
    public void testOtherVersions() {
        assertAll(() -> assertThrows(RuntimeException.class, () -> TrestleIRIFactory.getIRIBuilder(IRIVersion.V2)),
                () -> assertThrows(RuntimeException.class, () -> TrestleIRIFactory.getIRIBuilder(IRIVersion.V3)));
    }
}

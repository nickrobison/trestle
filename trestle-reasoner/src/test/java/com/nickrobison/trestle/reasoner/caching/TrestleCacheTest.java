package com.nickrobison.trestle.reasoner.caching;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 2/17/17.
 */
@Tag("integration")
@Disabled
public class TrestleCacheTest extends AbstractReasonerTest {

    private static final Logger logger = LoggerFactory.getLogger(TrestleCacheTest.class);

    @Override
    protected String getTestName() {
        return "cache_test";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.JTSGeometryTest.class);
    }

    @Test
    public void testCache() throws TrestleClassException, MissingOntologyEntity, ParseException {
        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
        final Geometry jtsGeom2 = new WKTReader().read("POINT(27.0 91.0)");
        final TestClasses.JTSGeometryTest jtsGeometryTest = new TestClasses.JTSGeometryTest(4326, jtsGeom, LocalDate.of(1989, 3, 16));
//        Try to load some data then read it back out.
//        final TestClasses.OffsetDateTimeTest offsetDateTimeTest = new TestClasses.OffsetDateTimeTest(5515, OffsetDateTime.now(), OffsetDateTime.now().plusYears(5));
        reasoner.writeTrestleObject(jtsGeometryTest);
        reasoner.getUnderlyingOntology().runInference();

        Instant firstStart = Instant.now();
        TestClasses.JTSGeometryTest first = reasoner.readTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), LocalDate.of(1989, 7, 1), null);
        Instant firstEnd = Instant.now();
        Instant secondStart = Instant.now();
        logger.info("Reading first object took {} ms", Duration.between(firstStart, firstEnd).toMillis());
        TestClasses.JTSGeometryTest second = reasoner.readTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), LocalDate.of(1990, 3, 1), null);
        Instant secondEnd = Instant.now();
        logger.info("Reading second object took {} ms", Duration.between(secondStart, secondEnd).toMillis());
        assertEquals(first, second, "Objects should be equal");
        assertTrue(Duration.between(firstStart, firstEnd).compareTo(Duration.between(secondStart, secondEnd)) > 0, "Cache should have lower latency");


//        Update one of the facts, which should invalidate the cache
        logger.info("Updating one of the facts");
        reasoner.addFactToTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), "geom", jtsGeom2, LocalDate.of(1989, 10, 1), null, null);
        reasoner.getUnderlyingOntology().runInference();
        firstStart = Instant.now();
        first = reasoner.readTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), LocalDate.of(1989, 10, 20), null);
        firstEnd = Instant.now();
        secondStart = Instant.now();
        logger.info("Reading second object took {} ms", Duration.between(firstStart, firstEnd).toMillis());
        second = reasoner.readTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), LocalDate.of(1990, 3, 1), null);
        secondEnd = Instant.now();
        assertEquals(first, second, "Objects should be equal");
        assertTrue(Duration.between(firstStart, firstEnd).compareTo(Duration.between(secondStart, secondEnd)) > 0, "Cache should have lower latency");

////        Test for invalidation
//        mock(TrestleCache.IndividualCacheEntryListener.class);
    }
}

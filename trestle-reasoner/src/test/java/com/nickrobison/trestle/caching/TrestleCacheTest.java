package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.TestClasses;
import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 2/17/17.
 */
@Tag("integration")
public class TrestleCacheTest {

    private static final Logger logger = LoggerFactory.getLogger(TrestleCacheTest.class);
    private static final String OVERRIDE_PREFIX = "http://nickrobison.com/test-owl#";
    private TrestleReasoner reasoner;

    @BeforeEach
    public void setup() {
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf"));
        reasoner = new TrestleBuilder()
                .withDBConnection(config.getString("trestle.ontology.connectionString"),
                        config.getString("trestle.ontology.username"),
                        config.getString("trestle.ontology.password"))
                .withName("cache_test")
                .withOntology(IRI.create(config.getString("trestle.ontology.location")))
                .withPrefix(OVERRIDE_PREFIX)
                .withInputClasses(TestClasses.JTSGeometryTest.class)
                .initialize()
                .build();

//        df = OWLManager.getOWLDataFactory();
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
        reasoner.addFactToTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), "geom", jtsGeom2, LocalDate.of(1990, 10, 1), null, null);
        firstStart = Instant.now();
        first = reasoner.readTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), LocalDate.of(1989, 10, 20), null);
        firstEnd = Instant.now();
        secondStart = Instant.now();
        logger.info("Reading second object took {} ms", Duration.between(firstStart, firstEnd).toMillis());
        second = reasoner.readTrestleObject(TestClasses.JTSGeometryTest.class, jtsGeometryTest.getAdm0_code().toString(), LocalDate.of(1990, 3, 1), null);
        secondEnd = Instant.now();
        assertEquals(first, second, "Objects should be equal");
        assertTrue(Duration.between(firstStart, firstEnd).compareTo(Duration.between(secondStart, secondEnd)) > 0, "Cache should have lower latency");
    }

    @AfterEach
    public void shutdown() {
        reasoner.shutdown(true);
    }
}

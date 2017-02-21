package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.TestClasses;
import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.io.ParseException;
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
                .withInputClasses(TestClasses.OffsetDateTimeTest.class)
                .initialize()
                .build();

//        df = OWLManager.getOWLDataFactory();
    }

    @Test
    public void testCache() throws TrestleClassException, MissingOntologyEntity, ParseException {
//        Try to load some data then read it back out.
//        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
//        final TestClasses.JTSGeometryTest jtsGeometryTest = new TestClasses.JTSGeometryTest(4326, jtsGeom, LocalDate.of(1989, 3, 26));
        final TestClasses.OffsetDateTimeTest offsetDateTimeTest = new TestClasses.OffsetDateTimeTest(5515, OffsetDateTime.now(), OffsetDateTime.now().plusYears(5));
        reasoner.writeTrestleObject(offsetDateTimeTest);
        reasoner.getUnderlyingOntology().runInference();
        final Instant firstStart = Instant.now();
        final TestClasses.OffsetDateTimeTest first = reasoner.readTrestleObject(TestClasses.OffsetDateTimeTest.class, offsetDateTimeTest.adm0_code.toString(), LocalDate.of(2017, 7, 1), null);
        final Instant firstEnd = Instant.now();
        final Instant secondStart = Instant.now();
        logger.info("Reading first object took {} ms", Duration.between(firstStart, firstEnd).toMillis());
        final TestClasses.OffsetDateTimeTest second = reasoner.readTrestleObject(TestClasses.OffsetDateTimeTest.class, offsetDateTimeTest.adm0_code.toString(), LocalDate.of(2018, 3, 1), null);
        final Instant secondEnd = Instant.now();
        logger.info("Reading second object took {} ms", Duration.between(secondStart, secondEnd).toMillis());
        assertEquals(first, second, "Objects should be equal");
        assertTrue(Duration.between(firstStart, firstEnd).compareTo(Duration.between(secondStart, secondEnd)) > 0, "Cache should have lower latency");
//        reasoner.addFactToTrestleObject(TestClasses.GAULComplexClassTest.class, jtsGeometryTest.getAdm0_code().toString(), "testInteger", 71, LocalDate.of(2016, 3, 1), null, null);
    }

    @AfterEach
    public void shutdown() {
        reasoner.shutdown(true);
    }
}

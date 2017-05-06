package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.annotations.DatasetClass;
import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TemporalType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 9/14/16.
 */
@Tag("integration")
public class TimezoneTest {

    private static TrestleReasoner reasoner;
    private static OWLDataFactory df;

    @BeforeAll
    public static void setup() {
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf"));
        reasoner = new TrestleBuilder()
                .withDBConnection(config.getString("trestle.ontology.connectionString"),
                        config.getString("trestle.ontology.username"),
                        config.getString("trestle.ontology.password"))
                .withName("timezone_tests")
                .withInputClasses(DefaultTimeZone.class, DifferentIntervalTimeZones.class)
                .withOntology(IRI.create(config.getString("trestle.ontology.location")))
                .withoutMetrics()
                .withoutCaching()
                .initialize()
                .build();

        df = OWLManager.getOWLDataFactory();
    }

    @Test
    public void testDefaultTimeZone() throws TrestleClassException, MissingOntologyEntity {
        final DefaultTimeZone defaultTimeZone = new DefaultTimeZone(LocalDate.of(1990, 1, 1).atStartOfDay(), "default-timezone");
        reasoner.writeTrestleObject(defaultTimeZone);
        reasoner.getUnderlyingOntology().runInference();
        @NonNull final DefaultTimeZone returnedDefaultTimeZone = reasoner.readTrestleObject(DefaultTimeZone.class, "default-timezone", LocalDate.of(1990, 1, 1).atStartOfDay(), null);
        assertEquals(defaultTimeZone, returnedDefaultTimeZone, "Should be equal");
        assertEquals(defaultTimeZone.defaultTime, returnedDefaultTimeZone.defaultTime, "Times should match");
    }

    @Test
    public void testDifferentIntervalTimeZones() throws TrestleClassException, MissingOntologyEntity {
        final DifferentIntervalTimeZones differentIntervalTimeZones = new DifferentIntervalTimeZones("different-intervals", LocalDate.of(1990, 1, 1).atStartOfDay(), LocalDate.of(1995, 1, 1).atStartOfDay());
        reasoner.writeTrestleObject(differentIntervalTimeZones);
        reasoner.getUnderlyingOntology().runInference();
        @NonNull final DifferentIntervalTimeZones returnedIntervalTimeZones = reasoner.readTrestleObject(DifferentIntervalTimeZones.class, "different-intervals", LocalDate.of(1993, 1, 1).atStartOfDay(), null);
        assertEquals(differentIntervalTimeZones, returnedIntervalTimeZones, "Should be equal");
    }

    @AfterAll
    public static void shutdown() {
//        reasoner.writeOntology(new File("/Users/nrobison/Desktop/tz.owl").toURI(), false);
        reasoner.shutdown(true);
    }


    @DatasetClass(name = "defaulttimezone-test")
    public static class DefaultTimeZone {
        @IndividualIdentifier
        public String id;
        @DefaultTemporal(type = TemporalType.POINT, duration = 1, unit = ChronoUnit.YEARS, timeZone = "America/Los_Angeles")
        public LocalDateTime defaultTime;

        public DefaultTimeZone(LocalDateTime defaultTime, String id) {
            this.defaultTime = defaultTime;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DefaultTimeZone that = (DefaultTimeZone) o;

            if (!id.equals(that.id)) return false;
            return defaultTime.equals(that.defaultTime);

        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + defaultTime.hashCode();
            return result;
        }
    }

    @DatasetClass(name = "intervaltimezone-test")
    public static class DifferentIntervalTimeZones {
        private final String id;
        private final LocalDateTime startTime;
        @EndTemporal(timeZone = "America/Los_Angeles")
        public final LocalDateTime endTime;

        public DifferentIntervalTimeZones(String id, LocalDateTime startTime, LocalDateTime endTime) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @IndividualIdentifier
        public String getId() {
            return id;
        }

        @StartTemporal
        public LocalDateTime getStartTime() {
            return startTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DifferentIntervalTimeZones that = (DifferentIntervalTimeZones) o;

            if (!getId().equals(that.getId())) return false;
            if (!getStartTime().equals(that.getStartTime())) return false;
            return endTime.equals(that.endTime);

        }

        @Override
        public int hashCode() {
            int result = getId().hashCode();
            result = 31 * result + getStartTime().hashCode();
            result = 31 * result + endTime.hashCode();
            return result;
        }
    }
}

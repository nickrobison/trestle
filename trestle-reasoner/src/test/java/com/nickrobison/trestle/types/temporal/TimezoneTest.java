package com.nickrobison.trestle.types.temporal;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TemporalType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 9/14/16.
 */
@Tag("integration")
public class TimezoneTest extends AbstractReasonerTest {

    @Override
    protected String getTestName() {
        return "timezone_tests";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(DefaultTimeZone.class, DifferentIntervalTimeZones.class);
    }

    @Test
    public void testDefaultTimeZone() throws TrestleClassException, MissingOntologyEntity {
        final DefaultTimeZone defaultTimeZone = new DefaultTimeZone(LocalDate.of(1990, 1, 1).atStartOfDay(), "default-timezone");
        reasoner.writeTrestleObject(defaultTimeZone);
        @NonNull final DefaultTimeZone returnedDefaultTimeZone = reasoner.readTrestleObject(DefaultTimeZone.class, "default-timezone", LocalDate.of(1990, 1, 1).atStartOfDay(), null);
        assertEquals(defaultTimeZone, returnedDefaultTimeZone, "Should be equal");
        assertEquals(defaultTimeZone.defaultTime, returnedDefaultTimeZone.defaultTime, "Times should match");
    }

    @Test
    public void testDifferentIntervalTimeZones() throws TrestleClassException, MissingOntologyEntity {
        final DifferentIntervalTimeZones differentIntervalTimeZones = new DifferentIntervalTimeZones("different-intervals", LocalDate.of(1990, 1, 1).atStartOfDay(), LocalDate.of(1995, 1, 1).atStartOfDay());
        reasoner.writeTrestleObject(differentIntervalTimeZones);
        @NonNull final DifferentIntervalTimeZones returnedIntervalTimeZones = reasoner.readTrestleObject(DifferentIntervalTimeZones.class, "different-intervals", LocalDate.of(1993, 1, 1).atStartOfDay(), null);
        assertEquals(differentIntervalTimeZones, returnedIntervalTimeZones, "Should be equal");
    }

    @DatasetClass(name = "defaulttimezone-test")
    public static class DefaultTimeZone implements Serializable {
        private static long serialVersionUID = 42L;

        @IndividualIdentifier
        public String id;
        @DefaultTemporal(type = TemporalType.POINT, duration = 1, unit = ChronoUnit.YEARS, timeZone = "America/Los_Angeles")
        public LocalDateTime defaultTime;

        public DefaultTimeZone(LocalDateTime defaultTime, String id) {
            this.defaultTime = defaultTime;
            this.id = id;
        }

        @Override
        public boolean equals(@Nullable Object o) {
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
        public boolean equals(@Nullable Object o) {
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

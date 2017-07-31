package com.nickrobison.trestle.common;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalComparisonTest {

    @Test
    public void compareOffsetDateTimes() {
        final OffsetDateTime odtComparison = OffsetDateTime.of(LocalDate.of(1989, 3, 26), LocalTime.NOON, ZoneOffset.UTC);
        final OffsetDateTime odtDifferentZone = OffsetDateTime.of(LocalDate.of(1989, 3, 26), LocalTime.NOON.plusHours(6), ZoneOffset.ofHours(6));

        final LocalDate localDateSame = LocalDate.of(1989, 3, 26);
        final LocalDate localDateBefore = LocalDate.of(1980, 12, 17);
        final LocalDate localDateAfter = LocalDate.of(2005, 12, 17);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(odtComparison, localDateSame), "Should be equal to LocalDate"),
                () -> assertEquals(0, TemporalUtils.compareTemporals(odtComparison, odtDifferentZone), "Should be equal to ODT in different Zone"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(odtComparison, localDateAfter), "Should be before LocalDate"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(odtComparison, localDateBefore), "Should be after LocalDate"));

        final ZonedDateTime zonedEqual = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedBefore = ZonedDateTime.of(localDateBefore, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedAfter = ZonedDateTime.of(localDateAfter, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedEqualDifferentZone = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("UTC-8"));

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(odtComparison, zonedEqual), "Should be equal to ZonedDateTime"),
                () -> assertEquals(0, TemporalUtils.compareTemporals(odtComparison, zonedEqualDifferentZone), "Should be equal to ZonedDateTime in different zone"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(odtComparison, zonedAfter), "Should be before ZonedDateTime"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(odtComparison, zonedBefore), "Should be after ZonedDateTime"));

        final LocalDateTime localDTSame = LocalDateTime.of(localDateSame, LocalTime.NOON);
        final LocalDateTime localDTBefore = LocalDateTime.of(localDateSame, LocalTime.MIN);
        final LocalDateTime localDTAfter = LocalDateTime.of(localDateSame, LocalTime.MAX);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(odtComparison, localDTSame), "Should be equal to LocalDateTime"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(odtComparison, localDTAfter), "Should be before LocalDateTime"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(odtComparison, localDTBefore), "Should be after LocalDateTime"));

        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> TemporalUtils.compareTemporals(odtComparison, new TestTemporal())),
                () -> assertThrows(IllegalArgumentException.class, () -> TemporalUtils.compareTemporals(new TestTemporal(), odtComparison)));
    }

    @Test
    public void compareZonedDateTimes() {
        final ZonedDateTime zonedComparison = ZonedDateTime.of(LocalDate.of(1989, 3, 26), LocalTime.NOON, ZoneOffset.UTC);

        final LocalDate localDateSame = LocalDate.of(1989, 3, 26);
        final LocalDate localDateBefore = LocalDate.of(1980, 12, 17);
        final LocalDate localDateAfter = LocalDate.of(2005, 12, 17);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(zonedComparison, localDateSame), "Should be equal to LocalDate"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(zonedComparison, localDateAfter), "Should be before LocalDate"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(zonedComparison, localDateBefore), "Should be after LocalDate"));

        final ZonedDateTime zonedEqual = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedBefore = ZonedDateTime.of(localDateBefore, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedAfter = ZonedDateTime.of(localDateAfter, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedEqualDifferentZone = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("UTC-8"));

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(zonedComparison, zonedEqual), "Should be equal to ZonedDateTime"),
                () -> assertEquals(0, TemporalUtils.compareTemporals(zonedComparison, zonedEqualDifferentZone), "Should be equal to ZonedDateTime in different zone"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(zonedComparison, zonedAfter), "Should be before ZonedDateTime"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(zonedComparison, zonedBefore), "Should be after ZonedDateTime"));

        final LocalDateTime localDTSame = LocalDateTime.of(localDateSame, LocalTime.NOON);
        final LocalDateTime localDTBefore = LocalDateTime.of(localDateSame, LocalTime.MIN);
        final LocalDateTime localDTAfter = LocalDateTime.of(localDateSame, LocalTime.MAX);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(zonedComparison, localDTSame), "Should be equal to LocalDateTime"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(zonedComparison, localDTAfter), "Should be before LocalDateTime"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(zonedComparison, localDTBefore), "Should be after LocalDateTime"));

        assertThrows(IllegalArgumentException.class, () -> TemporalUtils.compareTemporals(zonedComparison, new TestTemporal()));
    }

    @Test
    public void compareLocalDateTime() {
        final LocalDateTime ldtComparison = LocalDateTime.of(LocalDate.of(1989, 3, 26), LocalTime.NOON);

        final LocalDate localDateSame = LocalDate.of(1989, 3, 26);
        final LocalDate localDateBefore = LocalDate.of(1980, 12, 17);
        final LocalDate localDateAfter = LocalDate.of(2005, 12, 17);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(ldtComparison, localDateSame), "Should be equal to LocalDate"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(ldtComparison, localDateAfter), "Should be before LocalDate"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(ldtComparison, localDateBefore), "Should be after LocalDate"));

        final ZonedDateTime zonedEqual = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedBefore = ZonedDateTime.of(localDateBefore, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedAfter = ZonedDateTime.of(localDateAfter, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedEqualDifferentZone = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("UTC-8"));

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(ldtComparison, zonedEqual), "Should be equal to ZonedDateTime"),
                () -> assertNotEquals(0, TemporalUtils.compareTemporals(ldtComparison, zonedEqualDifferentZone), "Should not be equal to ZonedDateTime in different zone"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(ldtComparison, zonedAfter), "Should be before ZonedDateTime"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(ldtComparison, zonedBefore), "Should be after ZonedDateTime"));

        final LocalDateTime localDTSame = LocalDateTime.of(localDateSame, LocalTime.NOON);
        final LocalDateTime localDTBefore = LocalDateTime.of(localDateSame, LocalTime.MIN);
        final LocalDateTime localDTAfter = LocalDateTime.of(localDateSame, LocalTime.MAX);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(ldtComparison, localDTSame), "Should be equal to LocalDateTime"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(ldtComparison, localDTAfter), "Should be before LocalDateTime"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(ldtComparison, localDTBefore), "Should be after LocalDateTime"));

        assertThrows(IllegalArgumentException.class, () -> TemporalUtils.compareTemporals(ldtComparison, new TestTemporal()));
    }

    @Test
    public void compareLocalDate() {
        final LocalDate localDateComparison = LocalDate.of(1989, 3, 26);

        final LocalDate localDateSame = LocalDate.of(1989, 3, 26);
        final LocalDate localDateBefore = LocalDate.of(1980, 12, 17);
        final LocalDate localDateAfter = LocalDate.of(2005, 12, 17);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(localDateComparison, localDateSame), "Should be equal to LocalDate"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(localDateComparison, localDateAfter), "Should be before LocalDate"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(localDateComparison, localDateBefore), "Should be after LocalDate"));

        final ZonedDateTime zonedEqual = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedBefore = ZonedDateTime.of(localDateBefore, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedAfter = ZonedDateTime.of(localDateAfter, LocalTime.NOON, ZoneOffset.UTC);
        final ZonedDateTime zonedEqualDifferentZone = ZonedDateTime.of(localDateSame, LocalTime.NOON, ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("UTC-8"));
        final ZonedDateTime zonedNotEqualDifferentZone = zonedEqualDifferentZone.withZoneSameInstant(ZoneId.of("UTC-13"));

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(localDateComparison, zonedEqual), "Should be equal to ZonedDateTime"),
                () -> assertEquals(0, TemporalUtils.compareTemporals(localDateComparison, zonedEqualDifferentZone), "Should be equal to ZonedDateTime in different zone"),
                () -> assertNotEquals(0, TemporalUtils.compareTemporals(localDateComparison, zonedNotEqualDifferentZone), "Should not be equal to ZonedDate time when zone goes past midnight"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(localDateComparison, zonedAfter), "Should be before ZonedDateTime"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(localDateComparison, zonedBefore), "Should be after ZonedDateTime"));

        final LocalDateTime localDTSameTime = LocalDateTime.of(localDateSame, LocalTime.NOON);
        final LocalDateTime localDTBeforeTime = LocalDateTime.of(localDateSame, LocalTime.MIN);
        final LocalDateTime localDTAfterTime = LocalDateTime.of(localDateSame, LocalTime.MAX);
        final LocalDateTime localDTBeforeDate = LocalDateTime.of(localDateBefore, LocalTime.NOON);
        final LocalDateTime localDTAfterDate = LocalDateTime.of(localDateAfter, LocalTime.NOON);

        assertAll(() -> assertEquals(0, TemporalUtils.compareTemporals(localDateComparison, localDTSameTime), "Should be equal to LocalDateTime with same date"),
                () -> assertEquals(0, TemporalUtils.compareTemporals(localDateComparison, localDTAfterTime), "Should be equal LocalDateTime with same date, different time"),
                () -> assertEquals(0, TemporalUtils.compareTemporals(localDateComparison, localDTBeforeTime), "Should be equal LocalDateTime with same date, different time"),
                () -> assertEquals(-1, TemporalUtils.compareTemporals(localDateComparison, localDTAfterDate), "Should be before LocalDateTime with later date"),
                () -> assertEquals(1, TemporalUtils.compareTemporals(localDateComparison, localDTBeforeDate), "Should be be after LocalDateTime with earlier date"));

        assertThrows(IllegalArgumentException.class, () -> TemporalUtils.compareTemporals(localDateComparison, new TestTemporal()));
    }

    private static class TestTemporal implements Temporal {

        TestTemporal() {

        }

        @Override
        public boolean isSupported(TemporalUnit unit) {
            return false;
        }

        @Override
        public Temporal with(TemporalField field, long newValue) {
            return null;
        }

        @Override
        public Temporal plus(long amountToAdd, TemporalUnit unit) {
            return null;
        }

        @Override
        public long until(Temporal endExclusive, TemporalUnit unit) {
            return 0;
        }

        @Override
        public boolean isSupported(TemporalField field) {
            return false;
        }

        @Override
        public long getLong(TemporalField field) {
            return 0;
        }
    }
}


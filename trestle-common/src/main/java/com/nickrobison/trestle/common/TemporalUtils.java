package com.nickrobison.trestle.common;

import java.time.*;
import java.time.temporal.Temporal;

public class TemporalUtils {

    private TemporalUtils() {}

    /**
     * Compares two temporals against each other
     * In the event that the temporals have different levels of resolution, they will be casted down to the lowest level of precision
     * If an {@link LocalDateTime} or {@link java.time.LocalDate} are passed, they will be compared at offset {@link ZoneOffset#UTC}
     * When comparing a {@link ZonedDateTime} with {@link LocalDateTime} or {@link LocalDate} ZoneOffset adjustment is not taken into account
     *
     * @param compareTo      - {@link Temporal} temporal to compare against
     * @param compareAgainst - {@link Temporal} temporal to compare to
     * @return - Returns {@code -1} if compareTo is before compareAgainst. {@code 0} if both are equal. {@code 1} if compareTo is after compareAgainst.
     */
    public static int compareTemporals(Temporal compareTo, Temporal compareAgainst) {
        if (compareTo instanceof OffsetDateTime) {
            return normalizeCompare(compareOffsetDateTimeAndTemporal((OffsetDateTime) compareTo, compareAgainst));
        } else if (compareTo instanceof ZonedDateTime) {
            return normalizeCompare(compareZonedDateTimeAndTemporal((ZonedDateTime) compareTo, compareAgainst));
        } else if (compareTo instanceof LocalDateTime) {
            return normalizeCompare(compareLocalDateTimeAndTemporal((LocalDateTime) compareTo, compareAgainst));
        } else if (compareTo instanceof LocalDate) {
            return normalizeCompare(compareLocalDateAndTemporal((LocalDate) compareTo, compareAgainst));
        }

        throw new IllegalArgumentException(String.format("Cannot compare %s against %s", compareTo.getClass(), compareAgainst.getClass()));
    }

    private static int compareOffsetDateTimeAndTemporal(OffsetDateTime compareTo, Temporal compareAgainst) {
        if (compareAgainst instanceof OffsetDateTime) {
            return compareTo.compareTo(((OffsetDateTime) compareAgainst).withOffsetSameInstant(compareTo.getOffset()));
        } else if (compareAgainst instanceof ZonedDateTime) {
            return compareTo.atZoneSameInstant(((ZonedDateTime) compareAgainst).getZone()).compareTo((ZonedDateTime) compareAgainst);
        } else if (compareAgainst instanceof LocalDateTime) {
            return compareTo.toLocalDateTime().compareTo((LocalDateTime) compareAgainst);
        } else if (compareAgainst instanceof LocalDate) {
            return compareTo.toLocalDate().compareTo((LocalDate) compareAgainst);
        }
        throw new IllegalArgumentException(String.format("Cannot compare OffsetDateTime against %s", compareAgainst.getClass()));
    }

    private static int compareZonedDateTimeAndTemporal(ZonedDateTime compareTo, Temporal compareAgainst) {
        if (compareAgainst instanceof OffsetDateTime) {
            return compareTo.compareTo(((OffsetDateTime) compareAgainst).atZoneSameInstant(compareTo.getZone()));
        } else if (compareAgainst instanceof ZonedDateTime) {
            return compareTo.compareTo(((ZonedDateTime) compareAgainst).withZoneSameInstant(compareTo.getZone()));
        } else if (compareAgainst instanceof LocalDateTime) {
            return compareTo.toLocalDateTime().compareTo((LocalDateTime) compareAgainst);
        } else if (compareAgainst instanceof LocalDate) {
            return compareTo.toLocalDate().compareTo((LocalDate) compareAgainst);
        }
        throw new IllegalArgumentException(String.format("Cannot compare ZonedDateTime against %s", compareAgainst.getClass()));
    }

    private static int compareLocalDateTimeAndTemporal(LocalDateTime compareTo, Temporal compareAgainst) {
        if (compareAgainst instanceof OffsetDateTime) {
            return compareTo.compareTo(((OffsetDateTime) compareAgainst).toLocalDateTime());
        } else if (compareAgainst instanceof ZonedDateTime) {
            return compareTo.compareTo(((ZonedDateTime) compareAgainst).toLocalDateTime());
        } else if (compareAgainst instanceof LocalDateTime) {
            return compareTo.compareTo(((LocalDateTime) compareAgainst));
        } else if (compareAgainst instanceof LocalDate) {
            return compareTo.toLocalDate().compareTo((LocalDate) compareAgainst);
        }
        throw new IllegalArgumentException(String.format("Cannot compare LocalDateTime against %s", compareAgainst.getClass()));
    }

    private static int compareLocalDateAndTemporal(LocalDate compareTo, Temporal compareAgainst) {
        if (compareAgainst instanceof OffsetDateTime) {
            return compareTo.compareTo(((OffsetDateTime) compareAgainst).toLocalDate());
        } else if (compareAgainst instanceof ZonedDateTime) {
            return compareTo.compareTo(((ZonedDateTime) compareAgainst).toLocalDate());
        } else if (compareAgainst instanceof LocalDateTime) {
            return compareTo.compareTo(((LocalDateTime) compareAgainst).toLocalDate());
        } else if (compareAgainst instanceof LocalDate) {
            return compareTo.compareTo((LocalDate) compareAgainst);
        }
        throw new IllegalArgumentException(String.format("Cannot compare LocalDate against %s", compareAgainst.getClass()));
    }

    private static int normalizeCompare(int compareValue) {
        return Integer.compare(compareValue, 0);
    }
}

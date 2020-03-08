package com.nickrobison.trestle.gaulintegrator.UnitTests;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created by nrobison on 5/6/16.
 */
public class TemporalAdjustmentTests {

    @Test
    public void testDateAdd() {
        LocalDate testYear = LocalDate.of(1990, Month.DECEMBER, 10);
        LocalDate testDate = testYear.plusYears(1).with(TemporalAdjusters.firstDayOfYear());
        assertEquals(LocalDate.of(1991, Month.JANUARY, 01), testDate, "Should be first day of next year");
    }

    @Test
    public void testDateAdjust() {
        LocalDate testYear = LocalDate.of(1990, Month.AUGUST, 10);
        assertEquals(LocalDate.of(1990, Month.JANUARY, 1), testYear.with(TemporalAdjusters.firstDayOfYear()), "Should be the same dates");
    }
}

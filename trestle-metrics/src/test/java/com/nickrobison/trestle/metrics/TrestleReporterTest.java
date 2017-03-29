package com.nickrobison.trestle.metrics;

import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 3/20/17.
 */
public class TrestleReporterTest {

    private TrestleMetrician trestleMetrician;
    private TrestleMetricsReporter reporter;
    private static Injector injector;

    @BeforeAll
    public static void staticInit() {
        injector = Guice.createInjector(new MetricsModule());
    }

    @BeforeEach
    public void setup() {
        trestleMetrician = injector.getInstance(TrestleMetrician.class);
        reporter = trestleMetrician.getReporter();
    }

    @AfterEach
    public void teardown() {
        trestleMetrician.shutdown(new File("./target/metricsTest.csv"));
    }

    @Test
    public void testSimpleClass() throws InterruptedException {
        final TestMetricsClass metricsClass = new TestMetricsClass();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        reporter.report();
        metricsClass.testMeter();
        assertAll(() -> assertEquals(63, trestleMetrician.getRegistry().getGauges().size(), "Should have gauges"),
                () -> assertEquals(1, trestleMetrician.getRegistry().getMeters().size(), "Should have meters"),
                () -> assertEquals(1, trestleMetrician.getRegistry().getCounters().size(), "Should have timers"),
                () -> assertEquals(2, trestleMetrician.getRegistry().counter("com.nickrobison.trestle.metrics.TrestleReporterTest$TestMetricsClass.test-reporter-counter").getCount(), "Count should be 2"));
        Thread.sleep(500);
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        metricsClass.testIncrement();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testIncrement();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        Thread.sleep(10000);
        reporter.report();
        assertEquals(17, trestleMetrician.getRegistry().counter("com.nickrobison.trestle.metrics.TrestleReporterTest$TestMetricsClass.test-reporter-counter").getCount(), "Should be incremented correctly");
    }


    @Metriced
    private class TestMetricsClass {

        TestMetricsClass() {
        }


        @Gauge
        int testReporterGauge() {
            return 7;
        }

        @CounterIncrement(name = "test-reporter-counter")
        void testIncrement() {
        }

        @Metered
        void testMeter() {
        }
    }
}

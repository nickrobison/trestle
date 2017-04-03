package com.nickrobison.metrician;

import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.metrician.MetricsModule;
import com.nickrobison.metrician.TrestleMetricsReporter;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.typesafe.config.ConfigFactory;
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

    private Metrician metrician;
    private TrestleMetricsReporter reporter;
    private static Injector injector;

    @BeforeAll
    public static void staticInit() {
        ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf"));
        injector = Guice.createInjector(new MetricsModule());
    }

    @BeforeEach
    public void setup() {
        metrician = injector.getInstance(Metrician.class);
        reporter = metrician.getReporter();
    }

    @AfterEach
    public void teardown() {
        metrician.shutdown(new File("./target/metricsTest.csv"));
    }

    @Test
    public void testSimpleClass() throws InterruptedException {
        final TestMetricsClass metricsClass = new TestMetricsClass();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        reporter.report();
        metricsClass.testMeter();
        assertAll(() -> assertEquals(63, metrician.getRegistry().getGauges().size(), "Should have gauges"),
                () -> assertEquals(1, metrician.getRegistry().getMeters().size(), "Should have meters"),
                () -> assertEquals(1, metrician.getRegistry().getCounters().size(), "Should have timers"),
                () -> assertEquals(2, metrician.getRegistry().counter("com.nickrobison.metrician.TrestleReporterTest$TestMetricsClass.test-reporter-counter").getCount(), "Count should be 2"));
        Thread.sleep(500);
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        metricsClass.testIncrement();
        Thread.sleep(1000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(2000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(1000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(5000);
        reporter.report();
        metricsClass.testIncrement();
        Thread.sleep(2000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        Thread.sleep(3000);
        reporter.report();
        Thread.sleep(1000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(4000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        Thread.sleep(5000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testIncrement();
        Thread.sleep(2000);
        reporter.report();
        assertEquals(17, metrician.getRegistry().counter("com.nickrobison.metrician.TrestleReporterTest$TestMetricsClass.test-reporter-counter").getCount(), "Should be incremented correctly");
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

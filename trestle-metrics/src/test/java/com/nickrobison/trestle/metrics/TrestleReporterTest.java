package com.nickrobison.trestle.metrics;

import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 3/20/17.
 */
public class TrestleReporterTest {

    private TrestleMetrician trestleMetrician;
    private TrestleMetricsReporter reporter;

    @BeforeEach
    public void setup() {
        trestleMetrician = new TrestleMetrician();
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
        assertEquals(2, trestleMetrician.getRegistry().counter("com.nickrobison.trestle.metrics.TrestleReporterTest$TestMetricsClass.test-reporter-counter").getCount(), "Count should be 2");
        Thread.sleep(500);
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        reporter.report();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
        metricsClass.testMeter();
        metricsClass.testMeter();
        metricsClass.testMeter();
        Thread.sleep(10000);
        reporter.report();
    }


    @Metriced
    private class TestMetricsClass {

        TestMetricsClass() {}


        @Gauge
        int testReporterGauge() {
            return 7;
        }

        @CounterIncrement(name = "test-reporter-counter")
        void testIncrement() { }

        @Metered
        void testMeter() { }
    }
}

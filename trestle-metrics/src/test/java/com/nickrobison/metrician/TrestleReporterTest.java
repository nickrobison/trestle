package com.nickrobison.metrician;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 3/20/17.
 */
@SuppressWarnings({"initialization.fields.uninitialized"})
public class TrestleReporterTest {

    private Metrician metrician;
    private MetricianReporter reporter;
    private static Injector injector;

    @BeforeAll
    public static void staticInit() {
        ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf"));
        injector = Guice.createInjector(new MetricianModule());
    }

    @AfterEach
    public void teardown() {
        metrician.shutdown(new File("./target/metricsTest.csv"));
    }

    @Test
    public void testSimpleClass() throws InterruptedException {
        metrician = injector.getInstance(Metrician.class);
        reporter = metrician.getReporter();
        final TestMetricsReporterClass metricsClass = new TestMetricsReporterClass();
        metricsClass.testIncrement();
        metricsClass.testIncrement();
        reporter.report();
        metricsClass.testMeter();
        assertAll(() -> assertEquals(65, metrician.getRegistry().getGauges().size(), "Should have gauges"),
                () -> assertEquals(1, metrician.getRegistry().getMeters().size(), "Should have meters"),
                () -> assertEquals(1, metrician.getRegistry().getCounters().size(), "Should have timers"),
                () -> assertEquals(2, metrician.getRegistry().counter("com.nickrobison.metrician.TestMetricsReporterClass.test-reporter-counter").getCount(), "Count should be 2"));
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
        assertEquals(17, metrician.getRegistry().counter("com.nickrobison.metrician.TestMetricsReporterClass.test-reporter-counter").getCount(), "Should be incremented correctly");
    }

    @Test
    public void testNoOp() throws InterruptedException {
        metrician = injector.getInstance(MetricianNoop.class);
        reporter = metrician.getReporter();
        final Counter counter = metrician.registerCounter("test-counter");
        counter.inc();
        counter.inc();
        counter.inc(5);
        counter.dec(2);
        assertEquals(0, counter.getCount(), "Should have not increments");
        final Timer timer = metrician.registerTimer("noop-timer");
        final Timer.Context time = timer.time();
        Thread.sleep(100);
        time.stop();
        assertAll(() -> assertEquals(0, timer.getCount(), "Should never be called"),
                () -> assertEquals(0, timer.getSnapshot().get75thPercentile(), "Should have 0 snapshot"));
    }
}

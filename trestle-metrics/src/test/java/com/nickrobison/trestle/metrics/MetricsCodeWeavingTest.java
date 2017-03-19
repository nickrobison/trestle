package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 3/17/17.
 */
public class MetricsCodeWeavingTest {

    private MetricRegistry registry;

    @BeforeEach
    public void setup() {
        registry = SharedMetricRegistries.getOrCreate("trestle-registry");
    }

    @AfterEach
    public void reset() {
        SharedMetricRegistries.clear();
    }

    @Test
    public void testMeters() {
        assertEquals(0, registry.getMeters().size(), "Should have no meters");
        final TestClass testClass = new TestClass();
        assertEquals(1, registry.getMeters().size(), "Should have 1 meter");
        testClass.testMeter();
        testClass.testMeter();
        assertEquals(2, registry.meter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.testMeter").getCount(), "Should be executed twice");

    }

    @Test
    public void testTimer() {
        assertEquals(0, registry.getGauges().size(), "Should have no gauges");
        final TestClass testClass = new TestClass();
        assertEquals(1, registry.getGauges().size(), "Should have a single gauge");
        assertEquals(42, registry.gauge("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.gauge-test", null).getValue(), "Gauge should read 1");


    }

    @Test
    public void staticTest() {
        //        Call static method
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        assertAll(() -> assertEquals(3, registry.meter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestStaticClass.testMeter").getCount(), "Should have 3 static executions"),
                () -> assertEquals(7, registry.gauge("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestStaticClass.gauge-test-static", null).getValue(), "Should have 7 from static class"));;
    }


    @Metriced
    class TestClass {


        @Metered
        public void testMeter() {
        }

        @Gauge(name = "gauge-test")
        int testGauge() {
            return 42;
        }
    }

    @Metriced
    private static class TestStaticClass {

        @Metered
        private static void testMeter() {
        }

        @Gauge(name = "gauge-test-static")
        static int testGauge() {
            return 7;
        }
    }
}

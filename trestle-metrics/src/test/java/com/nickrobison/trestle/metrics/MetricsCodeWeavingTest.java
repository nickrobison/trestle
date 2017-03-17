package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 3/17/17.
 */
public class MetricsCodeWeavingTest {

    private static MetricRegistry registry;

    @BeforeAll
    public static void setup() {
        registry = SharedMetricRegistries.getOrCreate("trestle-registry");

    }

    @Test
    public void testMeters() {
        assertEquals(0, registry.getMeters().size(), "Should have no meters");
        final TestClass testClass = new TestClass();
        assertEquals(1, registry.getMeters().size(), "Should have 1 meter");
        testClass.testMeter();
        testClass.testMeter();
        assertEquals(2, registry.meter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.testMeter").getCount(), "Should be executed twice");
//        Call static method
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        assertAll(() -> assertEquals(3, registry.meter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestStaticClass.testMeter").getCount(), "Should have 3 static executions"),
                () -> assertEquals(2, registry.meter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.testMeter").getCount(), "Should be executed twice"));

    }




    @Metriced
    class TestClass {


        @Metered
        public void testMeter() {
        }
    }

    @Metriced
    private static class TestStaticClass {

        @Metered
        private static void testMeter() {

        }
    }
}

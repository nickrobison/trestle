package com.nickrobison.metrician;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.metrician.agent.MetricianAgentBuilder;
import com.nickrobison.metrician.instrumentation.MetricianInventory;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 3/17/17.
 */
@SuppressWarnings({"initialization.fields.uninitialized", "argument.type.incompatible"})
public class MetricsCodeWeavingTest {

    private MetricRegistry registry;

    @BeforeAll
    public static void setupBB() {
        ByteBuddyAgent.install();
        MetricianAgentBuilder.BuildAgent().installOnByteBuddyAgent();
    }

    @BeforeEach
    public void setup() {
        registry = SharedMetricRegistries.getOrCreate("trestle-registry");
    }

    @AfterEach
    public void reset() {
//        Need to clear the registry before resetting it.
        SharedMetricRegistries.clear();
        MetricianInventory.reset();
    }

    @Test
    public void testMeters() {
        assertEquals(0, registry.getMeters().size(), "Should have no meters");
        final MetricsTestClass testClass = new MetricsTestClass();
        testClass.testMeter();
        testClass.testMeter();
        assertAll(() -> assertEquals(1, registry.getMeters().size(), "Should have 1 meter"),
                () -> assertEquals(2, registry.meter("com.nickrobison.metrician.MetricsTestClass.testMeter").getCount(), "Should be executed twice"));

//        Exception meters
        assertAll(() -> assertThrows(RuntimeException.class, testClass::throwRuntimeException),
                () -> assertEquals(1, registry.meter("com.nickrobison.metrician.MetricsTestClass.throwRuntimeException.exceptions").getCount(), "Should have one caught Runtime Exception"));

        assertAll(() -> assertThrows(RuntimeException.class, testClass::throwInheritedException),
                () -> assertEquals(1, registry.meter("com.nickrobison.metrician.MetricsTestClass.inherit-exception").getCount(), "Should have one caught Runtime Exception"));
        assertAll(() -> assertThrows(RuntimeException.class, testClass::throwResourceException),
                () -> assertEquals(0, registry.meter("inherit-illegal").getCount(), "Should have no marked exceptions"));

    }

    @Test
    public void testGauge() {
        assertEquals(0, registry.getGauges().size(), "Should have no gauges");
        final MetricsTestClass testClass = new MetricsTestClass();
        assertEquals(1, registry.getGauges().size(), "Should have a single gauge");
        assertEquals(42, registry.gauge("gauge-test", null).getValue(), "Gauge should read 42");
    }

    @Test
    public void testTimer() {
        assertEquals(0, registry.getTimers().size(), "Should have no timers");
        final MetricsTestClass testClass = new MetricsTestClass();
        testClass.testTimer();
        assertAll(() -> assertEquals(1, registry.getTimers().size(), "Should have a single timer"),
                () -> assertEquals(1, registry.timer("com.nickrobison.metrician.MetricsTestClass.testTimer", null).getCount(), "Timer should have 1 count"));
    }

    @Test
    public void counterTest() {
        assertEquals(0, registry.getCounters().size(), "Should have no counters");
        final MetricsTestClass testClass = new MetricsTestClass();
        testClass.testCounterInc();
        testClass.testCounterInc();
        testClass.testCounterInc();
        assertEquals(1, registry.getCounters().size(), "Should only have a single counter");
        assertEquals(6, registry.counter("test-counter").getCount(), "Count should be 6");
        testClass.testCounterDec();
        testClass.testCounterDec();
        assertEquals(4, registry.counter("test-counter").getCount(), "Count should be 4");
    }

    @Test
    public void subClassTest() {
        final MetricsTestClass testClass = new MetricsTestSubClass();
        testClass.testMeter();
        testClass.testMeter();
        assertAll(() -> assertEquals(1, registry.getMeters().size(), "Should have 1 meter"),
                () -> assertEquals(2, registry.meter("com.nickrobison.metrician.MetricsTestClass.testMeter").getCount(), "Should be executed twice"));

//        Gauges
        assertAll(() -> assertEquals(2, registry.getGauges().size(), "Should have both gauges"),
                () -> assertEquals(42, registry.gauge("gauge-test", null).getValue(), "Gauge should read 42"),
                () -> assertEquals(7, registry.gauge("com.nickrobison.metrician.MetricsTestSubClass.testSubClassGauge", null).getValue(), "Subclass Gauge should read 7"));
    }

    @Test
    public void staticTest() {
//        Meters
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        assertEquals(3, registry.meter("com.nickrobison.metrician.MetricsCodeWeavingTest$TestStaticClass.testMeter").getCount(), "Should have 3 static executions");

//        Test counters
        TestStaticClass.testDec();
        TestStaticClass.testDec();
        TestStaticClass.testInc();
        assertAll(() -> assertEquals(2, registry.getCounters().size(), "Should have 2 static counters"),
                () -> assertEquals(-20, registry.counter("com.nickrobison.metrician.MetricsCodeWeavingTest$TestStaticClass.static-test-2").getCount(), "Static decrement should be called twice"),
                () -> assertEquals(1, registry.counter("com.nickrobison.metrician.MetricsCodeWeavingTest$TestStaticClass.static-test-1").getCount(), "Static increment should be called once"));
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

        @CounterIncrement(name = "static-test-1")
        static void testInc() {
        }

        @CounterDecrement(name = "static-test-2", amount = 10)
        static void testDec() {
        }

    }
}

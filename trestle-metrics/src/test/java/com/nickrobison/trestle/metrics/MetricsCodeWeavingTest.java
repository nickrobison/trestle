package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 3/17/17.
 */
public class MetricsCodeWeavingTest {

    private MetricRegistry registry;

    @BeforeAll
    public static void setupBB() {
        ByteBuddyAgent.install();
    }

    @BeforeEach
    public void setup() {
        registry = SharedMetricRegistries.getOrCreate("trestle-registry");
        MetricianAgentBuilder.BuildAgent();
//        new AgentBuilder.Default()
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
//                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
//                .type(ElementMatchers.isAnnotatedWith(Metriced.class))
//                .transform(new MetricianTransformer())
//                .installOnByteBuddyAgent();
    }

    @AfterEach
    public void reset() {
        SharedMetricRegistries.clear();
    }

    @Test
    public void testMeters() {
        assertEquals(0, registry.getMeters().size(), "Should have no meters");
        final TestClass testClass = new TestClass();
        testClass.testMeter();
        testClass.testMeter();
        assertAll(() -> assertEquals(1, registry.getMeters().size(), "Should have 1 meter"),
                () -> assertEquals(2, registry.meter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.testMeter").getCount(), "Should be executed twice"));
    }

    @Test
    public void testGauge() {
        assertEquals(0, registry.getGauges().size(), "Should have no gauges");
        final TestClass testClass = new TestClass();
//        testClass.testGauge();
        assertEquals(1, registry.getGauges().size(), "Should have a single gauge");
        assertEquals(42, registry.gauge("gauge-test", null).getValue(), "Gauge should read 42");
    }

    @Test
    public void testTimer() {
        assertEquals(0, registry.getTimers().size(), "Should have no timers");
        final TestClass testClass = new TestClass();
        testClass.testTimer();
        assertAll(() -> assertEquals(1, registry.getTimers().size(), "Should have a single timer"),
                () -> assertEquals(1, registry.timer("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.testTimer", null).getCount(), "Timer should have 1 count"));
    }

    @Test
    public void counterTest() {
        assertEquals(0, registry.getCounters().size(), "Should have no counters");
        final TestClass testClass = new TestClass();
        testClass.testCounterInc();
        testClass.testCounterInc();
        testClass.testCounterInc();
        assertEquals(1, registry.getCounters().size(), "Should only have a single counter");
        assertEquals(6, registry.counter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.test-counter").getCount(), "Count should be 6");
        testClass.testCounterDec();
        testClass.testCounterDec();
        assertEquals(4, registry.counter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestClass.test-counter").getCount(), "Count should be 4");
    }

    @Test
    public void staticTest() {
//        Meters
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        TestStaticClass.testMeter();
        assertAll(() -> assertEquals(3, registry.meter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestStaticClass.testMeter").getCount(), "Should have 3 static executions"),
                () -> assertEquals(7, registry.gauge("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestStaticClass.gauge-test-static", null).getValue(), "Should have 7 from static class"));

//        Test counters
        TestStaticClass.testDec();
        TestStaticClass.testDec();
        TestStaticClass.testInc();
        assertAll(() -> assertEquals(2, registry.getCounters().size(), "Should have 2 static counters"),
                () -> assertEquals(-20, registry.counter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestStaticClass.static-test-2").getCount(), "Static decrement should be called twice"),
                () -> assertEquals(1, registry.counter("com.nickrobison.trestle.metrics.MetricsCodeWeavingTest$TestStaticClass.static-test-1").getCount(), "Static increment should be called once"));
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

        @CounterIncrement(name = "test-counter", amount = 2)
        void testCounterInc() {}

        @CounterDecrement(name = "test-counter")
        void testCounterDec() {}

        @Timed
        void testTimer() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

        @CounterIncrement(name = "static-test-1")
        static void testInc() {};
        @CounterDecrement(name = "static-test-2", amount = 10)
        static void testDec() {};

    }
}

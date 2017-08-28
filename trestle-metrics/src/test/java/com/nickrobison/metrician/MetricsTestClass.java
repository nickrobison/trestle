package com.nickrobison.metrician;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterDecrement;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;

import java.util.MissingResourceException;

@Metriced
class MetricsTestClass {

    @Metered
    public void testMeter() {
    }

    @Gauge(name = "gauge-test", absolute = true)
    int testGauge() {
        return 42;
    }

    @CounterIncrement(name = "test-counter", amount = 2, absolute = true)
    void testCounterInc() {}

    @CounterDecrement(name = "test-counter", absolute = true)
    void testCounterDec() {}

    @Timed
    void testTimer() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @ExceptionMetered(cause = RuntimeException.class)
    void throwRuntimeException() {
        throw new RuntimeException("Test exception");
    }

    @ExceptionMetered(name = "inherit-exception", cause = RuntimeException.class)
    void throwInheritedException() {
        throw new IllegalArgumentException("Test argument exception");
    }

    @ExceptionMetered(name = "inherit-illegal", absolute = true, cause = IllegalArgumentException.class)
    void throwResourceException() {
        throw new MissingResourceException("Test missing resource", "test", "test");
    }
}

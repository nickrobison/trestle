package com.nickrobison.metrician;

import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;

@Metriced
class TestMetricsReporterClass {

    TestMetricsReporterClass() {
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

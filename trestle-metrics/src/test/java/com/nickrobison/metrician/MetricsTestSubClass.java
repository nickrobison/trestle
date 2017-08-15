package com.nickrobison.metrician;

import com.codahale.metrics.annotation.Gauge;

class MetricsTestSubClass extends MetricsTestClass {

    @Gauge
    int testSubClassGauge() {
        return 7;
    }

}

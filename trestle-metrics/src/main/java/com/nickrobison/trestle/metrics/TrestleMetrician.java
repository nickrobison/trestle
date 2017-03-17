package com.nickrobison.trestle.metrics;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Created by nrobison on 3/16/17.
 */
public class TrestleMetrician {


    private final MetricRegistry registry;
    private final JmxReporter jmxReporter;

    public TrestleMetrician() {
        registry = SharedMetricRegistries.getOrCreate("trestle-registry");
        jmxReporter = JmxReporter.forRegistry(registry).build();
        jmxReporter.start();
    }

    public void shutdown() {
        jmxReporter.close();
    }
}

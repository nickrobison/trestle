package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Created by nrobison on 3/17/17.
 */
public class DefaultMetricsStrategy implements MetricsRegistry {
    @Override
    public MetricRegistry resolveMetricRegistry(String registry) {
        return SharedMetricRegistries.getOrCreate(registry);
    }

    @Override
    public String resolveMetricName(String name) {
        return name;
    }
}

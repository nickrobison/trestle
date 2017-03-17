package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricRegistry;

/**
 * Created by nrobison on 3/17/17.
 */
public interface MetricsRegistry {

    MetricRegistry resolveMetricRegistry(String registry);

    String resolveMetricName(String name);
}

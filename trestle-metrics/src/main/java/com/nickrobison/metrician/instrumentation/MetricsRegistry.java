package com.nickrobison.metrician.instrumentation;

import com.codahale.metrics.MetricRegistry;

/**
 * Created by nrobison on 3/17/17.
 */

/**
 * Wrapper interface for {@link MetricsRegistry}
 * Eventually, can be used to add support for Java EE annotations
 * Currently only implemented by {@link DefaultMetricsStrategy}
 */
public interface MetricsRegistry {

    MetricRegistry resolveMetricRegistry(String registry);

    String resolveMetricName(String name);
}

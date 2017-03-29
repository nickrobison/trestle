package com.nickrobison.trestle.metrics;

import com.codahale.metrics.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nrobison on 3/27/17.
 */
public class MetricianInventory {

    public static final DefaultMetricsStrategy strategy = new DefaultMetricsStrategy();
    public static final MetricRegistry registry = strategy.resolveMetricRegistry("trestle-registry");
    public static final Map<String, AnnotatedMetric<Gauge>> gauges = new ConcurrentHashMap<>();
    public static final Map<String, AnnotatedMetric<Meter>> meters = new ConcurrentHashMap<>();
    public static final Map<String, AnnotatedMetric<Timer>> timers = new ConcurrentHashMap<>();
    public static final Map<String, AnnotatedMetric<Counter>> counters = new ConcurrentHashMap<>();
}

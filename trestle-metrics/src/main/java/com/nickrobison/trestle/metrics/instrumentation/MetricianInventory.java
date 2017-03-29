package com.nickrobison.trestle.metrics.instrumentation;

import com.codahale.metrics.*;
import com.nickrobison.trestle.metrics.AnnotatedMetric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nrobison on 3/27/17.
 */
public class MetricianInventory {

    public static DefaultMetricsStrategy strategy;
    public static MetricRegistry registry;
    public static Map<String, AnnotatedMetric<Gauge>> gauges;
    public static Map<String, AnnotatedMetric<Meter>> meters;
    public static Map<String, AnnotatedMetric<Timer>> timers;
    public static Map<String, AnnotatedMetric<Counter>> counters;

    static {
        reset();
    }

    static void reset() {
        strategy = new DefaultMetricsStrategy();
        registry = strategy.resolveMetricRegistry("trestle-registry");
        gauges = new ConcurrentHashMap<>();
        meters = new ConcurrentHashMap<>();
        timers = new ConcurrentHashMap<>();
        counters = new ConcurrentHashMap<>();
    }
}

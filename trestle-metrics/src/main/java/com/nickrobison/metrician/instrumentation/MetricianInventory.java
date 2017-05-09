package com.nickrobison.metrician.instrumentation;

import com.codahale.metrics.*;
import com.nickrobison.metrician.AnnotatedMetric;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nrobison on 3/27/17.
 */

/**
 * Shared data class which provides {@link Map} implementation for the various {@link Metric}s registered on the Java classes
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

    @EnsuresNonNull(value = {"strategy", "registry", "gauges", "meters", "timers", "counters"})
    public static void reset() {
        strategy = new DefaultMetricsStrategy();
        registry = strategy.resolveMetricRegistry("trestle-registry");
        gauges = new ConcurrentHashMap<>();
        meters = new ConcurrentHashMap<>();
        timers = new ConcurrentHashMap<>();
        counters = new ConcurrentHashMap<>();
    }
}

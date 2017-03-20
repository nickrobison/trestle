package com.nickrobison.trestle.metrics;

import com.codahale.metrics.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Created by nrobison on 3/20/17.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class MetricsTagger implements MetricRegistryListener {

    static final String METRIC_TYPE_COUNTER = "counters";
    static final String METRIC_TYPE_GAUGE = "gauges";

    private final Optional<String> prefix;
    private final Map<String, String> globalTags;
    private final Map<String, Map<String, String>> perMetricTags;
    private final Collection<RegexContainer<Map<String, String>>> regexTags;
    private final boolean enableTagComposition;
    private final MetricFilter metricFilter;
    private final MetricsDecomposer decomposer;

    MetricsTagger(Optional<String> prefix,
                  Map<String, String> globalTags,
                  Map<String, Map<String, String>> perMetricTags,
                  Collection<RegexContainer<Map<String, String>>> regexTags,
                  boolean enableTagComposition,
                  MetricsDecomposer decomposer,
                  MetricRegistry registry,
                  MetricFilter metricFilter) {
        this.prefix = prefix;
        this.globalTags = globalTags;
        this.perMetricTags = perMetricTags;
        this.regexTags = regexTags;
        this.enableTagComposition = enableTagComposition;
        this.metricFilter = metricFilter;
        this.decomposer = decomposer;

//        Initialize
        registry.getGauges().forEach(this::onGaugeAdded);
        registry.getCounters().forEach(this::onCounterAdded);
        registry.getHistograms().forEach(this::onHistogramAdded);
        registry.getTimers().forEach(this::onTimerAdded);
        registry.getMeters().forEach(this::onMeterAdded);

        registry.addListener(this);
    }
//
//    private void tagMetric(String metricType, String baseName) {
//
//    }

    @Override
    public void onGaugeAdded(String name, Gauge<?> gauge) {
//        if (metricFilter.matches(name, gauge)) {
//
//        }
    }

    @Override
    public void onGaugeRemoved(String name) {

    }

    @Override
    public void onCounterAdded(String name, Counter counter) {

    }

    @Override
    public void onCounterRemoved(String name) {

    }

    @Override
    public void onHistogramAdded(String name, Histogram histogram) {

    }

    @Override
    public void onHistogramRemoved(String name) {

    }

    @Override
    public void onMeterAdded(String name, Meter meter) {

    }

    @Override
    public void onMeterRemoved(String name) {

    }

    @Override
    public void onTimerAdded(String name, Timer timer) {

    }

    @Override
    public void onTimerRemoved(String name) {

    }
}

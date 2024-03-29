package com.nickrobison.metrician;

import com.codahale.metrics.*;
import com.nickrobison.metrician.backends.IMetricianBackend;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Created by nrobison on 3/20/17.
 */

/**
 * Component which listens to events from the {@link MetricRegistry}, and registers/unregisters them with the {@link IMetricianBackend}
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class MetricsListener implements MetricRegistryListener {

    static final String METRIC_TYPE_COUNTER = "counters";
    static final String METRIC_TYPE_GAUGE = "gauges";

    private final Optional<String> prefix;
    private final Map<String, String> globalTags;
    private final Map<String, Map<String, String>> perMetricTags;
    private final Collection<RegexContainer<Map<String, String>>> regexTags;
    private final boolean enableTagComposition;
    private final MetricFilter metricFilter;
    private final MetricsDecomposer decomposer;
    private final IMetricianBackend metricsBackend;

    MetricsListener(Optional<String> prefix,
                    Map<String, String> globalTags,
                    Map<String, Map<String, String>> perMetricTags,
                    Collection<RegexContainer<Map<String, String>>> regexTags,
                    boolean enableTagComposition,
                    MetricsDecomposer decomposer,
                    MetricRegistry registry,
                    MetricFilter metricFilter,
                    IMetricianBackend metricsBackend) {
        this.prefix = prefix;
        this.globalTags = globalTags;
        this.perMetricTags = perMetricTags;
        this.regexTags = regexTags;
        this.enableTagComposition = enableTagComposition;
        this.metricFilter = metricFilter;
        this.decomposer = decomposer;
        this.metricsBackend = metricsBackend;

        initializeMetrics(registry);
    }

    @SuppressWarnings({"methodref.receiver.bound.invalid", "argument.type.incompatible"})
    private void initializeMetrics(@UnderInitialization(MetricsListener.class)MetricsListener this, MetricRegistry registry) {
        //        Initialize
        registry.getGauges().forEach(this::onGaugeAdded);
        registry.getCounters().forEach(this::onCounterAdded);
        registry.getHistograms().forEach(this::onHistogramAdded);
        registry.getTimers().forEach(this::onTimerAdded);
        registry.getMeters().forEach(this::onMeterAdded);

        registry.addListener(this);
    }

    @Override
    public void onGaugeAdded(String name, Gauge<?> gauge) {
        if (metricFilter.matches(name, gauge)) {
            this.metricsBackend.registerGauge(name, gauge);
        }
    }

    @Override
    public void onGaugeRemoved(String name) {
//        We don't support removing metrics, yet
    }

    @Override
    public void onCounterAdded(String name, Counter counter) {
        if (metricFilter.matches(name, counter)) {
            this.metricsBackend.registerCounter(name, counter);
        }
    }

    @Override
    public void onCounterRemoved(String name) {
        //        We don't support removing metrics, yet
    }

    @Override
    public void onHistogramAdded(String name, Histogram histogram) {
        if (metricFilter.matches(name, histogram)) {
            final MetricsDecomposer.PartsStreamer streamer = decomposer.streamParts(name);
            streamer.countings().forEach(metricPart -> this.metricsBackend.registerCounter(metricPart.getMetricNameWithSuffix(name), null));
            streamer.samplings().forEach(metricPart -> this.metricsBackend.registerGauge(metricPart.getMetricNameWithSuffix(name), null));
        }
    }

    @Override
    public void onHistogramRemoved(String name) {
        //        We don't support removing metrics, yet
    }

    @Override
    public void onMeterAdded(String name, Meter meter) {
        if (metricFilter.matches(name, meter)) {
            final MetricsDecomposer.PartsStreamer streamer = decomposer.streamParts(name);
            streamer.countings().forEach(metricPart -> this.metricsBackend.registerCounter(metricPart.getMetricNameWithSuffix(name), null));
            streamer.metered().forEach(metricPart -> this.metricsBackend.registerGauge(metricPart.getMetricNameWithSuffix(name), null));
        }
    }

    @Override
    public void onMeterRemoved(String name) {
        //        We don't support removing metrics, yet
    }

    @Override
    public void onTimerAdded(String name, Timer timer) {
        if (metricFilter.matches(name, timer)) {
            final MetricsDecomposer.PartsStreamer streamer = decomposer.streamParts(name);
            streamer.countings().forEach(metricPart -> this.metricsBackend.registerCounter(metricPart.getMetricNameWithSuffix(name), null));
            streamer.metered().forEach(metricPart -> this.metricsBackend.registerGauge(metricPart.getMetricNameWithSuffix(name), null));
            streamer.samplings().forEach(metricPart -> this.metricsBackend.registerGauge(metricPart.getMetricNameWithSuffix(name), null));
        }
    }

    @Override
    public void onTimerRemoved(String name) {
        //        We don't support removing metrics, yet
    }
}

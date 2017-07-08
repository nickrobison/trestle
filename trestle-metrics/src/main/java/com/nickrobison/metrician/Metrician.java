package com.nickrobison.metrician;

import com.codahale.metrics.*;
import com.nickrobison.metrician.backends.MetricianExportedValue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by nrobison on 4/14/17.
 */
public interface Metrician {
    void shutdown();

    void shutdown(@Nullable File exportFile);

    void exportData(File exportFile);

    List<MetricianExportedValue> exportMetrics(@Nullable List<String> metrics, Long start, @Nullable Long end);

    MetricianReporter getReporter();

    MetricRegistry getRegistry();

    JVMMetrics getJvmMetrics();

    MetricianHeader getMetricsHeader();

    Map<Long, Object> getMetricValues(String metricID, Long start, @Nullable Long end);

    /**
     * Register a {@link MetricSet} with Metrician
     * @param metricSet - {@link MetricSet} to register
     */
    void registerMetricSet(MetricSet metricSet);

    /**
     * Register a {@link Counter} with the provided absolute name
     * @param name - Absolute name to use for Counter
     * @return - {@link Counter}
     */
    Timer registerTimer(String name);

    /**
     * Register a {@link Counter} with the provided absolute name
     * @param name - Absolute name to use for counter
     * @return - {@link Counter}
     */
    Counter registerCounter(String name);

    /**
     * Register a {@link Histogram} with the provided absolute name
     * @param name - Absolute name to use for Histogram
     * @return - {@link Histogram}
     */
    Histogram registerHistogram(String name);


    /**
     * Register a {@link Meter} with the provided absolute name
     * @param name - Absolute name to use for Meter
     * @return - {@link Meter}
     */
    Meter registerMeter(String name);

    /**
     * Register a {@link Gauge} with the provided absolute name
     * @param name - Absolute name to use for Gauge
     * @param gauge - {@link Gauge}
     * @param <T> - Type of {@link Gauge}
     */
    <T> void registerGauge(String name, Gauge<T> gauge);
}

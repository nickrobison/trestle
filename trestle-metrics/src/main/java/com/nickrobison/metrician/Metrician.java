package com.nickrobison.metrician;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
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
}

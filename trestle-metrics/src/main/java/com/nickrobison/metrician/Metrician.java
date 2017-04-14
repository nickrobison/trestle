package com.nickrobison.metrician;

import com.codahale.metrics.MetricRegistry;
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
}

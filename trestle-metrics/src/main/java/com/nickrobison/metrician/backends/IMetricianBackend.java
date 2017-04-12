package com.nickrobison.metrician.backends;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by nrobison on 3/20/17.
 */

/**
 * Interface for custom Metrician backend
 */
public interface IMetricianBackend {

    /**
     * Shutdown metrics backend without exporting data
     */
    void shutdown();

    /**
     * Shutdown connection to database
     * If a {@link File} is provided, the metrics will be exported, in CSV format, before closing the connection
     * @param exportFile - Optional {@link File} to export metrics into (CSV format)
     */
    void shutdown(@Nullable File exportFile);

    /**
     * Exports all metrics into a single CSV file
     * @param file {@link File} to write into
     */
    void exportData(File file);

    /**
     * Export Metrics for a given temporal interval
     * If no metrics are provided, returns all registered metrics
     * If ending temporal isn't provided, uses {@link Long#MAX_VALUE}
     * @param metrics - Nullable List of metric names
     * @param start - Unix timestamp (in ms) of start interval (inclusive)
     * @param end - Nullable Unix timestamp (in ms) of end interval.
     * @return
     */
    List<MetricianExportedValue> exportMetrics(@Nullable List<String> metrics, Long start, @Nullable Long end);

    /**
     * Register {@link Gauge} with backend
     * The Gauge may be null, especially if the method is being called from the MetricsListener
     * @param name - Name of gauge to register
     * @param gauge - Nullable {@link Gauge} to register
     */
    void registerGauge(String name, @Nullable Gauge<?> gauge);

    /**
     * Remove {@link Gauge} from backend
     * @param name - Name of gauge to remove
     */
    void removeGauge(String name);

    /**
     * Register {@link Counter} with backend
     * The Counter may be null, especially if the method is being called from the MetricsListener
     * @param name - Name of counter to register
     * @param counter - Nullable {@link Counter} to register
     */
    void registerCounter(String name, @Nullable Counter counter);

    /**
     * Remove {@link Counter} from backend
     * @param name - Name of counter ot remove
     */
    void removeCounter(String name);

    /**
     * Get a list of decomposed metrics that are currently registered with the backend
     * @return
     */
    Map<String, Long> getDecomposedMetrics();

    /**
     * Returns the given timestamp,value pairs for a specified Metric ID, from the specified timestamp onwards
     * @param metricID - String MetricID
     * @param start - Timestamp of first value (Unix ms)
     * @param end - Nullable timestamp of ending interval (Unix ms)
     * @return - Map of Timestamp,Value pairs
     */
    Map<Long, Object> getMetricsValues(String metricID, Long start, @Nullable Long end);
}

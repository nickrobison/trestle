package com.nickrobison.trestle.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.nickrobison.trestle.metrics.backends.ITrestleMetricsBackend;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.agrona.concurrent.AbstractConcurrentArrayQueue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 3/16/17.
 */
public class TrestleMetrician {
    private static final Logger logger = LoggerFactory.getLogger(TrestleMetrician.class);

    private final MetricRegistry registry;
    private final AbstractConcurrentArrayQueue<TrestleMetricsReporter.DataAccumulator> dataQueue;
    private final TrestleMetricsReporter trestleMetricsReporter;
    private final ITrestleMetricsBackend metricsBackend;
    private final TrestleJVMMetrics jvmMetrics;

    @Inject
    public TrestleMetrician(MetricRegistry registry,
                            AbstractConcurrentArrayQueue<TrestleMetricsReporter.DataAccumulator> dataqueue,
                            ITrestleMetricsBackend backend,
                            TrestleJVMMetrics jvmMetrics) {
        logger.info("Initializing Trestle Metrician");
        final Config config = ConfigFactory.load().getConfig("trestle.metrics");
        this.registry = registry;
        this.dataQueue = dataqueue;
        metricsBackend = backend;
        final MetricsDecomposer metricsDecomposer = new MetricsDecomposer(new HashMap<>(), new ArrayList<>());
        final MetricsListener metricsListener = new MetricsListener(Optional.empty(), new HashMap<>(), new HashMap<>(), new ArrayList<>(), false, metricsDecomposer, registry, MetricFilter.ALL, this.metricsBackend);
        trestleMetricsReporter = new TrestleMetricsReporter(registry, dataQueue, Optional.empty(), metricsDecomposer, MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.jvmMetrics = jvmMetrics;
        this.trestleMetricsReporter.start(config.getLong("period"), TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        logger.info("Stopping metrics reporting");
        this.trestleMetricsReporter.stop();
        shutdown(null);
    }

    public void shutdown(@Nullable File exportFile) {
        logger.info("Shutting down Trestle Metrician");
        SharedMetricRegistries.clear();
        metricsBackend.shutdown(exportFile);
    }

    public void exportData(File exportFile) {
        logger.info("Exporting metrics data to {}", exportFile);
        metricsBackend.exportData(exportFile);
    }

    public TrestleMetricsReporter getReporter() {
        return this.trestleMetricsReporter;
    }

    public MetricRegistry getRegistry() {
        return this.registry;
    }

    public TrestleJVMMetrics getJvmMetrics() {
        return this.jvmMetrics;
    }

    public TrestleMetricsHeader getMetricsHeader() {
        return new TrestleMetricsHeader(getJvmMetrics().currentUptime(), this.registry.getMetrics());
    }
}

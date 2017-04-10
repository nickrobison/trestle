package com.nickrobison.metrician;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.annotation.Gauge;
import com.nickrobison.metrician.backends.IMetricianBackend;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 3/16/17.
 */
@Metriced
public class Metrician {
    private static final Logger logger = LoggerFactory.getLogger(Metrician.class);

    private final MetricRegistry registry;
    private final BlockingQueue<MetricianReporter.DataAccumulator> dataQueue;
    private final MetricianReporter metricianReporter;
    private final IMetricianBackend metricsBackend;
    private final JVMMetrics jvmMetrics;
    private final Config config;
    private final long updatePeriod;

    @Inject
    public Metrician(MetricRegistry registry,
                     BlockingQueue<MetricianReporter.DataAccumulator> dataqueue,
                     IMetricianBackend backend,
                     JVMMetrics jvmMetrics) {
        logger.info("Initializing Trestle Metrician");
        config = ConfigFactory.load().getConfig("trestle.metrics");
        updatePeriod = config.getLong("period");
        logger.info("Updating registry every {} ms.", updatePeriod);
        this.registry = registry;
        this.dataQueue = dataqueue;
        metricsBackend = backend;
        final MetricsDecomposer metricsDecomposer = new MetricsDecomposer(new HashMap<>(), new ArrayList<>());
        final MetricsListener metricsListener = new MetricsListener(Optional.empty(), new HashMap<>(), new HashMap<>(), new ArrayList<>(), false, metricsDecomposer, registry, MetricFilter.ALL, this.metricsBackend);
        metricianReporter = new MetricianReporter(registry, dataQueue, Optional.empty(), metricsDecomposer, MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.jvmMetrics = jvmMetrics;
        this.metricianReporter.start(updatePeriod, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        logger.info("Stopping metrics reporting");
        this.metricianReporter.stop();
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

    public MetricianReporter getReporter() {
        return this.metricianReporter;
    }

    public MetricRegistry getRegistry() {
        return this.registry;
    }

    public JVMMetrics getJvmMetrics() {
        return this.jvmMetrics;
    }

    public MetricianHeader getMetricsHeader() {
        return new MetricianHeader(getJvmMetrics().currentUptime(), getJvmMetrics().startTime(), this.updatePeriod, this.metricsBackend.getDecomposedMetrics());
    }

    public Map<Long, Object> getMetricValues(String metricID, Long start, @Nullable Long end) {
        return this.metricsBackend.getMetricsValues(metricID, start, end);
    }

    @Gauge(name = "data-queue-length")
    private int getDataQueueLength() {
        return this.dataQueue.size();
    }
}

package com.nickrobison.metrician;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Gauge;
import com.nickrobison.metrician.backends.IMetricianBackend;
import com.nickrobison.metrician.backends.MetricianExportedValue;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by nrobison on 3/16/17.
 */
@Metriced
public class MetricianImpl implements Metrician {
    private static final Logger logger = LoggerFactory.getLogger(MetricianImpl.class);

    private final MetricRegistry registry;
    private final BlockingQueue<MetricianReporter.DataAccumulator> dataQueue;
    private final MetricianReporter metricianReporter;
    private final IMetricianBackend metricsBackend;
    private final JVMMetrics jvmMetrics;
    private final long updatePeriod;

    @Inject
    public MetricianImpl(MetricRegistry registry,
                     BlockingQueue<MetricianReporter.DataAccumulator> dataqueue,
                     IMetricianBackend backend,
                     JVMMetrics jvmMetrics) {
        logger.info("Initializing Trestle Metrician");
        final Config config = ConfigFactory.load().getConfig("trestle.metrics");
        updatePeriod = config.getLong("period");
        logger.info("Updating registry every {} ms.", updatePeriod);
        this.registry = registry;
        this.dataQueue = dataqueue;
        metricsBackend = backend;
        final MetricsDecomposer metricsDecomposer = new MetricsDecomposer(new HashMap<>(), new ArrayList<>());
        metricianReporter = new MetricianReporter(registry, dataQueue, Optional.empty(), metricsDecomposer, MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.jvmMetrics = jvmMetrics;
        this.metricianReporter.start(updatePeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        logger.info("Stopping metrics reporting");
        this.metricianReporter.stop();
        shutdown(null);
    }

    @Override
    public void shutdown(@Nullable File exportFile) {
        logger.info("Shutting down Trestle Metrician");
        SharedMetricRegistries.clear();
        metricsBackend.shutdown(exportFile);
    }

    @Override
    public void exportData(File exportFile) {
        logger.info("Exporting metrics data to {}", exportFile);
        metricsBackend.exportData(exportFile);
    }

    @Override
    public List<MetricianExportedValue> exportMetrics(@Nullable List<String> metrics, Long start, @Nullable Long end) {
        return this.metricsBackend.exportMetrics(metrics, start, end);
    }

    @Override
    public MetricianReporter getReporter() {
        return this.metricianReporter;
    }

    @Override
    public MetricRegistry getRegistry() {
        return this.registry;
    }

    @Override
    public JVMMetrics getJvmMetrics() {
        return this.jvmMetrics;
    }

    @Override
    public MetricianHeader getMetricsHeader() {
        return new MetricianHeader(getJvmMetrics().currentUptime(), getJvmMetrics().startTime(), this.updatePeriod, this.metricsBackend.getDecomposedMetrics());
    }

    @Override
    public Map<Long, Object> getMetricValues(String metricID, Long start, @Nullable Long end) {
        return this.metricsBackend.getMetricsValues(metricID, start, end);
    }

    @Override
    public void registerMetricSet(MetricSet metricSet) {
        this.registry.registerAll(metricSet);
    }

    @Override
    public Timer registerTimer(String name) {
        return this.registry.timer(name);
    }

    @Override
    public Counter registerCounter(String name) {
        return this.registry.counter(name);
    }

    @Override
    public Histogram registerHistogram(String name) {
        return this.registry.histogram(name);
    }

    @Override
    public Meter registerMeter(String name) {
        return this.registry.meter(name);
    }

    @Override
    public <T> void registerGauge(String name, com.codahale.metrics.Gauge<T> gauge) {
        this.registry.register(name, gauge);
    }

    @Gauge(name = "data-queue-length")
    private int getDataQueueLength() {
        return this.dataQueue.size();
    }
}

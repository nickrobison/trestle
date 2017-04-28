package com.nickrobison.metrician;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.nickrobison.metrician.backends.MetricianExportedValue;
import com.nickrobison.metrician.instrumentation.NoOpMetrics.NoOpCounter;
import com.nickrobison.metrician.instrumentation.NoOpMetrics.NoOpHistogram;
import com.nickrobison.metrician.instrumentation.NoOpMetrics.NoOpTimer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nrobison on 4/14/17.
 */
public class MetricianNoop implements Metrician {
    private static final Logger logger = LoggerFactory.getLogger(MetricianNoop.class);

    MetricianNoop() {
        logger.warn("Running with Metrician disabled, all calls will be No-Ops");
    }
    @Override
    public void shutdown() {

    }

    @Override
    public void shutdown(@Nullable File exportFile) {

    }

    @Override
    public void exportData(File exportFile) {

    }

    @Override
    public List<MetricianExportedValue> exportMetrics(@Nullable List<String> metrics, Long start, @Nullable Long end) {
        return new ArrayList<>();
    }

    @Override
    public MetricianReporter getReporter() {
        return null;
    }

    @Override
    public MetricRegistry getRegistry() {
        return null;
    }

    @Override
    public JVMMetrics getJvmMetrics() {
        return null;
    }

    @Override
    public MetricianHeader getMetricsHeader() {
        return null;
    }

    @Override
    public Map<Long, Object> getMetricValues(String metricID, Long start, @Nullable Long end) {
        return new HashMap<>();
    }

    @Override
    public Timer registerTimer(String name) {
        logger.warn("Metrician disabled, returning No-Op Metric");
        return new NoOpTimer();
    }

    @Override
    public Counter registerCounter(String name) {
        logger.warn("Metrician disabled, returning No-Op Metric");
        return new NoOpCounter();
    }

    @Override
    public Histogram registerHistogram(String name) {
        logger.warn("Metrician disabled, returning No-Op Metric");
        return new NoOpHistogram();
    }
}

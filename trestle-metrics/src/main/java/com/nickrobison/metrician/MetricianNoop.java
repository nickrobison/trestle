package com.nickrobison.metrician;

import com.codahale.metrics.*;
import com.nickrobison.metrician.backends.MetricianExportedValue;
import com.nickrobison.metrician.instrumentation.NoOpMetrics.NoOpCounter;
import com.nickrobison.metrician.instrumentation.NoOpMetrics.NoOpHistogram;
import com.nickrobison.metrician.instrumentation.NoOpMetrics.NoOpMeter;
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
@SuppressWarnings({"return.type.incompatible"})
public class MetricianNoop implements Metrician {
    private static final Logger logger = LoggerFactory.getLogger(MetricianNoop.class);
    public static final String METRICIAN_DISABLED_RETURNING_NO_OP_METRIC = "Metrician disabled, returning No-Op Metric";

    MetricianNoop() {
        logger.info("Running with Metrician disabled, all calls will be No-Ops");
    }
    @Override
    public void shutdown() {
//        Not Implemented
    }

    @Override
    public void shutdown(@Nullable File exportFile) {
//        Not Implemented
    }

    @Override
    public void exportData(File exportFile) {
//        Not Implemented
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
        logger.trace(METRICIAN_DISABLED_RETURNING_NO_OP_METRIC);
        return new NoOpTimer();
    }

    @Override
    public Counter registerCounter(String name) {
        logger.trace(METRICIAN_DISABLED_RETURNING_NO_OP_METRIC);
        return new NoOpCounter();
    }

    @Override
    public Histogram registerHistogram(String name) {
        logger.trace(METRICIAN_DISABLED_RETURNING_NO_OP_METRIC);
        return new NoOpHistogram();
    }

    @Override
    public Meter registerMeter(String name) {
        return new NoOpMeter();
    }

    @Override
    public <T> void registerGauge(String name, Gauge<T> gauge) {
        logger.trace(METRICIAN_DISABLED_RETURNING_NO_OP_METRIC);
    }

    @Override
    public void registerMetricSet(MetricSet metricSet) {
        logger.trace("Metrician disabled, not registering MetricSet");
    }
}

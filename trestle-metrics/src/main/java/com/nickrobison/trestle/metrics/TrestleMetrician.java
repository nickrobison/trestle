package com.nickrobison.trestle.metrics;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
//    private final JmxReporter jmxReporter;
    private final TrestleMetricsReporter trestleMetricsReporter;

    public TrestleMetrician() {
        logger.info("Initializing Trestle Metrician");
        registry = SharedMetricRegistries.getOrCreate("trestle-registry");
        final MetricsDecomposer metricsDecomposer = new MetricsDecomposer(new HashMap<>(), new ArrayList<>());
        final MetricsTagger metricsTagger = new MetricsTagger(Optional.empty(), new HashMap<>(), new HashMap<>(), new ArrayList<>(), false, metricsDecomposer, registry, MetricFilter.ALL);
        trestleMetricsReporter = new TrestleMetricsReporter(registry, Optional.empty(), metricsDecomposer, MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
//        jmxReporter = JmxReporter.forRegistry(registry).build();
//        jmxReporter.start();
    }

    public void shutdown() {
        logger.info("Shutting down Trestle Metrician");
//        jmxReporter.close();
    }

    public TrestleMetricsReporter getReporter() {
        return this.trestleMetricsReporter;
    }

    public MetricRegistry getRegistry() {
        return this.registry;
    }
}

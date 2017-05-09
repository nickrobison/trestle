package com.nickrobison.metrician.backends;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.annotation.Timed;
import com.nickrobison.metrician.MetricianReporter;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * Created by nrobison on 4/2/17.
 */

/**
 * Abstract class for shipping metrics to relational database backend
 */
@Metriced
@SuppressWarnings({"argument.type.incompatible", "initialization.fields.uninitialized"})
public abstract class RDBMSBackend implements IMetricianBackend {

    private static final Logger logger = LoggerFactory.getLogger(RDBMSBackend.class);
    final int thread_wait;
    final Config config;
    final Map<String, Long> metricMap;
    final Thread eventThread;
    final String threadName;

    final BlockingQueue<MetricianReporter.DataAccumulator> dataQueue;
    protected Connection connection;

    RDBMSBackend(BlockingQueue<MetricianReporter.DataAccumulator> dataQueue, String threadName) {
        this.threadName = threadName;
        this.dataQueue = dataQueue;
        this.config = ConfigFactory.load().getConfig("trestle.metrics.backend");
        this.thread_wait = this.config.getInt("threadWait");
        this.metricMap = new HashMap<>();

        final ProcessEvents processEvents = new ProcessEvents();
        eventThread = new Thread(processEvents, this.threadName);
        eventThread.start();
    }

    @Override
    public void shutdown() {
        shutdown(null);
    }

    /**
     * Database implementation dependent call to build export query
     * @param metrics - Nullable list of metric names
     * @return - {@link PreparedStatement} to execute
     * @throws SQLException
     */
    abstract PreparedStatement getExportPreparedStatement(@Nullable List<String> metrics) throws SQLException;

    @Override
    public List<MetricianExportedValue> exportMetrics(@Nullable List<String> metrics, Long start, @Nullable Long end) {
        logger.debug("Exporting values for Metrics {} from {} to {}", metrics, start, end == null ? Long.MAX_VALUE : end);
        final List<MetricianExportedValue> values = new ArrayList<>();
        try {
            final PreparedStatement exportStatement = getExportPreparedStatement(metrics);
            exportStatement.setLong(1, start);
            if (end != null) {
                exportStatement.setLong(2, end);
            } else {
                exportStatement.setLong(2, Long.MAX_VALUE);
            }

            try (ResultSet resultSet = exportStatement.executeQuery()) {
                while (resultSet.next()) {
                    while (resultSet.next()) {
                        values.add(new MetricianExportedValue(resultSet.getString(1), resultSet.getLong(2), resultSet.getObject(3)));
                    }
                }
            }
        } catch (SQLException e1) {
            logger.error("Unable to build Postgres export Statement", e1);
        }
        logger.info("Export complete");
        return values;
    }

    /**
     * Given a {@link List} of metrics names, combine them into a String usable in an SQL "IN" clause
     * @param metrics
     * @return
     */
    static String buildMetricsInStatement(List<String> metrics) {
        final StringBuilder joinedMetrics = new StringBuilder();
        joinedMetrics.append("('");
        joinedMetrics.append(metrics.get(0));
        for (int i = 1; i < metrics.size(); i++) {
            joinedMetrics.append("', '");
            joinedMetrics.append(metrics.get(i));
        }
        joinedMetrics.append("')");
        return joinedMetrics.toString();
    }

    abstract ResultSet getMetricValueResultSet(Long metricID, Long start, @Nullable Long end) throws SQLException;

    @Override
    public Map<Long, Object> getMetricsValues(String metricID, Long start, @Nullable Long end) {
        Map<Long, Object> results = new HashMap<>();
        final Long registeredMetricID = this.metricMap.get(metricID);

        try {
            final ResultSet resultSet = getMetricValueResultSet(registeredMetricID, start, end);
            try {
                while(resultSet.next()) {
                    results.put(resultSet.getLong(1), resultSet.getObject(2));
                }
            }
            finally {
                resultSet.close();
            }
        } catch (SQLException e) {
            logger.error("Error retrieving metrics for {}", metricID, e);
        }
        return results;
    }

    abstract @Nullable Long registerMetric(String metricName);

    @Override
    public abstract void exportData(File file);

    @Override
    public void registerGauge(String name, @Nullable Gauge<?> gauge) {
        logger.debug("Registering Gauge {}", name);
        registerMetric(name);
    }

    @Override
    public void registerCounter(String name, @Nullable Counter counter) {
        logger.debug("Registering counter {}", name);
        registerMetric(name);
    }

    @Override
    public void removeGauge(String name) {

    }

    @Override
    public void removeCounter(String name) {

    }

    @Override
    public Map<String, Long> getDecomposedMetrics() {
        return this.metricMap;
    }

    abstract void insertValues(List<MetricianMetricValue> events);

    protected class ProcessEvents implements Runnable {

        @Override
        public void run() {
            while (true) {
                final MetricianReporter.DataAccumulator event;
                try {
                    event = dataQueue.take();
                    processEvent(event);
                } catch (InterruptedException e) {
                    logger.debug("Thread interrupted, draining queue");
                    ArrayDeque<MetricianReporter.DataAccumulator> remainingEvents = new ArrayDeque<>();
                    dataQueue.drainTo(remainingEvents);
                    remainingEvents.forEach(this::processEvent);
                    logger.debug("Finished draining events");
                    return;
                }
            }
        }

        private void processEvent(MetricianReporter.DataAccumulator event) {
            final long timestamp = event.getTimestamp();
            List<MetricianMetricValue> events = new ArrayList<>();
//            Counters
            event.getCounters().entrySet().forEach(entry -> {
                final String key = entry.getKey();
                logger.trace("Counter {}: {}", key, entry.getValue());
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.warn("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);

                }
                events.add(new MetricianMetricValue<>(MetricianMetricValue.ValueType.COUNTER, metricKey, timestamp, entry.getValue()));
            });
//            Gauges
            event.getGauges().entrySet().forEach(entry -> {
                final String key = entry.getKey();
                logger.trace("Gauge {}: {}", key, entry.getValue());
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.warn("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);
                }
                events.add(new MetricianMetricValue<>(MetricianMetricValue.ValueType.GAUGE, metricKey, timestamp, entry.getValue()));
            });
            insertValues(events);
        }
    }
}

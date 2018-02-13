package com.nickrobison.metrician.backends;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.nickrobison.metrician.MetricianReporter;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final int threadWait;
    final Config config;
    final Map<String, Long> metricMap;
    final Thread eventThread;
    final String threadName;

    final BlockingQueue<MetricianReporter.DataAccumulator> dataQueue;
    protected HikariDataSource ds;

    RDBMSBackend(BlockingQueue<MetricianReporter.DataAccumulator> dataQueue, String threadName) {
        this.threadName = threadName;
        this.dataQueue = dataQueue;
        this.config = ConfigFactory.load().getConfig("trestle.metrics.backend");
        this.threadWait = this.config.getInt("threadWait");
        this.metricMap = new HashMap<>();

        final ProcessEvents processEvents = new ProcessEvents();
        eventThread = new Thread(processEvents, this.threadName);
        eventThread.start();
    }

    abstract HikariDataSource setupDataSource(@UnderInitialization(RDBMSBackend.class) RDBMSBackend this);

    protected Connection getConnection(@UnknownInitialization(RDBMSBackend.class) RDBMSBackend this) {
        try {
            return this.ds.getConnection();
        } catch (SQLException e) {
            logger.error("Unable to get database connection", e);
            throw new IllegalStateException("Problem getting connection to metrics database", e);
        }
    }

    @Override
    public void shutdown() {
        eventThread.interrupt();
        this.ds.close();
        shutdown(null);
    }

    /**
     * Database implementation dependent call to build export query
     *
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
     *
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

    ResultSet getMetricValueResultSet(Long metricID, Long start, @Nullable Long end) throws SQLException {
        String exportQuery = "SELECT C.TIMESTAMP, C.VALUE FROM \n" +
                "(\n" +
                "    SELECT *\n" +
                "    FROM GAUGES\n" +
                "    UNION ALL\n" +
                "    SELECT *\n" +
                "    FROM COUNTERS\n" +
                "    ) AS C\n" +
                "WHERE C.METRICID = ? AND C.TIMESTAMP >= ? AND C.TIMESTAMP <= ? ORDER BY C.TIMESTAMP ASC;";


        try (final Connection connection = getConnection();
             CallableStatement statement = connection.prepareCall(exportQuery)) {
            statement.setLong(1, metricID);
            statement.setLong(2, start);
            if (end != null) {
                statement.setLong(3, end);
            } else {
                statement.setLong(3, Long.MAX_VALUE);
            }
            return statement.executeQuery();
        }
    }

    @Override
    public Map<Long, Object> getMetricsValues(String metricID, Long start, @Nullable Long end) {
        Map<Long, Object> results = new HashMap<>();
        final Long registeredMetricID = this.metricMap.get(metricID);

        try {
            final ResultSet resultSet = getMetricValueResultSet(registeredMetricID, start, end);
            try {
                while (resultSet.next()) {
                    results.put(resultSet.getLong(1), resultSet.getObject(2));
                }
            } finally {
                resultSet.close();
            }
        } catch (SQLException e) {
            logger.error("Error retrieving metrics for {}", metricID, e);
        }
        return results;
    }

    abstract @Nullable Long registerMetric(String metricName);

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
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    final MetricianReporter.DataAccumulator event = dataQueue.take();
                    processEvent(event);
                }
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }
            logger.debug("Thread interrupted, draining queue");
            ArrayDeque<MetricianReporter.DataAccumulator> remainingEvents = new ArrayDeque<>();
            final int drainCount = dataQueue.drainTo(remainingEvents);
            remainingEvents.forEach(this::processEvent);
            logger.debug("Finished draining {} events", drainCount);
        }

        private void processEvent(MetricianReporter.DataAccumulator event) {
            final long timestamp = event.getTimestamp();
            List<MetricianMetricValue> events = new ArrayList<>();
//            Counters
            event.getCounters().forEach((key, value) -> {
                logger.trace("Counter {}: {}", key, value);
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.debug("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);

                }
                events.add(new MetricianMetricValue<>(MetricianMetricValue.ValueType.COUNTER, metricKey, timestamp, value));
            });
//            Gauges
            event.getGauges().forEach((key, value) -> {
                logger.trace("Gauge {}: {}", key, value);
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.debug("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);
                }
                events.add(new MetricianMetricValue<>(MetricianMetricValue.ValueType.GAUGE, metricKey, timestamp, value));
            });
            insertValues(events);
        }
    }
}

package com.nickrobison.trestle.metrics.backends;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.nickrobison.trestle.metrics.TrestleMetricsReporter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.sql.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by nrobison on 3/31/17.
 */
public class PostgresBackend implements ITrestleMetricsBackend {
    private static final Logger logger = LoggerFactory.getLogger(PostgresBackend.class);
    private final BlockingQueue<TrestleMetricsReporter.DataAccumulator> dataQueue;
    private final Config config;
    private final Connection connection;
    private final Thread eventThread;
    private final Map<String, Long> metricMap;

    @Inject
    PostgresBackend(BlockingQueue<TrestleMetricsReporter.DataAccumulator> dataQueue) {
        this.dataQueue = dataQueue;
        this.config = ConfigFactory.load().getConfig("trestle.metrics.backend");
        this.metricMap = new HashMap<>();
        connection = initializeDatabase();

        final ProcessEvents processEvents = new ProcessEvents();
        eventThread = new Thread(processEvents, "postgres-event-thread");
        eventThread.start();
    }

    private Connection initializeDatabase() {
        logger.debug("Checking for tables, creating if non-existent");
        final Connection connection;
        try {
            final String connectionString = config.getString("connectionString");
            logger.debug("Connecting to {}", connectionString);
            connection = DriverManager.getConnection(connectionString, config.getString("username"), config.getString("password"));
        } catch (SQLException e) {
            throw new RuntimeException(e.getCause());
        }
        return connection;
    }

    @Override
    public void shutdown() {
        shutdown(null);
    }

    @Override
    public void shutdown(@Nullable File exportFile) {
        logger.info("Shutting down Postgres backend");
        eventThread.interrupt();
        try {
            logger.debug("Waiting {} ms for queue to drain", THREAD_WAIT_MS);
            eventThread.join(THREAD_WAIT_MS);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for event thread to finish", e);
        }
        if (exportFile != null) {
            exportData(exportFile);
        }
        logger.debug("Queue drained, terminating connection");
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("Unable to close Postgres connection", e);
        }
    }

    @Override
    public void exportData(File file) {
        logger.info("Exporting metrics to {}", file);
        try {
//            Get all the metrics
            String exportQuery = String.format("COPY (SELECT M.METRIC, C.TIMESTAMP, C.VALUE FROM METRICS AS M\n" +
                    "LEFT JOIN (\n" +
                    "    SELECT *\n" +
                    "    FROM GAUGES\n" +
                    "    UNION ALL\n" +
                    "    SELECT *\n" +
                    "    FROM COUNTERS\n" +
                    "    ) AS C\n" +
                    "ON C.METRICID = M.METRICID) TO '%s' (format CSV);", file.getAbsolutePath());
            final PreparedStatement preparedStatement = connection.prepareStatement(exportQuery);
            preparedStatement.execute();
        } catch (SQLException e) {
            logger.error("Unable to export results to {}", file, e);
        }
        logger.info("Export complete");
    }

    private Long registerMetric(String metricName) {
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement(String.format("INSERT INTO metrics.METRICS (Metric) VALUES('%s')", metricName), Statement.RETURN_GENERATED_KEYS);
            preparedStatement.executeUpdate();
            final ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                final long aLong = generatedKeys.getLong(1);
                metricMap.put(metricName, aLong);
                return aLong;
            } else {
                logger.warn("No keys returned when registering gauge {}, not inserting into map", metricName);
            }
        } catch (SQLException e) {
            logger.error("Unable to insert metric {} into table", metricName, e);
        }
        return null;
    }

    @Override
    public void registerGauge(String name, @Nullable Gauge<?> gauge) {
        logger.debug("Registering Gauge {}", name);
        registerMetric(name);
    }

    @Override
    public void removeGauge(String name) {

    }

    @Override
    public void registerCounter(String name, @Nullable Counter counter) {
        logger.debug("Registering counter {}", name);
        registerMetric(name);
    }

    @Override
    public void removeCounter(String name) {

    }

    private class ProcessEvents implements Runnable {

        @Override
        public void run() {
            while (true) {
                final TrestleMetricsReporter.DataAccumulator event;
                try {
                    event = dataQueue.take();
                    processEvent(event);
                } catch (InterruptedException e) {
                    logger.debug("Thread interrupted, draining queue");
                    ArrayDeque<TrestleMetricsReporter.DataAccumulator> remainingEvents = new ArrayDeque<>();
                    dataQueue.drainTo(remainingEvents);
                    remainingEvents.forEach(this::processEvent);
                    logger.debug("Finished draining events");
                    return;
                }
            }
        }

        private void processEvent(TrestleMetricsReporter.DataAccumulator event) {
            final long timestamp = event.getTimestamp();
            final StringBuilder sqlInsertString = new StringBuilder();
//            Counters
            event.getCounters().entrySet().forEach(entry -> {
                final String key = entry.getKey();
                logger.trace("Counter {}: {}", key, entry.getValue());
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.warn("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);
                }
                sqlInsertString.append(String.format("INSERT INTO metrics.counters VALUES('%s', %s, '%s');\n", metricKey, timestamp, entry.getValue()));
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
                sqlInsertString.append(String.format("INSERT INTO metrics.gauges VALUES('%s', %s, '%s');\n", metricKey, timestamp, entry.getValue()));
            });
            try {
                final PreparedStatement insertStatement = connection.prepareStatement(sqlInsertString.toString());
                insertStatement.execute();
            } catch (SQLException e) {
                logger.error("Unable to insert event into H2 in-memory database", e);
            }
        }
    }
}

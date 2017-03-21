package com.nickrobison.trestle.metrics.backends;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.nickrobison.trestle.metrics.TrestleMetricsReporter;
import org.agrona.concurrent.AbstractConcurrentArrayQueue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 3/20/17.
 */
public class H2MemoryBackend implements ITrestleMetricsBackend {
    private static final Logger logger = LoggerFactory.getLogger(H2MemoryBackend.class);
    private static final int THREAD_WAIT_MS = 10000;
    private final AbstractConcurrentArrayQueue<TrestleMetricsReporter.DataAccumulator> dataQueue;
    private final Thread eventThread;
    private final Connection connection;
    private final Map<String, Long> metricMap;

    public H2MemoryBackend(AbstractConcurrentArrayQueue<TrestleMetricsReporter.DataAccumulator> dataQueue) {
        logger.info("Initializing H2 backend");
        logger.warn("Not for production use");
        this.dataQueue = dataQueue;
        metricMap = new HashMap<>();
//        Connect to database
        try {
            connection = initializeDatabase();
        } catch (SQLException e) {
            logger.error("Unable to initialize H2 in-memory database", e);
            throw new RuntimeException(e.getCause());
        }

        final ProcessEvents processEvents = new ProcessEvents();
        eventThread = new Thread(processEvents, "hsqldb-event-thread");
        eventThread.start();
    }

    private Connection initializeDatabase() throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:h2:mem:trestle-metrics");
        final CallableStatement createRootTable = connection.prepareCall("CREATE TABLE metrics (MetricID IDENTITY, Metric VARCHAR(150))");
        final CallableStatement createGaugesTable = connection.prepareCall("CREATE TABLE gauges (MetricID BIGINT, Timestamp BIGINT, Value DOUBLE)");
        final CallableStatement createCounterTable = connection.prepareCall("CREATE TABLE counters (MetricID BIGINT, Timestamp BIGINT, Value BIGINT)");
        createRootTable.execute();
        createCounterTable.execute();
        createGaugesTable.execute();
        return connection;
    }

    @Override
    public void shutdown(File exportFile) {
        logger.info("Shutting down H2 backend");
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
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("Unable to close H2 in-memory database", e);
        }
    }

    @Override
    public void exportData(File file) {
        logger.info("Exporting metrics to {}", file);
        try {
//            Get all the metric names
            String exportQuery = "SELECT M.METRIC, C.TIMESTAMP, C.VALUE FROM METRICS AS M\n" +
                    "LEFT JOIN (\n" +
                    "    SELECT *\n" +
                    "    FROM GAUGES\n" +
                    "    UNION ALL\n" +
                    "    SELECT *\n" +
                    "    FROM COUNTERS\n" +
                    "    ) AS C\n" +
                    "ON C.METRICID = M.METRICID;";
            final CallableStatement callableStatement = connection.prepareCall(String.format("CALL CSVWRITE('%s', '%s')", file.toString(), exportQuery));
            callableStatement.execute();
        } catch (SQLException e) {
            logger.error("Unable to export results to {}", file, e);
        }
        logger.info("Export complete");
    }

    private Long registerMetric(String metricName) {
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement(String.format("INSERT INTO metrics (Metric) VALUES('%s')", metricName), Statement.RETURN_GENERATED_KEYS);
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
//        TODO(nrobison): We should register all the sub-gauges at init
        logger.debug("Registering Gauge {}", name);
        registerMetric(name);
    }

    @Override
    public void removeGauge(String name) {

    }

    @Override
    public void registerCounter(String name, @Nullable Counter counter) {
        //        TODO(nrobison): We should register all the sub-counters at init
        logger.debug("Registering counter {}", name);
        registerMetric(name);
    }

    @Override
    public void removeCounter(String name) {

    }

    private class ProcessEvents implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                final TrestleMetricsReporter.DataAccumulator event = dataQueue.poll();
                if (event != null) {
                    logger.debug("Backend has event");
                    processEvent(event);
                }
            }
            logger.debug("Thread interrupted, draining queue");
            dataQueue.drain(this::processEvent);
        }

        private void processEvent(TrestleMetricsReporter.DataAccumulator event) {
            final long timestamp = event.getTimestamp();
            final StringBuilder sqlInsertString = new StringBuilder();
//            Counters
            event.getCounters().entrySet().forEach(entry -> {
                final String key = entry.getKey();
                logger.debug("Counter {}: {}", key, entry.getValue());
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.warn("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);
                }
                sqlInsertString.append(String.format("INSERT INTO counters VALUES('%s', %s, '%s');\n", metricKey, timestamp, entry.getValue()));
            });
//            Gauges
            event.getGauges().entrySet().forEach(entry -> {
                final String key = entry.getKey();
                logger.debug("Gauge {}: {}", key, entry.getValue());
                Long metricKey = metricMap.get(key);
                if (metricKey == null) {
                    logger.warn("Got null key for metric {}, registering", key);
                    metricKey = registerMetric(key);
                }
                sqlInsertString.append(String.format("INSERT INTO gauges VALUES('%s', %s, '%s');\n", metricKey, timestamp, entry.getValue()));
            });
            try {
                final CallableStatement callableStatement = connection.prepareCall(sqlInsertString.toString());
                callableStatement.execute();
            } catch (SQLException e) {
                logger.error("Unable to insert event into H2 in-memory database", e);
            }
        }
    }
}

package com.nickrobison.metrician.backends;

import com.nickrobison.metrician.MetricianReporter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by nrobison on 3/31/17.
 */

/**
 * Postgres backend for Metrician Metrics
 */
public class PostgresBackend extends RDBMSBackend {
    private static final Logger logger = LoggerFactory.getLogger(PostgresBackend.class);

    @Inject
    PostgresBackend(BlockingQueue<MetricianReporter.DataAccumulator> dataQueue) {
        super(dataQueue, "postgres-event-thread");
        logger.info("Initializing Postgres Backend");
        connection = initializeDatabase();
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
    public void shutdown(@Nullable File exportFile) {
        logger.info("Shutting down Postgres backend");
        eventThread.interrupt();
        try {
            logger.debug("Waiting {} ms for queue to drain", thread_wait);
            eventThread.join(thread_wait);
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

    @Override
    public Map<Long, Object> getMetricsValues(String metricID, Long start, @Nullable Long end) {
        logger.error("Not implemented yet");
        return null;
    }

    @Override
    void insertValues(List<MetricianMetricValue> events) {
        final StringBuilder sqlInsertString = new StringBuilder();
        events.forEach(event -> {
            switch (event.getType()) {
                case COUNTER:
                    sqlInsertString.append(String.format("INSERT INTO metrics.counters VALUES('%s', %s, '%s');\n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                case GAUGE:
                    sqlInsertString.append(String.format("INSERT INTO metrics.gauges VALUES('%s', %s, '%s');\n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                default:
                    logger.error("Unable to determine metric type {}, skipping", event.getType());
                    break;
            }

            try {
                final PreparedStatement insertStatement = connection.prepareStatement(sqlInsertString.toString());
                insertStatement.execute();
            } catch (SQLException e) {
                logger.error("Unable to insert event into Postgres database", e);
            }
        });
    }

    @Override
    Long registerMetric(String metricName) {
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

}

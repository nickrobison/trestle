package com.nickrobison.metrician.backends;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.metrician.MetricianReporter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.sql.*;
import java.util.List;
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
        this.ds = setupDataSource();
        initializeDatabase();
    }

    @Override
    HikariDataSource setupDataSource() {
        final String connectionString = config.getString("connectionString");
        logger.debug("Connecting to {}", connectionString);
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(connectionString);
        hikariConfig.setUsername(config.getString("username"));
        hikariConfig.setPassword(config.getString("password"));
        hikariConfig.setAutoCommit(true);
        return new HikariDataSource(hikariConfig);
    }

//    TODO(nrobison): Implement table creation
    @SuppressWarnings({"squid:S1172"}) // Checker needs this annotated param
    private void initializeDatabase(@UnderInitialization(PostgresBackend.class) PostgresBackend this) {
        logger.debug("Checking for tables, creating if non-existent");
        logger.warn("Table creation not implemented yet");
//        final Connection connection;
//        try (final Connection connection = getConnection()){
//
//            connection = DriverManager.getConnection(connectionString, config.getString("username"), config.getString("password"));
//            connection.setAutoCommit(true);
//        } catch (SQLException e) {
//            throw new RuntimeException(e.getCause());
//        }
//        return connection;
    }

    @Override
    public void shutdown(@Nullable File exportFile) {
        logger.info("Shutting down Postgres backend");
        eventThread.interrupt();
        try {
            logger.debug("Waiting {} ms for queue to drain", threadWait);
            eventThread.join(threadWait);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for event thread to finish", e);
            Thread.currentThread().interrupt();
        }
        if (exportFile != null) {
            exportData(exportFile);
        }
        logger.debug("Queue drained, terminating connection");
    }

    @Override
    @SuppressWarnings({"squid:S1192"})
    public void exportData(File file) {
        logger.info("Exporting metrics to {}", file);
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
        try (final Connection connection = getConnection();
        final PreparedStatement preparedStatement = connection.prepareStatement(exportQuery)){
            preparedStatement.execute();
            logger.info("Export complete");
        } catch (SQLException e) {
            logger.error("Unable to export results to {}", file, e);
        }
    }

    @Override
    PreparedStatement getExportPreparedStatement(@Nullable List<String> metrics) throws SQLException {
        final String queryString;
        if (metrics != null && !metrics.isEmpty()) {
            queryString = String.format("SELECT M.METRIC, C.TIMESTAMP, C.VALUE FROM METRICS AS M\n" +
                    "LEFT JOIN (\n" +
                    "    SELECT *\n" +
                    "    FROM GAUGES\n" +
                    "    UNION ALL\n" +
                    "    SELECT *\n" +
                    "    FROM COUNTERS\n" +
                    "    ) AS C\n" +
                    "ON C.METRICID = M.METRICID WHERE M.METRIC IN %s AND C.TIMESTAMP >= ? AND C.TIMESTAMP <= ? ORDER BY M.METRIC, C.TIMESTAMP ASC;", buildMetricsInStatement(metrics));
        } else {
            queryString = "SELECT M.METRIC, C.TIMESTAMP, C.VALUE FROM METRICS AS M\n" +
                    "LEFT JOIN (\n" +
                    "    SELECT *\n" +
                    "    FROM GAUGES\n" +
                    "    UNION ALL\n" +
                    "    SELECT *\n" +
                    "    FROM COUNTERS\n" +
                    "    ) AS C\n" +
                    "ON C.METRICID = M.METRICID WHERE C.TIMESTAMP >= ? AND C.TIMESTAMP <= ? ORDER BY M.METRIC, C.TIMESTAMP ASC;";
        }

        try (final Connection connection = getConnection()){
            return connection.prepareStatement(queryString);
        }
    }

    @Override
    @Timed
    void insertValues(List<MetricianMetricValue> events) {
        final StringBuilder sqlInsertString = new StringBuilder();
        events.forEach(event -> {
            switch (event.getType()) {
                case COUNTER:
                    sqlInsertString.append(String.format("INSERT INTO metrics.counters VALUES('%s', %s, '%s');%n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                case GAUGE:
                    sqlInsertString.append(String.format("INSERT INTO metrics.gauges VALUES('%s', %s, '%s');%n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                default:
                    logger.error("Unable to determine metric type {}, skipping", event.getType());
                    break;
            }
        });
        try (final Connection connection = getConnection();
             final PreparedStatement insertStatement = connection.prepareStatement(sqlInsertString.toString())){
            insertStatement.execute();
        } catch (SQLException e) {
            logger.error("Unable to insert event into Postgres database", e);
        }
    }

    @Override
    @Nullable Long registerMetric(String metricName) {
        try (final Connection connection = getConnection();
             final PreparedStatement lookupQuery = connection.prepareStatement("SELECT metricid FROM metrics.METRICS WHERE metric = ?")){
//            See if the metric is already registered
            lookupQuery.setString(1, metricName);

            try (final ResultSet foundResults = lookupQuery.executeQuery()) {
                if (foundResults.next()) {
                    final long metricID = foundResults.getLong(1);
                    logger.debug("Metric {} already registered with ID {}", metricName, metricID);
                    metricMap.put(metricName, metricID);
                    return metricID;
                }
            } catch (SQLException e) {
                logger.error("Unable to lookup metricIDs");
            }
//            If not, register it
            try (final PreparedStatement preparedStatement = connection.prepareStatement(String.format("INSERT INTO metrics.METRICS (Metric) VALUES('%s')", metricName), Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.executeUpdate();
                try (final ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        final long metricID = generatedKeys.getLong(1);
                        logger.debug("Registering {} with key {}", metricName, metricID);
                        metricMap.put(metricName, metricID);
                        return metricID;
                    } else {
                        logger.warn("No keys returned when registering gauge {}, not inserting into map", metricName);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to insert metric {} into table", metricName, e);
        }
        return null;
    }
}

package com.nickrobison.metrician.backends;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.metrician.MetricianReporter;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by nrobison on 3/20/17.
 */

/**
 * Default in-memory implementation metrics backend using the H2 database
 */
public class H2MemoryBackend extends RDBMSBackend {
    private static final Logger logger = LoggerFactory.getLogger(H2MemoryBackend.class);

    @Inject
    H2MemoryBackend(BlockingQueue<MetricianReporter.DataAccumulator> dataQueue) {
        super(dataQueue, "h2-event-thread");
        logger.info("Initializing H2 backend");
        logger.warn("Not for production use");
//        Connect to database
        connection = initializeDatabase();
    }

    private Connection initializeDatabase(@UnderInitialization(H2MemoryBackend.class) H2MemoryBackend this) {
        logger.debug("Creating tables");
        final Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:h2:mem:");
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e.getCause());
        }
        try {
            final CallableStatement metricsCreate = connection.prepareCall("CREATE TABLE metrics (MetricID IDENTITY, Metric VARCHAR(150))");
            metricsCreate.execute();
            logger.debug("Table METRICS created");
        } catch (SQLException e) {
            logger.warn("Table METRICS already exists, truncating");
            try {
                final CallableStatement metricsTruncate = connection.prepareCall("TRUNCATE TABLE metrics");
                metricsTruncate.execute();
                logger.debug("Table METRICS truncated");
            } catch (SQLException e1) {
                throw new RuntimeException(e1.getCause());
            }
        }
        try {
            final CallableStatement gaugesCreate = connection.prepareCall("CREATE TABLE gauges (MetricID BIGINT, Timestamp BIGINT, Value DOUBLE, PRIMARY KEY (MetricID, Timestamp));");
            gaugesCreate.execute();
            logger.debug("Table GAUGES created");
        } catch (SQLException e) {
            logger.warn("Table GAUGES already exists, truncating");
            try {
                final CallableStatement gaugesTruncate = connection.prepareCall("TRUNCATE TABLE gauges");
                gaugesTruncate.execute();
                logger.debug("Table GAUGES truncated");
            } catch (SQLException e1) {
                throw new RuntimeException(e1.getCause());
            }
        }
        try {
            final CallableStatement countersCreate = connection.prepareCall("CREATE TABLE counters (MetricID BIGINT, Timestamp BIGINT, Value BIGINT, PRIMARY KEY(MetricID, Timestamp));");
            countersCreate.execute();
            logger.debug("Table COUNTERS created");
        } catch (SQLException e) {
            logger.warn("Table COUNTERS already exists, truncating");
            try {
                final CallableStatement countersTruncate = connection.prepareCall("TRUNCATE TABLE counters");
                countersTruncate.execute();
                logger.debug("Table COUNTERS truncated");
            } catch (SQLException e1) {
                throw new RuntimeException(e1.getCause());
            }
        }
        return connection;
    }

    @Override
    public void shutdown(@Nullable File exportFile) {
        logger.info("Shutting down H2 backend");
        eventThread.interrupt();
        try {
            logger.debug("Waiting {} ms for queue to drain", this.thread_wait);
            eventThread.join(this.thread_wait);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for event thread to finish", e);
        }
        if (exportFile != null) {
            exportData(exportFile);
        }
        logger.debug("Queue drained, terminating Backend");
        try {
            final CallableStatement dropEverything = connection.prepareCall("DROP ALL OBJECTS DELETE FILES ");
            dropEverything.execute();
            connection.close();
        } catch (SQLException e) {
            logger.error("Unable to close H2 in-memory database", e);
        }
    }

    @Override
    public void exportData(File file) {
        logger.info("Exporting metrics to {}", file);
        try {
//            Get all the metrics
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
        return connection.prepareStatement(queryString);
    }

    @Override
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

        final CallableStatement statement = connection.prepareCall(exportQuery);
        statement.setLong(1, metricID);
        statement.setLong(2, start);
        if (end != null) {
            statement.setLong(3, end);
        } else {
            statement.setLong(3, Long.MAX_VALUE);
        }
        return statement.executeQuery();
    }

    @Override
    @Nullable Long registerMetric(String metricName) {
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
    @Timed
    void insertValues(List<MetricianMetricValue> events) {
        final StringBuilder sqlInsertString = new StringBuilder();
        events.forEach(event -> {
            switch (event.getType()) {
                case COUNTER:
                    sqlInsertString.append(String.format("INSERT INTO counters VALUES('%s', %s, '%s');\n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                case GAUGE:
                    sqlInsertString.append(String.format("INSERT INTO gauges VALUES('%s', %s, '%s');\n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                default:
                    logger.error("Unable to determine metric type {}, skipping", event.getType());
                    break;
            }
        });
        try {
            final PreparedStatement insertStatement = connection.prepareStatement(sqlInsertString.toString());
            insertStatement.execute();
        } catch (SQLException e) {
            logger.error("Unable to insert event into H2 database", e);
        }
    }
}

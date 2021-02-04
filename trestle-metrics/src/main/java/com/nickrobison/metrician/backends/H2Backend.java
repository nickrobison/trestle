package com.nickrobison.metrician.backends;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.metrician.MetricianReporter;
import com.nickrobison.metrician.exceptions.MetricianPersistenceException;
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
 * Created by nrobison on 3/20/17.
 */

/**
 * Default in-memory implementation metrics backend using the H2 database
 */
public class H2Backend extends RDBMSBackend {
    private static final Logger logger = LoggerFactory.getLogger(H2Backend.class);

    @Inject
    H2Backend(BlockingQueue<MetricianReporter.DataAccumulator> dataQueue) {
        super(dataQueue, "h2-event-thread");
        logger.info("Initializing H2 backend");
        //        Connect to database
        ds = setupDataSource();
        initializeDatabase();
    }

    @Override
    HikariDataSource setupDataSource(@UnderInitialization(RDBMSBackend.class) H2Backend this) {
        final String connectionString = config.getString("connectionString");
        if (connectionString.contains("mem")) {
            logger.warn("In-memory backend not recommended for production use");
        }
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setAutoCommit(true);
        hikariConfig.setJdbcUrl(connectionString);
//        hikariConfig.setJdbcUrl("jdbc:h2:~/test.trestle");
        return new HikariDataSource(hikariConfig);
    }

    @SuppressWarnings({"squid:S1172"}) // We suppress this because Checker needs this annotated param
    private void initializeDatabase(@UnderInitialization(RDBMSBackend.class) H2Backend this) {
        logger.debug("Creating tables");
        try (final Connection connection = getConnection()) {
//            Create the tables
            createMetricsTable(connection);
            createGaugesTable(connection);
            createCountersTable(connection);
        } catch (SQLException e) {
            throw new MetricianPersistenceException(e);
        }
    }

    private static void createMetricsTable(Connection connection) {
        try (final CallableStatement metricsCreate = connection.prepareCall("CREATE TABLE metrics (MetricID IDENTITY, Metric VARCHAR(150))")) {
            metricsCreate.execute();
            logger.debug("Table METRICS created");
        } catch (SQLException e) {
            logger.warn("Table METRICS already exists, truncating");
            try (final CallableStatement metricsTruncate = connection.prepareCall("TRUNCATE TABLE metrics")) {
                metricsTruncate.execute();
                logger.debug("Table METRICS truncated");
            } catch (SQLException e1) {
                throw new MetricianPersistenceException(e1);
            }
        }
    }

    private static void createGaugesTable(Connection connection) {
        try (final CallableStatement gaugesCreate = connection.prepareCall("CREATE TABLE gauges (MetricID BIGINT, Timestamp BIGINT, Value DOUBLE, PRIMARY KEY (MetricID, Timestamp));")) {
            gaugesCreate.execute();
            logger.debug("Table GAUGES created");
        } catch (SQLException e) {
            logger.warn("Table GAUGES already exists, truncating");
            try (final CallableStatement gaugesTruncate = connection.prepareCall("TRUNCATE TABLE gauges")) {
                gaugesTruncate.execute();
                logger.debug("Table GAUGES truncated");
            } catch (SQLException e1) {
                throw new MetricianPersistenceException(e1);
            }
        }
    }

    private static void createCountersTable(Connection connection) {
        try (final CallableStatement countersCreate = connection.prepareCall("CREATE TABLE counters (MetricID BIGINT, Timestamp BIGINT, Value BIGINT, PRIMARY KEY(MetricID, Timestamp));")) {
            countersCreate.execute();
            logger.debug("Table COUNTERS created");
        } catch (SQLException e) {
            logger.warn("Table COUNTERS already exists, truncating");
            try (final CallableStatement countersTruncate = connection.prepareCall("TRUNCATE TABLE counters")) {
                countersTruncate.execute();
                logger.debug("Table COUNTERS truncated");
            } catch (SQLException e1) {
                throw new MetricianPersistenceException(e1);
            }
        }
    }


    @Override
    public void shutdown(@Nullable File exportFile) {
        logger.info("Shutting down H2 backend");
        eventThread.interrupt();
        try {
            logger.debug("Waiting {} ms for queue to drain", this.threadWait);
            eventThread.join(this.threadWait);
        } catch (InterruptedException e) {
            logger.error("Error while waiting for event thread to finish", e);
//                I have no idea if this is the correct fix or not, but we'll give it a shot
            Thread.currentThread().interrupt();
        }
        if (exportFile != null) {
            exportData(exportFile);
        }
        logger.debug("Queue drained, terminating Backend");
        try (final Connection connection = getConnection();
             final CallableStatement dropEverything = connection.prepareCall("DROP ALL OBJECTS DELETE FILES ")) {
            dropEverything.execute();
        } catch (SQLException e) {
            logger.error("Unable to close H2 database", e);
        }
    }

    @Override
    @SuppressWarnings({"squid:S1192"})
    public void exportData(File file) {
        logger.info("Exporting metrics to {}", file);
        //            Get all the metrics
        String exportQuery = "CALL CSVWRITE(?, 'SELECT M.METRIC, C.TIMESTAMP, C.VALUE FROM METRICS AS M" +
                " LEFT JOIN (" +
                " SELECT *" +
                " FROM GAUGES" +
                " UNION ALL" +
                " SELECT *" +
                " FROM COUNTERS" +
                " ) AS C" +
                " ON C.METRICID = M.METRICID;')";
        try (final Connection connection = getConnection();
             final CallableStatement callableStatement = connection.prepareCall(exportQuery)) {
            callableStatement.setString(1, file.toString());
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

        try (final Connection connection = getConnection()){
            return connection.prepareStatement(queryString);
        }
    }

    @Override
    @Nullable Long registerMetric(String metricName) {
        try (final Connection connection = getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(String.format("INSERT INTO metrics (Metric) VALUES('%s')", metricName.replace("'", "")), Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.executeUpdate();
            try (final ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    final long aLong = generatedKeys.getLong(1);
                    metricMap.put(metricName, aLong);
                    return aLong;
                } else {
                    logger.warn("No keys returned when registering gauge {}, not inserting into map", metricName);
                }
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
                    sqlInsertString.append(String.format("INSERT INTO counters VALUES('%s', %s, '%s');%n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                case GAUGE:
                    sqlInsertString.append(String.format("INSERT INTO gauges VALUES('%s', %s, '%s');%n", event.getKey(), event.getTimestamp(), event.getValue()));
                    break;
                default:
                    logger.error("Unable to determine metric type {}, skipping", event.getType());
                    break;
            }
        });
        try (final Connection connection = getConnection();
                final PreparedStatement insertStatement = connection.prepareStatement(sqlInsertString.toString())) {
            insertStatement.execute();
        } catch (SQLException e) {
            logger.error("Unable to insert event into H2 database", e);
        }
    }
}

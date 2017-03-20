package com.nickrobison.trestle.metrics.backends;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.nickrobison.trestle.metrics.TrestleMetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Queue;

/**
 * Created by nrobison on 3/20/17.
 */
public class H2MemoryBackend implements ITrestleMetricsBackend {
    private static final Logger logger = LoggerFactory.getLogger(H2MemoryBackend.class);
    private final Queue<TrestleMetricsReporter.DataAccumulator> dataQueue;
    private final Thread eventThread;
    private final Connection connection;

    public H2MemoryBackend(Queue<TrestleMetricsReporter.DataAccumulator> dataQueue) {
        this.dataQueue = dataQueue;
        logger.info("Initializing H2 backend");
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
        final CallableStatement tableCreateStatement = connection.prepareCall("CREATE TABLE metrics (Metric VARCHAR(255), Timestamp BIGINT, Value VARCHAR(20))");
        tableCreateStatement.execute();
        return connection;
    }

    @Override
    public void shutdown(File exportFile) {
        logger.info("Shutting down H2 backend");
//        TODO(nrobison): Drain events
        eventThread.interrupt();
        try {
            eventThread.join(10000);
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

    private void exportData(File file) {
        logger.info("Exporting metrics to {}", file);
        try {
            final CallableStatement callableStatement = connection.prepareCall(String.format("CALL CSVWRITE('%s', 'SELECT * FROM metrics')", file.toString()));
            callableStatement.execute();
        } catch (SQLException e) {
            logger.error("Unable to export results to {}", file, e);
        }
        logger.info("Export complete");
    }

    @Override
    public void registerGauge(String name, Gauge<?> gauge) {

    }

    @Override
    public void removeGauge(String name) {

    }

    @Override
    public void registerCounter(String name, Counter counter) {

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
                    final long timestamp = event.getTimestamp();
                    final StringBuilder sqlInsertString = new StringBuilder();
                    event.getCounters().entrySet().forEach(entry -> {
                        logger.debug("Counter {}: {}", entry.getKey(), entry.getValue());
                        sqlInsertString.append(String.format("INSERT INTO metrics VALUES('%s', %s, '%s');\n", entry.getKey(), timestamp, entry.getValue()));
                    });
                    event.getGauges().entrySet().forEach(entry -> {
                        logger.debug("Gauge {}: {}", entry.getKey(), entry.getValue());
                        sqlInsertString.append(String.format("INSERT INTO metrics VALUES('%s', %s, '%s');\n", entry.getKey(), timestamp, entry.getValue()));
                    });
                    try {
                        final CallableStatement callableStatement = connection.prepareCall(sqlInsertString.toString());
                        callableStatement.execute();
                    } catch (SQLException e) {
                        logger.error("Unable to insert event into H2 in-memory database", e);
                    }
                }
            }
            logger.debug("Thread interrupted, shutting down");
        }
    }
}

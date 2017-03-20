package com.nickrobison.trestle.metrics.backends;

import com.nickrobison.trestle.metrics.TrestleMetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

/**
 * Created by nrobison on 3/20/17.
 */
public class HSQLDBBackend implements ITrestleMetricsBackend {
    private static final Logger logger = LoggerFactory.getLogger(HSQLDBBackend.class);
    private final Queue<TrestleMetricsReporter.DataAccumulator> dataQueue;
    private final Thread eventThread;

    public HSQLDBBackend(Queue<TrestleMetricsReporter.DataAccumulator> dataQueue) {
        this.dataQueue = dataQueue;
        logger.info("Initializing HSQLDB backend");
        final ProcessEvents processEvents = new ProcessEvents();
        eventThread = new Thread(processEvents, "hsqldb-event-thread");
        eventThread.start();
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down HSQLDB backend");
//        TODO(nrobison): Drain events
        eventThread.interrupt();
    }

    private class ProcessEvents implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                final TrestleMetricsReporter.DataAccumulator event = dataQueue.poll();
                if (event != null) {
                    logger.debug("Backend has event");
                    event.getCounters().entrySet().forEach(entry -> logger.debug("Counter {}: {}", entry.getKey(), entry.getValue()));
                    event.getGauges().entrySet().forEach(entry -> logger.debug("Gauge {}: {}", entry.getKey(), entry.getValue()));
                }
            }
            logger.debug("Thread interrupted, shutting down");
        }
    }
}

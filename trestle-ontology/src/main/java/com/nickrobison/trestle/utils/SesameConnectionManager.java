package com.nickrobison.trestle.utils;

import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 1/16/17.
 */
@Metriced
public class SesameConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(SesameConnectionManager.class);
    private final ManyToManyConcurrentArrayQueue<RepositoryConnection> connectionQueue;
    private final Repository repository;


    /**
     * Create a new connection manager for a given {@link Repository}
     * Initialize the pool with a specific size and create some initial {@link RepositoryConnection}
     * @param repository - {@link Repository} to manage connections for
     * @param poolSize - Size of connnection pool
     * @param initialConnections - Initial connections to add to pool
     */
    public SesameConnectionManager(Repository repository, int poolSize, int initialConnections) {
        this.connectionQueue = new ManyToManyConcurrentArrayQueue<>(poolSize);
        this.repository = repository;

        if (initialConnections > poolSize) {
            throw new RuntimeException(String.format("Connection pool size is less than requested number of initial connections"));
        }

        logger.info("Initializing connection pool of size {} with {} initial connections", poolSize, initialConnections);
        for (int i = 0; i < initialConnections; i++) {
            returnConnection(createNewConnection());
        }
    }

    /**
     * Get a {@link RepositoryConnection} from the connection pool.
     * If no connections are available, a new one is created
     * @return - {@link RepositoryConnection}
     */
    @Metered(name = "sesame-connection-pool-get", absolute = true)
    public RepositoryConnection getConnection() {
        final RepositoryConnection connection = this.connectionQueue.poll();
        if (connection != null) {
            logger.debug("Retrieving connection from pool {} connections still available", getConnectionCount());
            return connection;
        }
        logger.debug("Connection pool exhausted, creating a new connection");
        return createNewConnection();
    }

    /**
     * Create  new {@link RepositoryConnection}
     * @return - Newly created {@link RepositoryConnection}
     */
    @Metered(name = "sesame-connection-pool-create", absolute = true)
    private RepositoryConnection createNewConnection() {
        logger.debug("Creating new connection to repository");
        return this.repository.getConnection();
    }

    /**
     * Returns connection to the pool
     * If the pool is full, the connection is disposed of
     * If the connection is still in a transaction, an error is thrown
     * @param connection - {@link RepositoryConnection} to return to queue
     */
    @Metered(name = "sesame-connection-pool-return", absolute = true)
    public void returnConnection(RepositoryConnection connection) {
        if (connection.isActive()) {
            logger.error("Connection still has live transaction");
        }
        if (!this.connectionQueue.offer(connection)) {
            logger.debug("Queue full, closing connection");
            connection.close();
        }
        logger.debug("Returned connection to pool, {} available", getConnectionCount());
    }

    /**
     * Get the number of currently available connections
     * @return - int of connections available in the pool
     */
    @Gauge(name = "sesame-connection-pool-available", absolute = true)
    private int getConnectionCount() {
        return this.connectionQueue.size();
    }

    /**
     * Shutdown connection pool by draining the queue and closing the connection
     * Any open connections will be rolled-back
     */
    public void shutdownPool() {
        logger.info("Draining connection pool");
        this.connectionQueue.drain(RepositoryConnection::close);
        logger.info("Connection pool shutdown");
    }
}

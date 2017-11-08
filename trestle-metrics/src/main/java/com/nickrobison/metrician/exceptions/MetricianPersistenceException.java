package com.nickrobison.metrician.exceptions;

/**
 * Exception class thrown when a fatal errors occurs trying to persist metrics to the database
 */
public class MetricianPersistenceException extends RuntimeException {

    public MetricianPersistenceException(String error) {
        super(error);
    }

    public MetricianPersistenceException(Exception e) {
        super(e);
    }
}

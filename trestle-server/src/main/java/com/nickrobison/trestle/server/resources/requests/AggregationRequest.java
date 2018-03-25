package com.nickrobison.trestle.server.resources.requests;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by nickrobison on 3/24/18.
 */
public class AggregationRequest {

    @NotEmpty
    private String dataset;
    @NotEmpty
    private String strategy;
    private String wkt;

    public AggregationRequest() {
//        Not used
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }
}

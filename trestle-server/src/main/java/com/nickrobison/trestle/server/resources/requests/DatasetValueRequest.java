package com.nickrobison.trestle.server.resources.requests;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by nickrobison on 4/7/18.
 */
public class DatasetValueRequest {

    @NotEmpty
    private String dataset;
    @NotEmpty
    private String fact;
    private Long limit;

    public DatasetValueRequest() {
//        Not used
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getFact() {
        return fact;
    }

    public void setFact(String fact) {
        this.fact = fact;
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }
}

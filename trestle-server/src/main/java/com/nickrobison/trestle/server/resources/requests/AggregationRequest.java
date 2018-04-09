package com.nickrobison.trestle.server.resources.requests;

import com.nickrobison.trestle.reasoner.engines.spatial.AggregationEngine.AggregationRestriction;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.Nonnull;

/**
 * Created by nickrobison on 3/24/18.
 */
public class AggregationRequest {

    @NotEmpty
    private String dataset;
    @NotEmpty
    private String strategy;
    @Nonnull
    private AggregationRestriction restriction;

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

    public AggregationRestriction getRestriction() {
        return restriction;
    }

    public void setRestriction(AggregationRestriction restriction) {
        this.restriction = restriction;
    }
}

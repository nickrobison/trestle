package com.nickrobison.trestle.server.resources.requests;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * Created by nickrobison on 3/24/18.
 */
public class AggregationRequest {

    @NotNull
    private AggregationRestrictionRequest restriction;
    @NotNull
    private AggregationStrategyRequest strategy;

    public AggregationRequest() {
//        Not used
    }

    public AggregationRestrictionRequest getRestriction() {
        return restriction;
    }

    public void setRestriction(AggregationRestrictionRequest restriction) {
        this.restriction = restriction;
    }

    public AggregationStrategyRequest getStrategy() {
        return strategy;
    }

    public void setStrategy(AggregationStrategyRequest strategy) {
        this.strategy = strategy;
    }

    public static class AggregationRestrictionRequest {

        @NotEmpty
        private String dataset;
        @NotEmpty
        private String property;
        @NotNull
        private Object value;

        public AggregationRestrictionRequest() {
//            Not used
        }

        public String getDataset() {
            return dataset;
        }

        public void setDataset(String dataset) {
            this.dataset = dataset;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public static class AggregationStrategyRequest {

        @NotEmpty
        private String field;
        @NotEmpty
        private String operation;
        @NotNull
        private Object value;

        public AggregationStrategyRequest() {
//            Not used
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}

package com.nickrobison.trestle.reasoner.engines.spatial.aggregation;

/**
 * Created by nickrobison on 4/16/18.
 */
public class AggregationOperation implements ITransformationOp {

    private final String property;
    private final AggregationType type;
    private final Object value;

    public AggregationOperation(String property, AggregationType type, Object value) {
        this.property = property;
        this.type = type;
        this.value = value;
    }

    @Override
    public String buildFilterString() {
        return "FILTER(" + this.property +
                ' ' +
                this.type.toString() +
                ' ' +
                this.value.toString() +
                ")";
    }
}

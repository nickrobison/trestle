package com.nickrobison.trestle.reasoner.engines.spatial.aggregation;

/**
 * Created by nickrobison on 4/16/18.
 */
public enum AggregationType {
    /*
    ==      equal to
    !=      not equal to
    >       greater than
    >=      greater than or equal to
    <       less than
    <=      less than or equal to
    */
    EQ("="),
    NEQ("!="),
    GT(">"),
    GTEQ(">="),
    LT("<"),
    LTEQ("<=");

    private String operand;

    AggregationType(String operand) {
        this.operand = operand;
    }


    @Override
    public String toString() {
        return this.operand;
    }
}

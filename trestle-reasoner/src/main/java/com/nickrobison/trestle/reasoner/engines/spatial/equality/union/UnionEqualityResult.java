package com.nickrobison.trestle.reasoner.engines.spatial.equality.union;

import com.nickrobison.trestle.types.events.TrestleEventType;

import java.util.Set;

public class UnionEqualityResult<T> {

    private final T unionObject;
    private final Set<T> unionOf;
    private final TrestleEventType type;
    private final double strength;

    public UnionEqualityResult(T unionObject, Set<T> unionOf, TrestleEventType type, double strength) {
        this.unionObject = unionObject;
        this.unionOf = unionOf;
        this.type = type;
        this.strength = strength;
    }

    public T getUnionObject() {
        return unionObject;
    }

    public Set<T> getUnionOf() {
        return unionOf;
    }

    public TrestleEventType getType() {
        return type;
    }

    public double getStrength() {
         return this.strength;
    }

    @Override
    public String toString() {
        return "UnionEqualityResult{" +
                "unionObject=" + unionObject +
                ", unionOf=" + unionOf +
                ", type=" + type +
                '}';
    }
}

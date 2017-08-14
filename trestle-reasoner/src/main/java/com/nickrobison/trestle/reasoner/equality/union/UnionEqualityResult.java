package com.nickrobison.trestle.reasoner.equality.union;

import com.nickrobison.trestle.types.events.TrestleEventType;

import java.util.Set;

public class UnionEqualityResult<T> {

    private final T unionObject;
    private final Set<T> unionOf;
    private final TrestleEventType type;

    public UnionEqualityResult(T unionObject, Set<T> unionOf, TrestleEventType type) {
        this.unionObject = unionObject;
        this.unionOf = unionOf;
        this.type = type;
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

    @Override
    public String toString() {
        return "UnionEqualityResult{" +
                "unionObject=" + unionObject +
                ", unionOf=" + unionOf +
                ", type=" + type +
                '}';
    }
}

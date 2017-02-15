package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by nrobison on 2/13/17.
 */
public class PointNode<Value> extends LeafNode<Value> {
    private final Map<FastTuple, Value> values = new HashMap<>();
    private int records = 0;

    PointNode(int leafID, FastTuple leafMetadata) {
        super(leafID, leafMetadata);
    }

    void copyInitialValues(FastTuple[] keys, Value[] vals) {
        for (int i = 0; i < keys.length; i++) {
            this.values.put(keys[i], vals[i]);
            this.records++;
        }
    }
    @Override
    public int getRecordCount() {
        return this.records;
    }

    @Override
    public boolean isSplittable() {
        return false;
    }

    @Override
    @Nullable Value getValue(String objectID, long atTime) {
        final TupleExpressionGenerator.BooleanTupleExpression eval = buildFindExpression(objectID, atTime);
        final Optional<Value> value = this.values.entrySet()
                .stream()
                .filter(entry -> eval.evaluate(entry.getKey()))
                .map(Map.Entry::getValue)
                .findAny();
        return value.orElse(null);
    }

    @Override
    LeafSplit insert(String objectID, long startTime, long endTime, Value value) {
        return insert(buildObjectKey(objectID, startTime, endTime), value);
    }

    @Override
    LeafSplit insert(FastTuple newKey, Value value) {
        if (!this.values.containsKey(newKey)) {
            this.values.put(newKey, value);
            this.records++;
        }
        return null;
    }

    @Override
    boolean delete(String objectID, long atTime) {
        final FastTuple key = buildObjectKey(objectID, atTime, atTime);
        if (this.values.containsKey(key)) {
            this.values.remove(key);
            return true;
        }
        return false;
    }

    @Override
    boolean update(String objectID, long atTime, Value value) {
        final FastTuple key = buildObjectKey(objectID, atTime, atTime);
        if (this.values.containsKey(key)) {
            this.values.replace(key, value);
            return true;
        }
        return false;
    }


}

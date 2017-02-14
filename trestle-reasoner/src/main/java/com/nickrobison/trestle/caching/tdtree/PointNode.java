package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 2/13/17.
 */
public class PointNode<Value> extends LeafNode<Value> {
    private final Map<Long, Value> values = new HashMap<>();
    private int records = 0;

    PointNode(int leafID, FastTuple leafMetadata) {
        super(leafID, leafMetadata);
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
        return this.values.get(longHashCode(objectID));
    }

    @Override
    LeafSplit insert(String objectID, long startTime, long endTime, Value value) {
        return insert(buildObjectKey(objectID, startTime, endTime), value);
    }

    @Override
    LeafSplit insert(FastTuple newKey, Value value) {
        final long key = newKey.getLong(1);
        if (!this.values.containsKey(key)) {
            this.values.put(key, value);
            this.records++;
        }
        return null;
    }
}

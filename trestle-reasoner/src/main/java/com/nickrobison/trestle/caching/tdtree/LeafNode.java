package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;

/**
 * Created by nrobison on 2/13/17.
 */
public abstract class LeafNode<Value> {

    protected final int leafID;
    protected final FastTuple leafMetadata;

    LeafNode(int leafID, FastTuple leafMetadata) {
        this.leafID = leafID;
        this.leafMetadata = leafMetadata;
    }

    public int getID() {
        return this.leafID;
    }

    public abstract int getRecordCount();

    /**
     * Retrieve a value from the Leaf that matches the given ObjectID and is valid at the specified timestamp
     * Returns null if no matching object is found
     *
     * @param objectID - ID of object to find
     * @param atTime   - Time which the object must be valid
     * @return - Nullable String value
     */
    abstract Object getValue(String objectID, long atTime);

    protected static long longHashCode(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    abstract LeafSplit insert(String objectID, long startTime, long endTime, Value value);

    abstract LeafSplit insert(FastTuple newKey, Value value);
}

package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Created by nrobison on 2/13/17.
 */
public abstract class LeafNode<Value> {

    protected static final TupleSchema splittableKeySchema = buildSplittableKeySchema();
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
     * Is this a splittable node?
     * @return - <code>true</code> if {@link SplittableNode}. <code>false</code> if {@link PointNode}.
     */
    public abstract boolean isSplittable();

    abstract LeafSplit insert(String objectID, long startTime, long endTime, Value value);

    abstract LeafSplit insert(FastTuple newKey, Value value);

    /**
     * Retrieve a value from the Leaf that matches the given ObjectID and is valid at the specified timestamp
     * Returns null if no matching object is found
     *
     * @param objectID - ID of object to find
     * @param atTime   - Time which the object must be valid
     * @return - Nullable String value
     */
    abstract @Nullable Value getValue(String objectID, long atTime);

    /**
     * Build {@link FastTuple} Object key from provided values
     * @param objectID - String objectID
     * @param startTime - Long start time
     * @param endTime - Long end time
     * @return - Object key
     */
    protected static FastTuple buildObjectKey(String objectID, long startTime, long endTime) {
        final FastTuple newKey;
        try {
            newKey = splittableKeySchema.createTuple();
            newKey.setLong(1, longHashCode(objectID));
            newKey.setLong(2, startTime);
            newKey.setLong(3, endTime);
            return newKey;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new Key tuple", e);
        }
    }

    protected static long longHashCode(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    private static TupleSchema buildSplittableKeySchema() {
        try {
            return TupleSchema.builder()
                    .addField("objectID", Long.TYPE)
                    .addField("start", Long.TYPE)
                    .addField("end", Long.TYPE)
                    .implementInterface(LeafKeySchema.class)
                    .heapMemory()
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Unable to build schema for leaf key", e);
        }
    }
}

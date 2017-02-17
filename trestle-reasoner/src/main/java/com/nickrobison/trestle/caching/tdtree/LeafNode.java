package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

import static com.nickrobison.trestle.caching.tdtree.TDTreeHelpers.longHashCode;

/**
 * Created by nrobison on 2/13/17.
 */
public abstract class LeafNode<Value> {

    static final TupleSchema splittableKeySchema = buildSplittableKeySchema();
    final int leafID;
    final String binaryID;
    final FastTuple leafMetadata;

    LeafNode(int leafID, FastTuple leafMetadata) {
        this.leafID = leafID;
        this.leafMetadata = leafMetadata;
        this.binaryID = Integer.toBinaryString(leafID);
    }

    public int getID() {
        return this.leafID;
    }

    public String getBinaryStringID() {
        return this.binaryID;
    }

    public abstract int getRecordCount();

    /**
     * Is this a splittable node?
     * @return - <code>true</code> if {@link SplittableNode}. <code>false</code> if {@link PointNode}.
     */
    public abstract boolean isSplittable();

    abstract LeafSplit insert(long objectID, long startTime, long endTime, Value value);

    abstract LeafSplit insert(FastTuple newKey, Value value);

    abstract boolean delete(String objectID, long atTime);

    abstract boolean update(String objectID, long atTime, Value value);

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
     * Dump the key/value pairs for the given leaf
     * @return Map of {@link FastTuple} and {@link Value} pairs for the node
     */
    abstract Map<FastTuple, Value> dumpLeaf();

    static FastTuple buildObjectKey(String objectID, long startTime, long endTime) {
        return buildObjectKey(longHashCode(objectID), startTime, endTime);
    }

    /**
     * Build {@link FastTuple} Object key from provided values
     * @param objectID - String objectID
     * @param startTime - Long start time
     * @param endTime - Long end time
     * @return - Object key
     */
    static FastTuple buildObjectKey(long objectID, long startTime, long endTime) {
        final FastTuple newKey;
        try {
            newKey = splittableKeySchema.createTuple();
            newKey.setLong(1, objectID);
            newKey.setLong(2, startTime);
            newKey.setLong(3, endTime);
            return newKey;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new Key tuple", e);
        }
    }

    /**
     * Build the expression to find the value valid at the given time point
     * @param objectID - ObjectID to match
     * @param atTime - Temporal to find valid value
     * @return - {@link com.boundary.tuple.codegen.TupleExpressionGenerator.BooleanTupleExpression} to evaluate against tuples
     */
    TupleExpressionGenerator.BooleanTupleExpression buildFindExpression(String objectID, long atTime) {
        try {
//            We have to do this really weird equality check because FastTuple doesn't support if statements (for now). So we check for a an interval match, then a point match
            final String queryString = String.format("(tuple.objectID == %sL) && (((tuple.start <= %sL) && (tuple.end > %sL)) | ((tuple.start == tuple.end)  && (tuple.start == %sL)))", longHashCode(objectID), atTime, atTime, atTime);
            return TupleExpressionGenerator.builder().expression(queryString).schema(splittableKeySchema).returnBoolean();
        } catch (Exception e) {
            throw new RuntimeException("Unable to build find expression", e);
        }
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

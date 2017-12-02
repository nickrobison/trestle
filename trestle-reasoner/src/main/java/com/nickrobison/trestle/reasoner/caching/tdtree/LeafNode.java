package com.nickrobison.trestle.reasoner.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

import static com.nickrobison.trestle.reasoner.caching.tdtree.TDTreeHelpers.getIDLength;
import static com.nickrobison.trestle.reasoner.caching.tdtree.TDTreeHelpers.longHashCode;

/**
 * Created by nrobison on 2/13/17.
 */

/**
 * Base class of the index leaf-nodes.
 * Both {@link SplittableLeaf} and {@link PointLeaf} inherit from this class
 *
 * @param <Value> - Generic type of Index value
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

    public double[] getLeafVerticies() {
        return TDTreeHelpers.getTriangleVerticies(TDTreeHelpers.adjustedLength[getIDLength(this.leafID)],
                this.leafMetadata.getShort(3),
                this.leafMetadata.getDouble(1),
                this.leafMetadata.getDouble(2));
    }

    public abstract String getLeafType();

    /**
     * Gets the number of records stored in the leaf
     * <em>Note:</em> removing a key/value pair does not decrement the record count, so this represents the free space in the currently allocated leaf.
     * Only a rebalance will cause the leafs to be re-compacted.
     *
     * @return - int of current record count
     */
    public abstract int getRecordCount();

    /**
     * Is this a splittable node?
     *
     * @return - <code>true</code> if {@link SplittableLeaf}. <code>false</code> if {@link PointLeaf}.
     */
    public abstract boolean isSplittable();

    /**
     * Insert a key/value pair into the node that is valid during the given interval
     *
     * @param objectID  - ObjectID of Key
     * @param startTime - Valid from time
     * @param endTime   - Valid to time
     * @param value     - {@link Value} to insert
     * @return - {@link LeafSplit} if the insert forced the leaf to split. Null value if not
     */
    abstract @Nullable LeafSplit insert(long objectID, long startTime, long endTime, @NonNull Value value);

    /**
     * Insert a {@link FastTuple} key/value pair into the leaf
     *
     * @param newKey - {@link FastTuple} key which contains the ObjectID, and valid ranges for the value
     * @param value  - {@link Value} to insert
     * @return - {@link LeafSplit} if the insert forced the leaf to split. Null value if not
     */
    abstract @Nullable LeafSplit insert(FastTuple newKey, @NonNull Value value);

    /**
     * Delete key/value pair from leaf
     *
     * @param objectID - ObjectID to match
     * @param atTime   - ValidTime to to restrict matching keys to
     * @return - <code>true</code> matching key was deleted in this leaf. <code>false</code> no keys were removed
     */
    abstract boolean delete(String objectID, long atTime);

    /**
     * Delete key/value pair from leaf
     * @param expression - Pre-compiled {@link com.boundary.tuple.codegen.TupleExpressionGenerator.BooleanTupleExpression} to evaluate
     * @return - <code>true</code> matching key was deleted in this leaf. <code>false</code> no keys were removed
     */
    abstract boolean delete(TupleExpressionGenerator.BooleanTupleExpression expression);

    /**
     * Delete all keys that point to the given value
     *
     * @param value - {@link Value} to purge from Leaf
     * @return - number of keys deleted
     */
    abstract long deleteKeysWithValue(@NonNull Value value);

    abstract boolean update(String objectID, long atTime, @NonNull Value value);

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
     * Retrieve a value from the Leaf that matches the given ObjectID and is valid at the specified timestamp
     * Returns null if no matching object is found
     * @param expression - Pre-compiled {@link com.boundary.tuple.codegen.TupleExpressionGenerator.BooleanTupleExpression} to evaluate
     * @return - Nullable {@link Value}
     */
    abstract @Nullable Value getValue(TupleExpressionGenerator.BooleanTupleExpression expression);

    /**
     * Dump the key/value pairs for the given leaf
     *
     * @return Map of {@link FastTuple} and {@link Value} pairs for the node
     */
    abstract Map<FastTuple, @NonNull Value> dumpLeaf();

    /**
     * Calculates the fragmentation of the leaf-node, which is the number of null records in the storage array with indexes less than the record count
     * @return - Percent fragmentation
     */
    abstract double calculateFragmentation();

    static FastTuple buildObjectKey(String objectID, long startTime, long endTime) {
        return buildObjectKey(longHashCode(objectID), startTime, endTime);
    }

    /**
     * Build {@link FastTuple} Object key from provided values
     *
     * @param objectID  - String objectID
     * @param startTime - Long start time
     * @param endTime   - Long end time
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
     *
     * @param objectID - ObjectID to match
     * @param atTime   - Temporal to find valid value
     * @return - {@link com.boundary.tuple.codegen.TupleExpressionGenerator.BooleanTupleExpression} to evaluate against tuples
     */
    static TupleExpressionGenerator.BooleanTupleExpression buildFindExpression(String objectID, long atTime) {
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

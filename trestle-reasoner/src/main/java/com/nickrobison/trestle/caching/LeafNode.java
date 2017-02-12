package com.nickrobison.trestle.caching;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.nickrobison.trestle.caching.TDTree.leafSchema;
import static com.nickrobison.trestle.caching.TriangleHelpers.*;

/**
 * Created by nrobison on 2/9/17.
 */
class LeafNode {
    private static final TupleSchema leafKeySchema = buildLeafKeySchema();
    private final int leafID;
    private final FastTuple leafMetadata;
    private final int blockSize;
    private final FastTuple[] keys;
    private final String[] values;
    private int records = 0;


    LeafNode(int leafID, FastTuple leafMetadata, int blockSize) {
        this.leafID = leafID;
        this.leafMetadata = leafMetadata;
        this.blockSize = blockSize;

//            Allocate Key array
        try {
            keys = leafKeySchema.createArray(blockSize);
            values = new String[blockSize];
        } catch (Exception e) {
            throw new RuntimeException("Unable to allocate key/value memory for leaf", e);
        }

    }

    public int getID() {
        return this.leafID;
    }

    public int getRecordCount() {
        return this.records;
    }


    /**
     * Retrieve a value from the Leaf that matches the given ObjectID and is valid at the specified timestamp
     * Returns null if no matching object is found
     * @param objectID - ID of object to find
     * @param atTime - Time which the object must be valid
     * @return - Nullable String value
     */
    @Nullable String getValue(String objectID, long atTime) {
        final TupleExpressionGenerator.BooleanTupleExpression eval;
        try {
//            We have to do this really weird equality check because FastTuple doesn't support if statements (for now). So we check for a an interval match, then a point match
            final String queryString = String.format("(tuple.objectID == %sL) & (((tuple.start <= %sL) & (tuple.end > %sL)) | ((tuple.start == tuple.end)  & (tuple.start == %sL)))", longHashCode(objectID), atTime, atTime, atTime);
            eval = TupleExpressionGenerator.builder().expression(queryString).schema(leafKeySchema).returnBoolean();
        } catch (Exception e) {
            throw new RuntimeException("Unable to build find expression", e);
        }
        for (int i = 0; i < this.records; i++) {
            if (eval.evaluate(keys[i])) {
                return values[i];
            }
        }
        return null;
    }

    LeafSplit insert(String objectID, long startTime, long endTime, String value) {
        final FastTuple newKey;
        try {
            newKey = leafKeySchema.createTuple();
            newKey.setLong(1, longHashCode(objectID));
            newKey.setLong(2, startTime);
            newKey.setLong(3, endTime);
            return insert(newKey, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create new Key tuple", e);
        }
    }

    LeafSplit insert (FastTuple newKey, String value) {
//            Check if we have more space, if we do, insert it.
        if (records < blockSize) {

//                Check if we have it, otherwise, add it
                if (!ArrayUtils.contains(keys, newKey)) {
                    keys[records] = newKey;
                    values[records] = value;
                    records++;
                }
                return null;
        } else {
//                If we don't have any more space, time to split
            final double parentStart = this.leafMetadata.getDouble(1);
            final double parentEnd = this.leafMetadata.getDouble(2);
            final short parentDirection = this.leafMetadata.getShort(3);
            final TriangleHelpers.TriangleApex childApex = calculateChildApex(getIDLength(this.leafID) + 1,
                    parentDirection,
                    parentStart,
                    parentEnd);
            final TriangleHelpers.ChildDirection childDirection = calculateChildDirection(parentDirection);
//            Create the lower and higher leafs
            final FastTuple lowerChild;
            final FastTuple higherChild;
            try {
                 lowerChild = leafSchema.createTuple();
                lowerChild.setDouble(1, childApex.start);
                lowerChild.setDouble(2, childApex.end);
                lowerChild.setShort(3, (short) childDirection.lowerChild);
                higherChild = leafSchema.createTuple();
                higherChild.setDouble(1, childApex.start);
                higherChild.setDouble(2, childApex.end);
                higherChild.setShort(3, (short) childDirection.higherChild);
            } catch (Exception e) {
                throw new RuntimeException("Unable to build tuples for child triangles", e);
            }

            final LeafNode lowerChildLeaf = new LeafNode(leafID << 1, lowerChild, this.blockSize);
            final LeafNode higherChildLeaf = new LeafNode((leafID << 1) | 1, higherChild, this.blockSize);
            final LeafSplit leafSplit = new LeafSplit(this.leafID, lowerChildLeaf, higherChildLeaf);
//            Divide values into children, by testing to see if they belong to the lower child
            final double[] lowerChildVerticies = getTriangleVerticies(getAdjustedLength(getIDLength(this.leafID) + 1), childDirection.lowerChild, childApex.start, childApex.end);
            for (int i = 0; i < this.blockSize; i++) {
                FastTuple key = keys[i];
                if (pointInTriangle(key.getLong(2), key.getLong(3), lowerChildVerticies)) {
                    final LeafSplit lowerChildSplit = lowerChildLeaf.insert(key, values[i]);
                    if (lowerChildSplit != null) {
                        leafSplit.lowerSplit = lowerChildSplit;
                    }
                } else {
                    final LeafSplit higherChildSplit = higherChildLeaf.insert(key, values[i]);
                    if (higherChildSplit != null) {
                        leafSplit.higherSplit = higherChildSplit;
                    }
                }
            }
//            Don't forget about the new record we're trying to insert
            if (pointInTriangle(newKey.getLong(2), newKey.getLong(3), lowerChildVerticies)) {
                final LeafSplit lowerChildSplit = lowerChildLeaf.insert(newKey, value);
                if (lowerChildSplit != null) {
                    leafSplit.lowerSplit = lowerChildSplit;
                }
            } else {
                final LeafSplit higherChildSplit = higherChildLeaf.insert(newKey, value);
                leafSplit.higherSplit = higherChildSplit;
            }
//            Zero out the records, so we know we've fully split everything
            this.records = 0;
            return leafSplit;
        }
    }


    private static TupleSchema buildLeafKeySchema() {
        try {
            return TupleSchema.builder()
                    .addField("objectID", Long.TYPE)
                    .addField("start", Long.TYPE)
                    .addField("end", Long.TYPE)
                    .heapMemory()
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Unable to build schema for leaf key", e);
        }
    }

    private static long longHashCode(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }
}

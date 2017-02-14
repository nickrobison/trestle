package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.TupleSchema;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Created by nrobison on 2/9/17.
 */
class LeafNode<Value> {
    private static final TupleSchema leafKeySchema = buildLeafKeySchema();
    private final int leafID;
    private final FastTuple leafMetadata;
    private int blockSize;
    final FastTuple[] keys;
    final Value[] values;
    private int records = 0;


    LeafNode(int leafID, FastTuple leafMetadata, int blockSize) {
        this.leafID = leafID;
        this.leafMetadata = leafMetadata;
        this.blockSize = blockSize;

//            Allocate Key array
        try {
            keys = leafKeySchema.createArray(blockSize);
            //noinspection unchecked
            values = (Value[]) new Object[blockSize];
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
     *
     * @param objectID - ID of object to find
     * @param atTime   - Time which the object must be valid
     * @return - Nullable String value
     */
    @Nullable Value getValue(String objectID, long atTime) {
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

    LeafSplit insert(String objectID, long startTime, long endTime, Value value) {
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

    LeafSplit insert(FastTuple newKey, Value value) {
//            Check if we have more space, if we do, insert it.
        if (records < blockSize) {
            return insertValueIntoArray(newKey, value);
//                Check if we have it, otherwise, add it

        } else {
//                If we don't have any more space, time to split
            final double parentStart = this.leafMetadata.getDouble(1);
            final double parentEnd = this.leafMetadata.getDouble(2);
            final short parentDirection = this.leafMetadata.getShort(3);
            final int idLength = TriangleHelpers.getIDLength(this.leafID);
            final TriangleHelpers.TriangleApex childApex = TriangleHelpers.calculateChildApex(idLength + 1,
                    parentDirection,
                    parentStart,
                    parentEnd);
            final TriangleHelpers.ChildDirection childDirection = TriangleHelpers.calculateChildDirection(parentDirection);
//            If one of the children is a point, pick the lower, turn it into a point and move on
            if (TriangleHelpers.triangleIsPoint(TriangleHelpers.getTriangleVerticies(TriangleHelpers.getAdjustedLength(idLength + 1 ), childDirection.lowerChild, childApex.start, childApex.end))) {
//                FIXME(nrobison): This is just a stop-gap, it WILL fail at some point during execution
                try {
                    final FastTuple lowerChild = TDTree.leafSchema.createTuple();
                    lowerChild.setDouble(1, childApex.start);
                    lowerChild.setDouble(2, childApex.end);
                    lowerChild.setShort(3, (short) childDirection.lowerChild);
                    final LeafNode pointLeaf = new LeafNode(leafID << 1, lowerChild, 10000);
                    for (int i = 0; i < this.blockSize; i++) {
                        pointLeaf.insert(this.keys[i], this.values[i]);
                    }
//                    Insert the new value and return the split
                    pointLeaf.insert(newKey, value);
//                    It's fine if these are duplicated, we'll sort them out later.
                    return new LeafSplit(this.leafID, pointLeaf, pointLeaf);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to create new key array for point leaf");
                }
            } else {

//            Create the lower and higher leafs
                final FastTuple lowerChild;
                final FastTuple higherChild;
                try {
                    lowerChild = TDTree.leafSchema.createTuple();
                    lowerChild.setDouble(1, childApex.start);
                    lowerChild.setDouble(2, childApex.end);
                    lowerChild.setShort(3, (short) childDirection.lowerChild);
                    higherChild = TDTree.leafSchema.createTuple();
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
                final double[] lowerChildVerticies = TriangleHelpers.getTriangleVerticies(TriangleHelpers.getAdjustedLength(idLength + 1), childDirection.lowerChild, childApex.start, childApex.end);
                for (int i = 0; i < this.blockSize; i++) {
                    FastTuple key = keys[i];
                    if (TriangleHelpers.pointInTriangle(key.getLong(2), key.getLong(3), lowerChildVerticies)) {
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
                if (TriangleHelpers.pointInTriangle(newKey.getLong(2), newKey.getLong(3), lowerChildVerticies)) {
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
    }

    private LeafSplit insertValueIntoArray(FastTuple key, Value value) {
        if (!ArrayUtils.contains(keys, key)) {
            keys[records] = key;
            values[records] = value;
            records++;
        }
        return null;
    }


    private static TupleSchema buildLeafKeySchema() {
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

    private static long longHashCode(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }
}

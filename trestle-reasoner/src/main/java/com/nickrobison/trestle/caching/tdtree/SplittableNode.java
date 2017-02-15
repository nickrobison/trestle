package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.nickrobison.trestle.caching.tdtree.TriangleHelpers.getIDLength;

/**
 * Created by nrobison on 2/9/17.
 */
class SplittableNode<Value> extends LeafNode<Value> {
    private int blockSize;
    final FastTuple[] keys;
    final Value[] values;
    private int records = 0;


    SplittableNode(int leafID, FastTuple leafMetadata, int blockSize) {
        super(leafID, leafMetadata);
        this.blockSize = blockSize;
//            Allocate Key array
        try {
            keys = splittableKeySchema.createArray(blockSize);
            //noinspection unchecked
            values = (Value[]) new Object[blockSize];
        } catch (Exception e) {
            throw new RuntimeException("Unable to allocate key/value memory for leaf", e);
        }

    }

    @Override
    public int getRecordCount() {
        return this.records;
    }

    @Override
    public boolean isSplittable() {
        return true;
    }

    @Override
    @Nullable Value getValue(String objectID, long atTime) {
        final TupleExpressionGenerator.BooleanTupleExpression eval = buildFindExpression(objectID, atTime);
        for (int i = 0; i < this.records; i++) {
            final FastTuple key = keys[i];
            if (key != null) {
                if (eval.evaluate(key)) {
                    return values[i];
                }
            }
        }
        return null;
    }

    @Override
    LeafSplit insert(String objectID, long startTime, long endTime, Value value) {
        return insert(buildObjectKey(objectID, startTime, endTime), value);
    }

    @Override
    LeafSplit insert(FastTuple newKey, Value value) {
//            Check if we have more space, if we do, insert it.
        if (records < blockSize) {
            return insertValueIntoArray(newKey, value);
        } else {
//                If we don't have any more space, time to split
            final double parentStart = this.leafMetadata.getDouble(1);
            final double parentEnd = this.leafMetadata.getDouble(2);
            final short parentDirection = this.leafMetadata.getShort(3);
            final int idLength = getIDLength(this.leafID);
            final TriangleHelpers.TriangleApex childApex = TriangleHelpers.calculateChildApex(idLength + 1,
                    parentDirection,
                    parentStart,
                    parentEnd);
            final TriangleHelpers.ChildDirection childDirection = TriangleHelpers.calculateChildDirection(parentDirection);
            final FastTuple lowerChild;
            final FastTuple higherChild;
            final LeafNode<Value> lowerChildLeaf;
            final LeafNode<Value> higherChildLeaf;
//            If one of the children is a point, pick the lower, turn it into a point and move on
            if (TriangleHelpers.triangleIsPoint(TriangleHelpers.getTriangleVerticies(TriangleHelpers.getAdjustedLength(idLength + 1), childDirection.lowerChild, childApex.start, childApex.end)) | getIDLength(this.leafID) == getIDLength(Integer.MAX_VALUE)) {
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
                    throw new RuntimeException("Unable to create new key array for point leaf");
                }

                lowerChildLeaf = new PointNode<>(leafID << 1, lowerChild);
                higherChildLeaf = new PointNode<>((leafID << 1) | 1, higherChild);
//                    Convert the leaf to a point leaf and replace the splittable node

                final PointNode<Object> pointNode = new PointNode<>(leafID, leafMetadata);
//                    Copy in all the keys and values
                pointNode.copyInitialValues(this.keys, this.values);
//                    Copy the new value
                pointNode.insert(newKey, value);

                this.records = 0;

                return new LeafSplit(leafID, pointNode, pointNode);
            } else {

//            Create the lower and higher leafs
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

                lowerChildLeaf = new SplittableNode<>(leafID << 1, lowerChild, this.blockSize);
                higherChildLeaf = new SplittableNode<>((leafID << 1) | 1, higherChild, this.blockSize);
            }
            final LeafSplit leafSplit = new LeafSplit(this.leafID, lowerChildLeaf, higherChildLeaf);
//            Divide values into children, by testing to see if they belong to the lower child
            final double[] lowerChildVerticies = TriangleHelpers.getTriangleVerticies(TriangleHelpers.getAdjustedLength(idLength + 1), childDirection.lowerChild, childApex.start, childApex.end);
            for (int i = 0; i < this.blockSize; i++) {
                FastTuple key = keys[i];
                if (key != null) {
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
            }
//            Don't forget about the new record we're trying to insert
            if (TriangleHelpers.pointInTriangle(newKey.getLong(2), newKey.getLong(3), lowerChildVerticies)) {
                final LeafSplit lowerChildSplit = lowerChildLeaf.insert(newKey, value);
                if (lowerChildSplit != null) {
                    leafSplit.lowerSplit = lowerChildSplit;
                }
            } else {
                leafSplit.higherSplit = higherChildLeaf.insert(newKey, value);
            }
//            Zero out the records, so we know we've fully split everything
            this.records = 0;
            return leafSplit;
        }
    }

    @Override
    boolean delete(String objectID, long atTime) {
        final TupleExpressionGenerator.BooleanTupleExpression eval = buildFindExpression(objectID, atTime);
        for (int i = 0; i < this.records; i++) {
            final FastTuple key = keys[i];
            if (key != null) {
                if (eval.evaluate(key)) {
                    keys[i] = null;
                    values[i] = null;
                    return true;
//                    Do we need to collapse?
                }
            }
        }
        return false;
    }

    @Override
    boolean update(String objectID, long atTime, Value value) {
        final TupleExpressionGenerator.BooleanTupleExpression eval = buildFindExpression(objectID, atTime);
        for (int i = 0; i < this.records; i++) {
            final FastTuple key = keys[i];
            if (key != null) {
                if (eval.evaluate(key)) {
                    values[i] = value;
                    return true;
                }
            }
        }
        return false;
    }

    private LeafSplit insertValueIntoArray(FastTuple key, Value value) {
        if (!ArrayUtils.contains(keys, key)) {
            keys[records] = key;
            values[records] = value;
            records++;
        }
        return null;
    }


}

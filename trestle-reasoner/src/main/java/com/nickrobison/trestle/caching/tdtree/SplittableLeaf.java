package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.nickrobison.trestle.caching.tdtree.TDTreeHelpers.getIDLength;

/**
 * Created by nrobison on 2/9/17.
 */
class SplittableLeaf<Value> extends LeafNode<Value> {
    private static final Logger logger = LoggerFactory.getLogger(SplittableLeaf.class);
    private int blockSize;
    final @Nullable FastTuple[] keys;
    final @Nullable Value[] values;
    private int records = 0;


    @SuppressWarnings({"method.invocation.invalid"})
    SplittableLeaf(int leafID, FastTuple leafMetadata, int blockSize) {
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
        logger.trace("Creating splittable leaf {}", this.getBinaryStringID());
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
        return getValue(eval);
    }

    @Override
    @Nullable Value getValue(TupleExpressionGenerator.BooleanTupleExpression expression) {
        for (int i = 0; i < this.records; i++) {
            final FastTuple key = keys[i];
            if (key != null) {
                if (expression.evaluate(key)) {
                    return values[i];
                }
            }
        }
        return null;
    }

    @Override
    @Nullable LeafSplit insert(long objectID, long startTime, long endTime, @NonNull Value value) {
        return insert(buildObjectKey(objectID, startTime, endTime), value);
    }

    @Override
//    If a key is non-null, then the value is non-null
    @SuppressWarnings({"argument.type.incompatible"})
    @Nullable LeafSplit insert(FastTuple newKey, @NonNull Value value) {
//            Check if we have more space, if we do, insert it.
        if (records < blockSize) {
            return insertValueIntoArray(newKey, value);
        } else {
//                If we don't have any more space, time to split
            final double parentStart = this.leafMetadata.getDouble(1);
            final double parentEnd = this.leafMetadata.getDouble(2);
            final short parentDirection = this.leafMetadata.getShort(3);
            final int idLength = getIDLength(this.leafID);
            final TDTreeHelpers.TriangleApex childApex = TDTreeHelpers.calculateChildApex(idLength + 1,
                    parentDirection,
                    parentStart,
                    parentEnd);
            final FastTuple lowerChild;
            final FastTuple higherChild;
            final LeafNode<Value> lowerChildLeaf;
            final LeafNode<Value> higherChildLeaf;
            final TDTreeHelpers.ChildDirection childDirection = TDTreeHelpers.calculateChildDirection(parentDirection);
//            If one of the children is a point, pick the lower, turn it into a point and move on
//            We also need to make sure we don't recurse too far, so the length of a leafID can't be more than 30
            if (TDTreeHelpers.triangleIsPoint(TDTreeHelpers.getTriangleVerticies(TDTreeHelpers.adjustedLength[idLength + 1], childDirection.lowerChild, childApex.start, childApex.end)) |
                    getIDLength(this.leafID) == (getIDLength(Integer.MAX_VALUE) - 1)) {
//                    Convert the leaf to a point leaf and replace the splittable node

                final PointLeaf<Object> pointLeaf = new PointLeaf<>(leafID, leafMetadata);
//                    Copy in all the keys and values
                pointLeaf.copyInitialValues(this.keys, this.values);
//                    Copy the new value
                pointLeaf.insert(newKey, value);

                this.records = 0;

                return new LeafSplit(leafID, pointLeaf, pointLeaf);
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

                lowerChildLeaf = new SplittableLeaf<>(leafID << 1, lowerChild, this.blockSize);
                higherChildLeaf = new SplittableLeaf<>((leafID << 1) | 1, higherChild, this.blockSize);
                logger.trace("Splitting {} into {} and {}", this.getBinaryStringID(), lowerChildLeaf.getBinaryStringID(), lowerChildLeaf.getBinaryStringID());
            }
            final LeafSplit leafSplit = new LeafSplit(this.leafID, lowerChildLeaf, higherChildLeaf);
//            Divide values into children, by testing to see if they belong to the lower child
            final double[] lowerChildVerticies = TDTreeHelpers.getTriangleVerticies(TDTreeHelpers.adjustedLength[idLength + 1], childDirection.lowerChild, childApex.start, childApex.end);
            for (int i = 0; i < this.blockSize; i++) {
                FastTuple key = keys[i];
                if (key != null) {
                    if (TDTreeHelpers.pointInTriangle(key.getLong(2), key.getLong(3), lowerChildVerticies)) {
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
            if (TDTreeHelpers.pointInTriangle(newKey.getLong(2), newKey.getLong(3), lowerChildVerticies)) {
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
        return delete(eval);
    }

    @Override
    boolean delete(TupleExpressionGenerator.BooleanTupleExpression expression) {
        for (int i = 0; i < this.records; i++) {
            final FastTuple key = keys[i];
            if (key != null) {
                if (expression.evaluate(key)) {
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
    long deleteKeysWithValue(@NonNull Value value) {
        long deletedKeys = 0;
        for (int i = 0; i < this.records; i++) {
            if (value.equals(values[i])) {
                keys[i] = null;
                values[i] = null;
                deletedKeys++;
            }
        }
        return deletedKeys;
    }

    @Override
    boolean update(String objectID, long atTime, @NonNull Value value) {
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

    @Override
    Map<FastTuple, @NonNull Value> dumpLeaf() {
        Map<FastTuple, @NonNull Value> leafRecords = new HashMap<>();
        for (int i = 0; i < this.records; i++) {
            final FastTuple key = keys[i];
            if (key != null) {
                if (values[i] != null) {
                    leafRecords.put(key, values[i]);
                }
            }
        }
        return leafRecords;
    }

    @Override
    double calculateFragmentation() {
        double nullRecords = 0;
        for (int i = 0; i < this.records; i++) {
            if (keys[i] == null) {
                nullRecords++;
            }
        }
        return nullRecords / (double) this.records;
    }

    @SuppressWarnings({"argument.type.incompatible"})
    private @Nullable LeafSplit insertValueIntoArray(FastTuple key, Value value) {
        if (!ArrayUtils.contains(keys, key)) {
            keys[records] = key;
            values[records] = value;
            records++;
        }
        return null;
    }
}

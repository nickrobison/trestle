package com.nickrobison.trestle.caching.tdtree;

import com.boundary.tuple.FastTuple;
import com.boundary.tuple.codegen.TupleExpressionGenerator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 2/13/17.
 */
public class PointLeaf<Value> extends LeafNode<Value> {
    private static final Logger logger = LoggerFactory.getLogger(PointLeaf.class);
    private final Map<FastTuple, Value> values = new Object2ObjectOpenHashMap<>(100, .7f);
    private int records = 0;

    PointLeaf(int leafID, FastTuple leafMetadata) {
        super(leafID, leafMetadata);
        logger.trace("Creating Point Node {}", this.getBinaryStringID());
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
        return getValue(eval);
    }

    @Override
    Value getValue(TupleExpressionGenerator.BooleanTupleExpression expression) {
        final Optional<Value> value = this.values.entrySet()
                .stream()
                .filter(entry -> expression.evaluate(entry.getKey()))
                .map(Map.Entry::getValue)
                .findAny();
        return value.orElse(null);
    }

    @Override
    LeafSplit insert(long objectID, long startTime, long endTime, @NonNull Value value) {
        return insert(buildObjectKey(objectID, startTime, endTime), value);
    }

    @Override
    LeafSplit insert(FastTuple newKey, @NonNull Value value) {
        if (!this.values.containsKey(newKey)) {
            this.values.put(newKey, value);
            this.records++;
        }
        return null;
    }

    @Override
    boolean delete(String objectID, long atTime) {
        final TupleExpressionGenerator.BooleanTupleExpression eval = buildFindExpression(objectID, atTime);
        return delete(eval);
    }

    @Override
    boolean delete(TupleExpressionGenerator.BooleanTupleExpression expression) {
        final Optional<FastTuple> matchingKey = this.values.entrySet()
                .stream()
                .filter(entry -> expression.evaluate(entry.getKey()))
                .map(Map.Entry::getKey)
                .findAny();
        if (matchingKey.isPresent()) {
            this.values.remove(matchingKey.get());
            return true;
        }
        return false;
    }

    @Override
    long deleteKeysWithValue(@NonNull Value value) {
        final List<FastTuple> list = this.values.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        list.forEach(this.values::remove);
        return list.size();
    }

    @Override
    boolean update(String objectID, long atTime, @NonNull Value value) {
        final FastTuple key = buildObjectKey(objectID, atTime, atTime);
        if (this.values.containsKey(key)) {
            this.values.replace(key, value);
            return true;
        }
        return false;
    }
    @Override
    Map<FastTuple, Value> dumpLeaf() {
        Map<FastTuple, Value> leafRecords = new HashMap<>();
        leafRecords.putAll(this.values);
        return leafRecords;
    }

    @Override
    double calculateFragmentation() {
        return 0;
    }
}

package com.nickrobison.trestle.reasoner.caching;

import com.codahale.metrics.annotation.Gauge;
import com.nickrobison.trestle.reasoner.caching.tdtree.LeafNode;
import com.nickrobison.trestle.reasoner.caching.tdtree.LeafStatistics;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by nrobison on 2/13/17.
 */
@SuppressWarnings({"squid:S00119"})
public interface ITrestleIndex<Value> {
    /**
     * Insert a key/value pair with an open interval
     *
     * @param objectID  - String object key
     * @param startTime - Long temporal of start temporal
     * @param value     - Value
     */
    void insertValue(String objectID, long startTime, @NonNull Value value);

    /**
     * Insert a key/value pair valid over a specific interval (or single point
     *
     * @param objectID  - String object key
     * @param startTime - Long temporal of start temporal
     * @param endTime   - Long temporal of end temporal
     * @param value     - {@link Value} to insert
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    void insertValue(String objectID, long startTime, long endTime, @NonNull Value value);

    /**
     * Get the index value valid for the given key at the specific point in time
     *
     * @param objectID - String object key
     * @param atTime   - Long temporal of validAt time
     * @return - {@link Value} if the key exists, null if not
     */
    @SuppressWarnings("Duplicates")
    @Nullable Value getValue(String objectID, long atTime);

    /**
     * Remove the key/value pair for the given objectID at the specified time
     *
     * @param objectID - String object key
     * @param atTime   - Logn temporal of validAt time to remove
     */
    void deleteValue(String objectID, long atTime);

    /**
     * Deletes all keys that contain the specified value
     *
     * @param value - {@link Value} to purge from cache
     */
    void deleteKeysWithValue(@NonNull Value value);

    /**
     * Update the value associated with the key valid at the specified temporal
     *
     * @param objectID - String object key
     * @param atTime   - Long temporal of validAt temporal
     * @param value    {@link Value} to update key with
     */
    void updateValue(String objectID, long atTime, @NonNull Value value);

    /**
     * Replaces a key/value pair valid at the specified point, with a new key/value pair and an updated temporal interval
     * For a point object, set the startTime and endTime parameters equal.
     *
     * @param objectID  - String object key
     * @param atTime    - Long temporal of validAt temporal
     * @param startTime - Long temporal of new validFrom temporal
     * @param endTime   - Long temporal of new validTo temporal
     * @param value     - {@link Value} new value to insert
     */
    void replaceKeyValue(String objectID, long atTime, long startTime, long endTime, @NonNull Value value);

    /**
     * Update the temporal interval of a given key to a new open interval
     *
     * @param objectID  - String object key
     * @param atTime    - Long temporal of validAt to update
     * @param startTime - Long temporal of new validFrom temporal
     */
    void setKeyTemporals(String objectID, long atTime, long startTime);

    /**
     * Update the temporal interval of a given key to a new closed interval, or point
     * For a point object, set the startTime and endTime parameters equal.
     *
     * @param objectID  - String object key
     * @param atTime    - Long temporal of validAt to update
     * @param startTime - Long temporal of new validFrom temporal
     * @param endTime   - Long temporal of new validTo temporal
     */
    void setKeyTemporals(String objectID, long atTime, long startTime, long endTime);

    /**
     * Rebuild the Index, re-balancing all the nodes and compacting excess space
     */
    void rebuildIndex();

    /**
     * Compute statistics on index leafs
     *
     * @return - {@link List} of {@link LeafStatistics}
     * @exception - {@link UnsupportedOperationException} if the underlying index is not of type {@link com.nickrobison.trestle.reasoner.caching.tdtree.TDTree}
     */
    List<LeafStatistics> getLeafStatistics();

    /**
     * Calculate percent fragmentation of all {@link LeafNode}s
     *
     * @return - Percent fragmentation
     */
    double calculateFragmentation();

    /**
     * Calculate estimated index size
     * Each write/delete modifies a {@link AtomicLong}, so it's just an estimated count, but quick to get
     *
     * @return - long of index size
     */
    @Gauge(name = "td-tree.index-size", absolute = true)
    long getIndexSize();

    /**
     * Return the maximum temporal value (epoch millis)
     *
     * @return - long of maximum value
     */
    long getMaxValue();
}

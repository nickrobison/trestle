package com.nickrobison.trestle.reasoner.caching;

import com.nickrobison.trestle.reasoner.caching.tdtree.LeafStatistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by nrobison on 5/1/17.
 */
public class TrestleCacheStatistics implements Serializable {
    public static final long serialVersionUID = 42L;

    private final Long offsetValue;
    private final Long maxValue;
    private final Long validIndexSize;
    private final Double validIndexFragmentation;
    private final Long dbIndexSize;
    private final Double dbIndexFragmentation;
    private final List<LeafStatistics> validLeafStats;
    private final List<LeafStatistics> dbLeafStats;

    TrestleCacheStatistics(Long offsetValue, Long maxValue, Long validIndexSize, Double validIndexFragmentation, Long dbIndexSize, Double dbIndexFragmentation) {
        this.offsetValue = offsetValue;
        this.maxValue = maxValue;
        this.validIndexSize = validIndexSize;
        this.validIndexFragmentation = validIndexFragmentation;
        this.dbIndexSize = dbIndexSize;
        this.dbIndexFragmentation = dbIndexFragmentation;
        this.validLeafStats = new ArrayList<>();
        this.dbLeafStats = new ArrayList<>();
    }

    public Long getOffsetValue() {
        return offsetValue;
    }

    public Long getMaxValue() {
        return maxValue;
    }

    public Long getValidIndexSize() {
        return validIndexSize;
    }

    public Double getValidIndexFragmentation() {
        return validIndexFragmentation;
    }

    public Long getDbIndexSize() {
        return dbIndexSize;
    }

    public Double getDbIndexFragmentation() {
        return dbIndexFragmentation;
    }

    public List<LeafStatistics> getValidLeafStats() {
        return validLeafStats;
    }

    public void addValidLeafStats(Collection<LeafStatistics> leafStats) {
        this.validLeafStats.addAll(leafStats);
    }

    public List<LeafStatistics> getDbLeafStats() {
        return dbLeafStats;
    }

    public void addDBLeafStats(Collection<LeafStatistics> leafStats) {
        this.dbLeafStats.addAll(leafStats);
    }
}

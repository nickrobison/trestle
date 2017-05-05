package com.nickrobison.trestle.caching;

/**
 * Created by nrobison on 5/1/17.
 */
public class TrestleCacheStatistics {
    private static final long serialVersionUID = 42L;

    private final Long validIndexSize;
    private final Double validIndexFragmentation;
    private final Long dbIndexSize;
    private final Double dbIndexFragmentation;

    TrestleCacheStatistics(Long validIndexSize, Double validIndexFragmentation, Long dbIndexSize, Double dbIndexFragmentation) {
        this.validIndexSize = validIndexSize;
        this.validIndexFragmentation = validIndexFragmentation;
        this.dbIndexSize = dbIndexSize;
        this.dbIndexFragmentation = dbIndexFragmentation;
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
}

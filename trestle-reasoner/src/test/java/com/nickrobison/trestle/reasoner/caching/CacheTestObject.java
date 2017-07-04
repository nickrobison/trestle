package com.nickrobison.trestle.reasoner.caching;

import java.io.Serializable;

/**
 * Created by nrobison on 7/3/17.
 */
class CacheTestObject implements Serializable {
    private final String objectName;
    private final Integer objectValue;

    CacheTestObject(String objectName, Integer objectValue) {
        this.objectName = objectName;
        this.objectValue = objectValue;
    }

    public String getObjectName() {
        return objectName;
    }

    public Integer getObjectValue() {
        return objectValue;
    }
}

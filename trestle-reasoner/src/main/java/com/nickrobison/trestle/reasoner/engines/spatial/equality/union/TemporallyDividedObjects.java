package com.nickrobison.trestle.reasoner.engines.spatial.equality.union;

import java.util.Set;

public class TemporallyDividedObjects<T> {

    private final Set<T> earlyObjects;
    private final Set<T> lateObjects;

    public TemporallyDividedObjects(Set<T> earlyObjects, Set<T> lateObjects) {
        this.earlyObjects = earlyObjects;
        this.lateObjects = lateObjects;
    }

    public Set<T> getEarlyObjects() {
        return earlyObjects;
    }

    public Set<T> getLateObjects() {
        return lateObjects;
    }
}

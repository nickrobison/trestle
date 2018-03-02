package com.nickrobison.metrician.instrumentation.NoOpMetrics;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;

/**
 * Created by nrobison on 4/14/17.
 */
class NoOpReservoir implements Reservoir {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public void update(long value) {
        //        No-op
    }

    @Override
    public Snapshot getSnapshot() {
        return new NoOpSnapshot();
    }
}

package com.nickrobison.metrician.instrumentation.NoOpMetrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;

/**
 * Created by nrobison on 4/14/17.
 */
public class NoOpHistogram extends Histogram {

    /**
     * Creates a new {@link Histogram} with the given reservoir.
     *
     * @param reservoir the reservoir to create a histogram from
     */
    public NoOpHistogram() {
        super(new NoOpReservoir());
    }

    @Override
    public void update(int value) {
    }

    @Override
    public void update(long value) {
    }

    @Override
    public long getCount() {
        return 0;
    }

    @Override
    public Snapshot getSnapshot() {
        return new NoOpSnapshot();
    }
}

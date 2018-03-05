package com.nickrobison.metrician.instrumentation.NoOpMetrics;

import com.codahale.metrics.Snapshot;

import java.io.OutputStream;

/**
 * Created by nrobison on 4/14/17.
 */
public class NoOpSnapshot extends Snapshot {
    @Override
    public double getValue(double quantile) {
        return 0;
    }

    @Override
    public long[] getValues() {
        return new long[0];
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public long getMax() {
        return 0;
    }

    @Override
    public double getMean() {
        return 0;
    }

    @Override
    public long getMin() {
        return 0;
    }

    @Override
    public double getStdDev() {
        return 0;
    }

    @Override
    public void dump(OutputStream output) {
        //        No-op
    }
}

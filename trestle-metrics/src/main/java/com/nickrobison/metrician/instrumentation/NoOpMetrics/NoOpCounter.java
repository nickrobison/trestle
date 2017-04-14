package com.nickrobison.metrician.instrumentation.NoOpMetrics;

import com.codahale.metrics.Counter;

/**
 * Created by nrobison on 4/14/17.
 */
public class NoOpCounter extends Counter {

    @Override
    public void inc() {
    }

    @Override
    public void inc(long n) {
    }

    @Override
    public void dec() {
    }

    @Override
    public void dec(long n) {
    }

    @Override
    public long getCount() {
        return 0;
    }
}

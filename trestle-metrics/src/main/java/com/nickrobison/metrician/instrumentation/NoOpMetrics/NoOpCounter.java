package com.nickrobison.metrician.instrumentation.NoOpMetrics;

import com.codahale.metrics.Counter;

/**
 * Created by nrobison on 4/14/17.
 */
public class NoOpCounter extends Counter {

    @Override
    public void inc() {
//        No-op
    }

    @Override
    public void inc(long n) {
        //        No-op
    }

    @Override
    public void dec() {
        //        No-op
    }

    @Override
    public void dec(long n) {
        //        No-op
    }

    @Override
    public long getCount() {
        return 0;
    }
}

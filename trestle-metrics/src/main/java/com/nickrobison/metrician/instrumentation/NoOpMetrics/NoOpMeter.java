package com.nickrobison.metrician.instrumentation.NoOpMetrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Meter;

/**
 * Created by nrobison on 7/7/17.
 */
public class NoOpMeter extends Meter {

    public NoOpMeter() {
// Not implemented
    }

    public NoOpMeter(Clock clock) {
// Not implemented
    }

    @Override
    public void mark() {
// Not implemented
    }

    @Override
    public void mark(long n) {
// Not implemented
    }

    @Override
    public long getCount() {
        return 0;
    }

    @Override
    public double getFifteenMinuteRate() {
        return 0;
    }

    @Override
    public double getFiveMinuteRate() {
        return 0;
    }

    @Override
    public double getMeanRate() {
        return 0;
    }

    @Override
    public double getOneMinuteRate() {
        return 0;
    }
}

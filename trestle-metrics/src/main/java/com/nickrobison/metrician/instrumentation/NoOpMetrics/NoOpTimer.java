package com.nickrobison.metrician.instrumentation.NoOpMetrics;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.concurrent.Callable;

/**
 * Created by nrobison on 4/14/17.
 */
public class NoOpTimer extends Timer {

    private final Timer timer;

    public NoOpTimer() {
        this.timer = new Timer();
    }
    @Override
    public <T> T time(Callable<T> event) throws Exception {
        return null;
    }

    @Override
    public void time(Runnable event) {
    }

    @Override
    public Context time() {
        return this.timer.time();
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

    @Override
    public Snapshot getSnapshot() {
        return new NoOpSnapshot();
    }
}

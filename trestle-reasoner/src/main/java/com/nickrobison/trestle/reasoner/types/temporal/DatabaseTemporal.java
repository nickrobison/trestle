package com.nickrobison.trestle.reasoner.types.temporal;

import com.nickrobison.trestle.reasoner.types.TemporalScope;

import java.time.temporal.Temporal;

/**
 * Created by nrobison on 1/25/17.
 */
public class DatabaseTemporal {
    private static final TemporalScope SCOPE = TemporalScope.DATABASE;

    public static class Builder<T extends Temporal> {

        Builder() {
        }

        public PointTemporal.Builder at(T at) {
            return new PointTemporal.Builder<>(SCOPE, at);
        }

        public IntervalTemporal.Builder from(T from) {
            return new IntervalTemporal.Builder<>(SCOPE, from);
        }
    }
}

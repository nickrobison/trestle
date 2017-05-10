package com.nickrobison.trestle.reasoner.types.temporal;

import com.nickrobison.trestle.reasoner.types.TemporalScope;

import java.time.temporal.Temporal;

/**
 * Created by nrobison on 6/30/16.
 */
public class ValidTemporal {

    private static final TemporalScope SCOPE = TemporalScope.VALID;

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

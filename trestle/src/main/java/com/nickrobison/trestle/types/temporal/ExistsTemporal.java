package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;

/**
 * Created by nrobison on 6/30/16.
 */
public class ExistsTemporal {

    private final static TemporalScope SCOPE = TemporalScope.EXISTS;

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

package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;

import java.time.LocalDateTime;

/**
 * Created by nrobison on 6/30/16.
 */
public class ValidTemporal {

    private static final TemporalScope SCOPE = TemporalScope.VALID;

    public static class Builder {

        Builder() {
        }

        public PointTemporal.Builder at(LocalDateTime at) {
            return new PointTemporal.Builder(SCOPE, at);
        }

        public IntervalTemporal.Builder from(LocalDateTime from) {
            return new IntervalTemporal.Builder(SCOPE, from);
        }


    }
}

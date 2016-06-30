package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;

import java.time.LocalDateTime;

/**
 * Created by nrobison on 6/30/16.
 */
public class ExistsTemporal {

    private final static TemporalScope SCOPE = TemporalScope.EXISTS;
//    @Override
//    TemporalType getType() {
//        return null;
//    }
//
//    @Override
//    TemporalScope getScope() {
//        return null;
//    }

    public static class Builder {
//        private TemporalType type;
//        private LocalDateTime startTime;
//        private LocalDateTime endTime;

        Builder() {        }

        public PointTemporal.Builder at(LocalDateTime at) { return new PointTemporal.Builder(SCOPE, at);}

        public IntervalTemporal.Builder from(LocalDateTime from) { return new IntervalTemporal.Builder(SCOPE, from);}

//        public Builder withExistsAt(LocalDateTime existsAt) {
//            this.type = TemporalType.POINT;
////            this.startTime =
//        }
    }
}

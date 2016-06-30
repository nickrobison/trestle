package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by nrobison on 6/30/16.
 */
public class ValidTemporal {

    private static final TemporalScope SCOPE = TemporalScope.VALID;

//    private final LocalDateTime startTime;
//    private final LocalDateTime endTime;

//    ValidTemporal(Builder builder) {
////        super(UUID.randomUUID().toString(), builder.temporal_of);
//        this.startTime = builder.startTime;
//        this.endTime = builder.endTime;
//    }
//
//    @Override
//    TemporalType getType() {
//        return null;
//    }
//
//    @Override
//    TemporalScope getScope() {
//        return SCOPE;
//    }

//    LocalDateTime getStartTime() {
//        return this.startTime;
//    }
//
//    LocalDateTime getEndTime() {
//        return this.getEndTime();
//    }

    public static class Builder {

//        private TemporalType type;
//        private LocalDateTime startTime;
//        private LocalDateTime endTime;
//        private Set<OWLNamedIndividual> temporal_of = new HashSet<>();

        Builder() {
        }

        public PointTemporal.Builder at(LocalDateTime at) {
            return new PointTemporal.Builder(SCOPE, at);
        }

        public IntervalTemporal.Builder from(LocalDateTime from) { return new IntervalTemporal.Builder(SCOPE, from);}




//        public Builder withValidAt(LocalDateTime validAt) {
//            this.type = TemporalType.POINT;
//            this.startTime = validAt;
//            return this;
//        }
//
//        public Builder withValidFrom(LocalDateTime validFrom) {
//            this.type = TemporalType.INTERVAL;
//            this.startTime = validFrom;
//            return this;
//        }
//
//        public Builder withValidTo(LocalDateTime validTo) {
//            this.type = TemporalType.INTERVAL;
//            this.endTime = validTo;
//            return this;
//        }
//
//        public Builder withValidInterval(LocalDateTime validFrom, LocalDateTime validTo) {
//            return this
//                    .withValidFrom(validFrom)
//                    .withValidTo(validTo);
//        }
//
//        public Builder withTemporalOf(OWLNamedIndividual... individuals) {
//            this.temporal_of.addAll(Arrays.asList(individuals));
//            return this;
//        }

//        public ValidTemporal build() {
//            return new ValidTemporal()
//        }


    }
}

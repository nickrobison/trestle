package com.nickrobison.trestle.types;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created by nrobison on 6/29/16.
 */
public class TemporalObject {

    private final String id;
    private final TemporalType type;
    private final TemporalScope scope;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private TemporalObject(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.scope = builder.scope;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
    }

    public static class Builder {

        private String id;
        private TemporalType type;
        private TemporalScope scope;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public Builder() {
            this.id = UUID.randomUUID().toString();
        }

        public Builder withID(String id) {
            this.id = id;
            return this;
        }

        public Builder withValidAt(LocalDateTime validAt) {
            this.type = TemporalType.POINT;
            this.scope = TemporalScope.VALID
            this.startTime = validAt;
            return this;
        }

        public Builder withExistsAt(LocalDateTime existsAt) {
            this.type = TemporalType.POINT;
            this.scope = TemporalScope.EXISTS;
            this.startTime = existsAt;
            return this;
        }

        public Builder withValidFrom(LocalDateTime validFrom) {
            this.type = TemporalType.INTERVAL;
            this.scope = TemporalScope.VALID;
            this.startTime = validFrom;
            return this;
        }

        public Builder withValidTo(LocalDateTime validTo) {
            this.type = TemporalType.INTERVAL;
            this.scope = TemporalScope.VALID;
            this.endTime = validTo;
            return this;
        }

        public Builder withExistsFrom(LocalDateTime existsFrom) {
            this.type = TemporalType.INTERVAL;
            this.scope = TemporalScope.EXISTS;
            this.startTime = existsFrom;
            return this;
        }

        public Builder withExistsTo(LocalDateTime existsTo) {
            this.type = TemporalType.INTERVAL;
            this.scope = TemporalScope.EXISTS;
            this.endTime = existsTo;
            return this;
        }

        public Builder withValidInterval(LocalDateTime validFrom, LocalDateTime validTo) {
            this.type = TemporalType.INTERVAL;
            this.scope = TemporalScope.VALID;
            this.startTime = validFrom;
            this.endTime = validTo;
            return this;
        }

        public Builder withExistsInterval(LocalDateTime existsFrom, LocalDateTime existsTo) {
            this.type = TemporalType.INTERVAL;
            this.scope = TemporalScope.EXISTS;
            this.startTime = existsFrom;
            this.endTime = existsTo;
            return this;
        }

        public TemporalObject build() {
            return new TemporalObject(this);
        }
    }
}

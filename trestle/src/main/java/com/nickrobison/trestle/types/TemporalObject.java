package com.nickrobison.trestle.types;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by nrobison on 6/29/16.
 */
@SuppressWarnings("initialization")
public class TemporalObject {

    private final String id;
    private final TemporalType type;
    private final TemporalScope scope;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Set<OWLNamedIndividual> temporal_of;

    private TemporalObject(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.scope = builder.scope;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.temporal_of = builder.temporal_of;
    }

    public TemporalType getType() {
        return type;
    }

    public TemporalScope getScope() {
        return scope;
    }

    public boolean isInterval() {
        return type == TemporalType.INTERVAL;
    }

    public boolean isContinuing() {
        return isInterval() && endTime == null;
    }

    public void addTemporalRelation(OWLNamedIndividual individual) {
        this.temporal_of.add(individual);
    }

    public Set<OWLNamedIndividual> getTemporalRelations() {
        return this.temporal_of;
    }

    public static class Builder {

        private String id;
        private TemporalType type;
        private TemporalScope scope;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Set<OWLNamedIndividual> temporal_of;

        public Builder() {

            this.id = UUID.randomUUID().toString();
            this.temporal_of = new HashSet<>();
        }

        public Builder withID(String id) {

            this.id = id;
            return this;
        }

        public Builder withType(TemporalType type) {

            this.type = type;
            return this;
        }

        public Builder withScope(TemporalScope scope) {

            this.scope = scope;
            return this;
        }

        public Builder withValidAt(LocalDateTime validAt) {

            this.startTime = validAt;
            return this.withType(TemporalType.POINT).withScope(TemporalScope.VALID);
        }

        public Builder withExistsAt(LocalDateTime existsAt) {

            this.startTime = existsAt;
            return this.withType(TemporalType.POINT).withScope(TemporalScope.EXISTS);
        }

        public Builder withValidFrom(LocalDateTime validFrom) {

            this.startTime = validFrom;
            return this.withType(TemporalType.INTERVAL).withScope(TemporalScope.VALID);
        }

        public Builder withValidTo(LocalDateTime validTo) {

            this.endTime = validTo;
            return this.withType(TemporalType.INTERVAL).withScope(TemporalScope.VALID);
        }

        public Builder withExistsFrom(LocalDateTime existsFrom) {

            this.startTime = existsFrom;
            return this.withType(TemporalType.INTERVAL).withScope(TemporalScope.EXISTS);
        }

        public Builder withExistsTo(LocalDateTime existsTo) {

            this.endTime = existsTo;
            return this.withType(TemporalType.INTERVAL).withScope(TemporalScope.EXISTS);
        }

        public Builder withValidInterval(LocalDateTime validFrom, LocalDateTime validTo) {

            return this
                    .withValidFrom(validFrom)
                    .withValidTo(validTo);
        }

        public Builder withExistsInterval(LocalDateTime existsFrom, LocalDateTime existsTo) {
            return this
                    .withExistsFrom(existsFrom)
                    .withExistsTo(existsTo);
        }

        public Builder withTemporalOf(OWLNamedIndividual... individuals) {
            this.temporal_of.addAll(Arrays.asList(individuals));
            return this;
        }

        public TemporalObject build() {
            return new TemporalObject(this);
        }
    }
}

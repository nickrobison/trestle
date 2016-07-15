package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by nrobison on 6/30/16.
 */
public class IntervalTemporal extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.INTERVAL;
    private final TemporalScope scope;
    private final LocalDateTime fromTime;
    private final Optional<LocalDateTime> toTime;

    private IntervalTemporal(Builder builder) {
        super(UUID.randomUUID().toString(), builder.relations);
        this.scope = builder.scope;
        this.fromTime = builder.fromTime;
        this.toTime = builder.toTime;
    }
    @Override
    public TemporalType getType() {
        return TYPE;
    }

    @Override
    public TemporalScope getScope() {
        return this.scope;
    }

    @Override
    public boolean isPoint() {
        return false;
    }

    @Override
    public boolean isInterval() {
        return true;
    }

    public boolean isContinuing() {
        return !toTime.isPresent();
    }

    public LocalDateTime getFromTime() {
        return this.fromTime;
    }

    public Optional<LocalDateTime> getToTime() {
        return this.toTime;
    }

    public static class Builder {

        private TemporalScope scope;
        private LocalDateTime fromTime;
        private Optional<LocalDateTime> toTime = Optional.empty();
        private Optional<Set<OWLNamedIndividual>> relations = Optional.empty();

        Builder(TemporalScope scope, LocalDateTime from) {
            this.scope = scope;
            this.fromTime = from;
        }

        public Builder to(LocalDateTime to) {
            this.toTime = Optional.of(to);
            return this;
        }

        public IntervalTemporal withRelations(OWLNamedIndividual... relations) {
            this.relations = Optional.of(new HashSet<>(Arrays.asList(relations)));
            return new IntervalTemporal(this);
        }


    }
}

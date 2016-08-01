package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Created by nrobison on 6/30/16.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class IntervalTemporal<T extends Temporal> extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.INTERVAL;
    private final TemporalScope scope;
    private final T fromTime;
    private final Optional<T> toTime;
    private final boolean isDefault;
    private final Optional<String> startName;
    private final Optional<String> endName;

    private IntervalTemporal(Builder<T> builder) {
        super(UUID.randomUUID().toString(), builder.relations);
        this.scope = builder.scope;
        this.fromTime = builder.fromTime;
        this.toTime = builder.toTime;
        this.isDefault = builder.isDefault;
        this.startName = builder.startName;
        this.endName = builder.endName;
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
    public boolean isValid() {
        return (this.scope == TemporalScope.VALID);
    }

    @Override
    public boolean isExists() {
        return (this.scope == TemporalScope.EXISTS);
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

    public boolean isDefault() { return this.isDefault; }

    public T getFromTime() {
        return this.fromTime;
    }

    public Optional<T> getToTime() {
        return this.toTime;
    }

    public String getStartName() {
        return this.startName.orElse("intervalStart");
    }

    public String getEndName() {
        return this.endName.orElse("intervalEnd");
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder<T extends Temporal> {

        private TemporalScope scope;
        private T fromTime;
        private Optional<T> toTime = Optional.empty();
        private Optional<Set<OWLNamedIndividual>> relations = Optional.empty();
        private Optional<String> startName = Optional.empty();
        private Optional<String> endName = Optional.empty();
        private boolean isDefault = false;

        Builder(TemporalScope scope, T from) {
            this.scope = scope;
            this.fromTime = from;
        }

        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public Builder to(T to) {
            this.toTime = Optional.of(to);
            return this;
        }

        public Builder withParameterNames(String startName, @Nullable String endName) {
            this.startName = Optional.of(startName);
            if (endName != null) {
                this.endName = Optional.of(endName);
            }
            return this;
        }

        public IntervalTemporal withRelations(OWLNamedIndividual... relations) {
            this.relations = Optional.of(new HashSet<>(Arrays.asList(relations)));
            return new IntervalTemporal<>(this);
        }


    }
}

package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Created by nrobison on 6/30/16.
 */
public class PointTemporal<T extends Temporal> extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.POINT;
    private final TemporalScope scope;
    private final T atTime;
    private final Optional<String> parameterName;

    private PointTemporal(Builder<T> builder) {
        super(UUID.randomUUID().toString(), builder.relations);
        this.scope = builder.scope;
        this.atTime = builder.atTime;
        this.parameterName = builder.parameterName;
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
        return scope == TemporalScope.VALID;
    }

    @Override
    public boolean isExists() {
        return scope == TemporalScope.EXISTS;
    }

    @Override
    public boolean isPoint() {
        return true;
    }

    @Override
    public boolean isInterval() {
        return false;
    }

    public T getPointTime() {
        return this.atTime;
    }

    public String getParameterName() {
        return this.parameterName.orElse("pointTime");
    }

    public static class Builder<T extends Temporal> {

        private TemporalScope scope;
        private T atTime;
        private Optional<Set<OWLNamedIndividual>> relations = Optional.empty();
        private Optional<String> parameterName = Optional.empty();

        Builder(TemporalScope scope, T at) {
            this.scope = scope;
            this.atTime = at;
        }

        public Builder withParameterName(String name) {
            this.parameterName = Optional.of(name);
            return this;
        }

        public PointTemporal withRelations(OWLNamedIndividual... relations) {
            this.relations = Optional.of(new HashSet<>(Arrays.asList(relations)));
            return new PointTemporal<>(this);
        }

    }
}

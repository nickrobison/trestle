package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by nrobison on 6/30/16.
 */
public class PointTemporal extends TemporalObject {

    private static final TemporalType TYPE = TemporalType.POINT;
    private final TemporalScope scope;
    private final LocalDateTime atTime;

    private PointTemporal(Builder builder) {
        super(UUID.randomUUID().toString(), builder.relations);
        this.scope = builder.scope;
        this.atTime = builder.atTime;
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
        return true;
    }

    @Override
    public boolean isInterval() {
        return false;
    }

    public LocalDateTime getPointTime() {
        return this.atTime;
    }

    public static class Builder {

        private TemporalScope scope;
        private LocalDateTime atTime;
        private Optional<Set<OWLNamedIndividual>> relations = Optional.empty();

        Builder(TemporalScope scope, LocalDateTime at) {
            this.scope = scope;
            this.atTime = at;
        }

        public PointTemporal withRelations(OWLNamedIndividual... relations) {
            this.relations = Optional.of(new HashSet<>(Arrays.asList(relations)));
            return new PointTemporal(this);
        }
    }
}

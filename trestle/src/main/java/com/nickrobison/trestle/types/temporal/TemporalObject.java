package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 6/30/16.
 */
public abstract class TemporalObject {

    private static final String PREFIX = "trestle:";
    private final String id;
    private final Optional<Set<OWLNamedIndividual>> temporal_of;

    TemporalObject(String id, Optional<Set<OWLNamedIndividual>> relations) {
        this.id = id;
        this.temporal_of = relations;
    }

    public String getID() {
        return this.id;
    }

    public IRI getIDAsIRI() {
        return IRI.create(PREFIX, this.id);
    }

    public Set<OWLNamedIndividual> getTemporalRelations() {
        if (this.temporal_of.isPresent()) {
            return this.temporal_of.get();
        } else {
            return new HashSet<>();
        }
    }

    public abstract boolean isInterval();

    public IntervalTemporal asInterval() {
        return (IntervalTemporal) this;
    }

    public abstract boolean isPoint();

    public PointTemporal asPoint() {
        return (PointTemporal) this;
    }

    public abstract TemporalType getType();

    public abstract TemporalScope getScope();

    public abstract boolean isValid();

    public abstract boolean isExists();
}

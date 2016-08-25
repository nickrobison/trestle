package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.PREFIX;
import static com.nickrobison.trestle.common.StaticIRI.XSDPREFIX;

/**
 * Created by nrobison on 6/30/16.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class TemporalObject {

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

    /**
     * Get the Base temporal type of the object
     * @return - Temporal subclass of object
     */
    public abstract Class<? extends Temporal> getBaseTemporalType();

    /**
     * Get the OWL IRI of the base temporal type
     * @return - IRI of OWLDatatype for base temporal type
     */
    public abstract IRI getBaseTemporalTypeIRI();

    protected static IRI parseTemporalClassToIRI(Class<? extends Temporal> clazz) {
        if (clazz == LocalDateTime.class) {
            return IRI.create(XSDPREFIX, "dateTime");
        } else if (clazz == LocalDate.class) {
            return IRI.create(XSDPREFIX, "date");
        } else  {
//            As a fall back, just use datetime.
            return IRI.create(XSDPREFIX, "date");
        }
    }
}

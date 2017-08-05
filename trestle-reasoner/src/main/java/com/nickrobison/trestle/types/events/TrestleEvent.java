package com.nickrobison.trestle.types.events;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.Serializable;
import java.time.temporal.Temporal;

public class TrestleEvent implements Serializable {
    private static final long serialVersionUID = 42L;

    private final TrestleEventType type;
    private final OWLNamedIndividual individual;
    private final Temporal atTemporal;


    public TrestleEvent(TrestleEventType type, OWLNamedIndividual individual, Temporal atTemporal) {
        this.type = type;
        this.individual = individual;
        this.atTemporal = atTemporal;
    }

    public TrestleEventType getType() {
        return type;
    }

    public OWLNamedIndividual getIndividual() {
        return individual;
    }

    public Temporal getAtTemporal() {
        return atTemporal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrestleEvent that = (TrestleEvent) o;

        if (getType() != that.getType()) return false;
        if (!getIndividual().equals(that.getIndividual())) return false;
        return getAtTemporal().equals(that.getAtTemporal());
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + getIndividual().hashCode();
        result = 31 * result + getAtTemporal().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TrestleEvent{" +
                "type=" + type +
                ", individual=" + individual +
                ", atTemporal=" + atTemporal +
                '}';
    }
}

package com.nickrobison.trestle.types.events;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.Serializable;
import java.time.temporal.Temporal;

@SuppressWarnings({"squid:S1948", "squid:S3437"}) // I believe all implementations of Temporal are serializable
public class TrestleEvent implements Serializable {
    private static final long serialVersionUID = 42L;

    private final TrestleEventType type;
    private final OWLNamedIndividual individual;
    private final OWLNamedIndividual eventID;
    private final Temporal atTemporal;

    public TrestleEvent(TrestleEventType type, OWLNamedIndividual subject, OWLNamedIndividual eventID, Temporal atTemporal) {
        this.type = type;
        this.individual = subject;
        this.eventID = eventID;
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
    @SuppressWarnings({"not.interned"})
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrestleEvent that = (TrestleEvent) o;

        if (getType() != that.getType()) return false;
        if (!getIndividual().equals(that.getIndividual())) return false;
        if (!eventID.equals(that.eventID)) return false;
        return getAtTemporal().equals(that.getAtTemporal());
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + getIndividual().hashCode();
        result = 31 * result + eventID.hashCode();
        result = 31 * result + getAtTemporal().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TrestleEvent{" +
                "type=" + type +
                ", individual=" + individual +
                ", eventID=" + eventID +
                ", atTemporal=" + atTemporal +
                '}';
    }
}

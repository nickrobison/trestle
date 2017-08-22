package com.nickrobison.trestle.reasoner.equality.union;

import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Wrapper class for storing an {@link OWLNamedIndividual} and its corresponding {@link TemporalObject} existence temporal
 */
class STObjectWrapper {
    private final OWLNamedIndividual individual;
    private final TemporalObject existenceTemporal;
    private final IRI type;

    STObjectWrapper(OWLNamedIndividual individual, IRI type, TemporalObject existenceTemporal) {
        this.individual = individual;
        this.existenceTemporal = existenceTemporal;
        this.type = type;
    }

    public OWLNamedIndividual getIndividual() {
        return individual;
    }

    public TemporalObject getExistenceTemporal() {
        return existenceTemporal;
    }

    public IRI getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "STObjectWrapper{" +
                "individual=" + individual +
                ", existenceTemporal=" + existenceTemporal +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        STObjectWrapper that = (STObjectWrapper) o;

        if (!getIndividual().equals(that.getIndividual())) return false;
        if (!getExistenceTemporal().equals(that.getExistenceTemporal())) return false;
        return getType().equals(that.getType());
    }

    @Override
    public int hashCode() {
        int result = getIndividual().hashCode();
        result = 31 * result + getExistenceTemporal().hashCode();
        result = 31 * result + getType().hashCode();
        return result;
    }
}

package com.nickrobison.trestle.utils;

import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Set;

/**
 * Created by nrobison on 1/15/17.
 */

/**
 * Temporal ID/Properties pair, use mostly for async functions in the TrestleIndividual building
 */
public class TemporalPropertiesPair {

    private final OWLNamedIndividual temporal;
    private final Set<OWLDataPropertyAssertionAxiom> temporalProperties;

    public TemporalPropertiesPair(OWLNamedIndividual temporal, Set<OWLDataPropertyAssertionAxiom> temporalProperties) {
        this.temporal = temporal;
        this.temporalProperties = temporalProperties;
    }

    public OWLNamedIndividual getTemporal() {
        return temporal;
    }

    public String getTemporalID() {
        return getTemporal().getIRI().toString();
    }

    public Set<OWLDataPropertyAssertionAxiom> getTemporalProperties() {
        return temporalProperties;
    }
}

package com.nickrobison.trestle.reasoner.events;

import com.nickrobison.trestle.types.TrestleEvent;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.temporal.Temporal;
import java.util.List;

public interface EventEngine {
    /**
     * Added a Trestle_Event to the given individual
     * @param event - {@link TrestleEvent} to add
     * @param individual - {@link OWLNamedIndividual} to add event to
     * @param eventTemporal - {@link Temporal} of when event occurred
     */
    void addEvent(TrestleEvent event, OWLNamedIndividual individual, Temporal eventTemporal);

    /**
     * Adjusts an object's events based on modified temporals
     * @param objectExistenceAxioms - {@link List} of {@link OWLDataPropertyAssertionAxiom} representing the new existence temporals to write
     */
    void adjustObjectEvents(List<OWLDataPropertyAssertionAxiom> objectExistenceAxioms);
}

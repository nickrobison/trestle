package com.nickrobison.trestle.reasoner.events;

import com.nickrobison.trestle.types.events.TrestleEventType;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Set;

public interface TrestleEventEngine {
    /**
     * Added a Trestle_Event to the given individual
     * @param event - {@link TrestleEventType} to add
     * @param individual - {@link OWLNamedIndividual} to add event to
     * @param eventTemporal - {@link Temporal} of when event occurred
     */
    void addEvent(TrestleEventType event, OWLNamedIndividual individual, Temporal eventTemporal);

    /**
     * Adjusts an object's events based on modified temporals
     * @param objectExistenceAxioms - {@link List} of {@link OWLDataPropertyAssertionAxiom} representing the new existence temporals to write
     */
    void adjustObjectEvents(List<OWLDataPropertyAssertionAxiom> objectExistenceAxioms);

    /**
     * Add a SPLIT or MERGE {@link TrestleEventType} to a given {@link OWLNamedIndividual}
     * Events are oriented subject -> object, so A splits_into [B,C,D] and H merged_from [E,F,G]
     * Individuals are not created if they don't already exist
     * throws {@link IllegalArgumentException} if something other than {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} is passed
     *
     * @param type    {@link TrestleEventType} to add
     * @param subject - {@link OWLNamedIndividual} subject of Event
     * @param objects - {@link Set} of {@link OWLNamedIndividual} that are the objects of the event
     */
    void addSplitMergeEvent(TrestleEventType type, OWLNamedIndividual subject, Set<OWLNamedIndividual> objects);
}

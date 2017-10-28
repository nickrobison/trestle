package com.nickrobison.trestle.reasoner.individual;

import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.events.TrestleEvent;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Optional;
import java.util.Set;

public interface IndividualEngine {
    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @return - {@link TrestleIndividual}
     */
    TrestleIndividual getTrestleIndividual(String individualIRI);

    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individual - {@link OWLNamedIndividual}
     * @return - {@link TrestleIndividual}
     */
    TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual);

    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     * @param clazz      - Class of object get get event for
     * @param individual - {@link OWLNamedIndividual} to gather events for
     * @return - {@link Optional} {@link Set} of {@link TrestleEvent} for the given individual
     */
    Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual);
}

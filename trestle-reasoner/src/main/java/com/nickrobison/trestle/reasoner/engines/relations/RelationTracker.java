package com.nickrobison.trestle.reasoner.engines.relations;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Created by nickrobison on 7/21/18.
 */
public interface RelationTracker {

    /**
     * Determine whether or a not the relationships between the two objects has already been computed.
     *
     * @param subject - {@link OWLNamedIndividual} of subject
     * @param object  - {@link OWLNamedIndividual} of object
     * @return - Whether or not the relation has already been computed
     */
    boolean hasRelation(OWLNamedIndividual subject, OWLNamedIndividual object);

    /**
     * Determine whether or a not the relationships between the two objects has already been computed.
     *
     * @param subject - {@link IRI} of subject
     * @param object  - {@link IRI} of object
     * @return - Whether or not the relation has already been computed
     */
    boolean hasRelation(IRI subject, IRI object);

    /**
     * Add the relationship between the two objects to the cache
     *
     * @param subject - {@link OWLNamedIndividual} of subject
     * @param object  - {@link OWLNamedIndividual} of object
     */
    void addRelation(OWLNamedIndividual subject, OWLNamedIndividual object);

    /**
     * Add the relationship between the two objects to the cache
     *
     * @param subject - {@link IRI} of subject
     * @param object  - {@link IRI} of object
     */
    void addRelation(IRI subject, IRI object);

    /**
     * Remove any relationships for the given object from the cache
     *
     * @param object - {@link OWLNamedIndividual}
     */
    void removeComputedRelations(OWLNamedIndividual object);

    /**
     * Remove any relationships for the given object from the cache
     *
     * @param object - {@link IRI}
     */
    void removeComputedRelations(IRI object);
}

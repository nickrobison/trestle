package com.nickrobison.trestle.reasoner.engines.relations;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nickrobison on 7/21/18.
 */
public class RelationTrackerNoOp implements RelationTracker {

    private static final Logger logger = LoggerFactory.getLogger(RelationTrackerNoOp.class);

    RelationTrackerNoOp() {
        logger.info("Relationship tracker is disabled.");
    }

    @Override
    public boolean hasRelation(OWLNamedIndividual subject, OWLNamedIndividual object) {
        return false;
    }

    @Override
    public boolean hasRelation(IRI subject, IRI object) {
        return false;
    }

    @Override
    public void addRelation(OWLNamedIndividual subject, OWLNamedIndividual object) {
//        Not implemented
    }

    @Override
    public void addRelation(IRI subject, IRI object) {
//        Not implemented
    }

    @Override
    public void removeComputedRelations(OWLNamedIndividual object) {
//        Not implemented
    }

    @Override
    public void removeComputedRelations(IRI object) {
//        Not implemented
    }
}

package com.nickrobison.trestle.reasoner.engines.events;

import com.nickrobison.trestle.types.events.TrestleEventType;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Set;

public class EventEngineNoOp implements TrestleEventEngine {
    private static final Logger logger = LoggerFactory.getLogger(EventEngineNoOp.class);

    public EventEngineNoOp() {
        logger.info("Events disabled, creating No-Op engine");
    }

    @Override
    public void addEvent(TrestleEventType event, OWLNamedIndividual individual, Temporal eventTemporal) {
//        Not implemented
    }

    @Override
    public void adjustObjectEvents(List<OWLDataPropertyAssertionAxiom> objectExistenceAxioms) {
//        Not implemented
    }

    @Override
    public void addSplitMergeEvent(TrestleEventType type, OWLNamedIndividual subject, Set<OWLNamedIndividual> objects, Temporal eventTemporal) {
//        Not implemented
    }
}

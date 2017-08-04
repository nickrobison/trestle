package com.nickrobison.trestle.reasoner.events;

import com.nickrobison.trestle.types.TrestleEvent;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.Temporal;
import java.util.List;

public class EventEngineNoOp implements EventEngine {
    private static final Logger logger = LoggerFactory.getLogger(EventEngineNoOp.class);

    public EventEngineNoOp() {
        logger.info("Events disabled, creating No-Op engine");
    }

    @Override
    public void addEvent(TrestleEvent event, OWLNamedIndividual individual, Temporal eventTemporal) {
//        Not implemented
    }

    @Override
    public void adjustObjectEvents(List<OWLDataPropertyAssertionAxiom> objectExistenceAxioms) {
//        Not implemented
    }
}

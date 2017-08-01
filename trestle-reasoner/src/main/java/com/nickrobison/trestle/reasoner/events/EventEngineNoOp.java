package com.nickrobison.trestle.reasoner.events;

import com.nickrobison.trestle.types.TrestleEvent;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.Temporal;

public class EventEngineNoOp implements EventEngine {
    private static final Logger logger = LoggerFactory.getLogger(EventEngineNoOp.class);

    public EventEngineNoOp() {
        logger.info("Events disabled, creating No-Op engine");
    }

    @Override
    public void addEvent(TrestleEvent event, OWLNamedIndividual individual, Temporal eventTemporal) {
//        Not implemented
    }
}

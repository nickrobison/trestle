package com.nickrobison.trestle.reasoner.events;

import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TrestleEvent;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;

import static com.nickrobison.trestle.common.StaticIRI.temporalExistsAtIRI;
import static com.nickrobison.trestle.common.StaticIRI.trestleEventIRI;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

public class EventEngineImpl implements EventEngine {
    private static final Logger logger = LoggerFactory.getLogger(EventEngineImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private final ITrestleOntology ontology;
    private final String prefix;

    @Inject
    public EventEngineImpl(ITrestleOntology ontology, @Named("reasonerPrefix") String prefix) {
        logger.info("Creating Event Engine");
        this.ontology = ontology;
        this.prefix = prefix;
    }

    @Override
    public void addEvent(TrestleEvent event, OWLNamedIndividual individual, Temporal eventTemporal) {
        logger.debug("Adding event {} to {} at {}", event, individual, eventTemporal);
        switch (event) {
            case CREATED:
                addCreatedDeletedEvent(TrestleEvent.CREATED, individual, eventTemporal);
                break;
            case DESTROYED:
                addCreatedDeletedEvent(TrestleEvent.DESTROYED, individual, eventTemporal);
                break;
        }
    }

    private void addCreatedDeletedEvent(TrestleEvent event, OWLNamedIndividual individual, Temporal eventTemporal) {
//        Create the axioms
        final OWLNamedIndividual eventID = df.getOWLNamedIndividual(IRI.create(this.prefix, String.format("%s:%s_event", individual.toStringID(), event.getShortName())));
        final OWLClassAssertionAxiom classAxiom = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleEventIRI), eventID);
        final OWLDataPropertyAssertionAxiom existsAtAxiom = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                individual,
                df.getOWLLiteral(parseTemporalToOntologyDateTime(eventTemporal, ZoneOffset.UTC).toString(), OWL2Datatype.XSD_DATE_TIME));
        final OWLObjectPropertyAssertionAxiom objectAssertion = df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(event.getIRI()),
                individual,
                eventID);
//        Open the transaction
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
//            Create the new event
            this.ontology.createIndividual(classAxiom);
//            Add the data property
            this.ontology.writeIndividualDataProperty(existsAtAxiom);
            this.ontology.writeIndividualObjectProperty(objectAssertion);
//            Write the object relation
        } catch (MissingOntologyEntity missingOntologyEntity) {
            logger.error("Missing ontology entity", missingOntologyEntity);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }
}

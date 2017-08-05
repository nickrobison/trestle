package com.nickrobison.trestle.reasoner.events;

import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.events.TrestleEventType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;

import static com.nickrobison.trestle.common.StaticIRI.*;

public class EventEngineImpl implements EventEngine {
    private static final Logger logger = LoggerFactory.getLogger(EventEngineImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final String prefix;

    @Inject
    public EventEngineImpl(ITrestleOntology ontology, QueryBuilder qb, @Named("reasonerPrefix") String prefix) {
        logger.info("Creating Event Engine");
        this.ontology = ontology;
        this.prefix = prefix;
        this.qb = qb;
    }

    @Override
    public void addEvent(TrestleEventType event, OWLNamedIndividual individual, Temporal eventTemporal) {
        logger.debug("Adding event {} to {} at {}", event, individual, eventTemporal);
        switch (event) {
            case CREATED:
                addCreatedDestroyedEvent(TrestleEventType.CREATED, individual, eventTemporal);
                break;
            case DESTROYED:
                addCreatedDestroyedEvent(TrestleEventType.DESTROYED, individual, eventTemporal);
                break;
        }
    }

    @Override
    public void adjustObjectEvents(List<OWLDataPropertyAssertionAxiom> objectExistenceAxioms) {
        List<OWLDataPropertyAssertionAxiom> eventAxioms = new ArrayList<>();
        objectExistenceAxioms.forEach(axiom -> {
//            Move the creation event?
            if (axiom.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsFromIRI)) {
                logger.debug("Adjusting {} event for {} to {}", TrestleEventType.CREATED, axiom.getSubject(), axiom.getObject().toString());
                eventAxioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                        buildEventName(axiom.getSubject().asOWLNamedIndividual(), TrestleEventType.CREATED),
                        axiom.getObject()));
            } else if (axiom.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsToIRI)) {
                logger.debug("Adjusting {} event for {} to {}", TrestleEventType.DESTROYED, axiom.getSubject(), axiom.getObject().toString());
                eventAxioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                        buildEventName(axiom.getSubject().asOWLNamedIndividual(), TrestleEventType.DESTROYED),
                        axiom.getObject()));
            }
//            Write the properties
            final String updateQuery = this.qb.updateObjectProperties(eventAxioms);
            this.ontology.executeUpdateSPARQL(updateQuery);
        });
    }

    private void addCreatedDestroyedEvent(TrestleEventType event, OWLNamedIndividual individual, Temporal eventTemporal) {
//        Create the axioms
        final OWLNamedIndividual eventID = buildEventName(individual, event);
        final OWLClassAssertionAxiom classAxiom = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleEventIRI), eventID);
        final OWLDataPropertyAssertionAxiom existsAtAxiom = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                eventID,
                TemporalParser.temporalToLiteral(eventTemporal));
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

    private OWLNamedIndividual buildEventName(OWLNamedIndividual individual, TrestleEventType event) {
        return df.getOWLNamedIndividual(IRI.create(this.prefix, String.format("%s:%s:event", IRIUtils.extractTrestleIndividualName(individual.getIRI()), event.getShortName())));
    }

}

package com.nickrobison.trestle.reasoner.engines.events;

import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
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
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseToTemporal;

public class EventEngineImpl implements TrestleEventEngine {
    private static final Logger logger = LoggerFactory.getLogger(EventEngineImpl.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final String prefix;

    @Inject
    public EventEngineImpl(ITrestleOntology ontology,
                           QueryBuilder qb,
                           @ReasonerPrefix String prefix) {
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
                addTemporalEvent(TrestleEventType.CREATED, individual, eventTemporal);
                break;
            case DESTROYED:
                addTemporalEvent(TrestleEventType.DESTROYED, individual, eventTemporal);
                break;
            default:
                throw new IllegalArgumentException("Only CREATED or DESTROYED events are supported");
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
                        TrestleEventEngine.buildEventName(df, this.prefix, axiom.getSubject().asOWLNamedIndividual(), TrestleEventType.CREATED),
                        axiom.getObject()));
            } else if (axiom.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsToIRI)) {
                logger.debug("Adjusting {} event for {} to {}", TrestleEventType.DESTROYED, axiom.getSubject(), axiom.getObject().toString());
                eventAxioms.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                        TrestleEventEngine.buildEventName(df, this.prefix, axiom.getSubject().asOWLNamedIndividual(), TrestleEventType.DESTROYED),
                        axiom.getObject()));
            }
//            Write the properties
            final String updateQuery = this.qb.updateObjectProperties(eventAxioms, trestleEventIRI);
            this.ontology.executeUpdateSPARQL(updateQuery);
        });
    }

    @Override
    public void addSplitMergeEvent(TrestleEventType type, OWLNamedIndividual subject, Set<OWLNamedIndividual> objects, Temporal eventTemporal) {
        logger.debug("Adding event {} to {} at {} with {}", type, subject, eventTemporal, objects);
        switch (type) {
            case MERGED:
                this.addMergedEvent(subject, objects, eventTemporal);
                break;
            case SPLIT:
                this.addSplitEvent(subject, objects, eventTemporal);
                break;
            default:
                throw new IllegalArgumentException("Only SPLIT and MERGED events are supported");
        }
    }

    private void addMergedEvent(OWLNamedIndividual subject, Set<OWLNamedIndividual> objects, Temporal eventTemporal) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            final OWLNamedIndividual eventName = TrestleEventEngine.buildEventName(df, this.prefix, subject, TrestleEventType.MERGED);
//            Get the start temporal of the subject
//            final Temporal fromTemporal = this.extractTrestleObjectTemporal(subject, temporalExistsFromIRI);
//            Write the new event
            addTemporalEvent(TrestleEventType.MERGED, subject, eventTemporal);
//            Add the components to the event
            for (OWLNamedIndividual object : objects) {
                this.ontology.writeIndividualObjectProperty(object, componentOfIRI, eventName);
            }
        } catch (MissingOntologyEntity e) {
            logger.error("Missing Individual {}", e.getIndividual(), e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    private void addSplitEvent(OWLNamedIndividual subject, Set<OWLNamedIndividual> objects, Temporal eventTemporal) {
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            final OWLNamedIndividual eventName = TrestleEventEngine.buildEventName(df, this.prefix, subject, TrestleEventType.SPLIT);
//            Get the start temporal of the subject
//            final Temporal fromTemporal = this.extractTrestleObjectTemporal(subject, temporalExistsToIRI);
//            Write the new event
            addTemporalEvent(TrestleEventType.SPLIT, subject, eventTemporal);
//            Add the components to the event
            for (OWLNamedIndividual object : objects) {
                this.ontology.writeIndividualObjectProperty(object, componentOfIRI, eventName);
            }
        } catch (MissingOntologyEntity e) {
            logger.error("Missing Individual {}", e.getIndividual(), e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            throw new TrestleEventException(df.getOWLNamedIndividual(e.getIndividual()), "Missing individual");
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    private void addTemporalEvent(TrestleEventType event, OWLNamedIndividual individual, Temporal eventTemporal) {
//        Create the axioms
        final OWLNamedIndividual eventID = TrestleEventEngine.buildEventName(df, this.prefix, individual, event);
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
        } catch (MissingOntologyEntity e) {
            logger.error("Missing ontology entity", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            throw new TrestleEventException(df.getOWLNamedIndividual(e.getIndividual()), "Missing individual");
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    /**
     * Get the temporal values from the specified individual
     * Will first attempt to read a {@link OWLDataProperty} corresponding to the provided {@link IRI} and will then read {@link com.nickrobison.trestle.common.StaticIRI#temporalExistsAtIRI} if no value is found
     * Note: Will only work if either the individual existed before the transaction started, or the isolation level is READ_UNCOMMITTED
     *
     * @param individual  - {@link OWLNamedIndividual} to get temporals from
     * @param temporalIRI - {@link IRI} of temporal value to start with
     * @return - {@link Temporal} corresponding to the value of the given {@link IRI} or {@link com.nickrobison.trestle.common.StaticIRI#temporalExistsAtIRI}
     */
    @SuppressWarnings({"squid:UnusedPrivateMethod"})
    // Suppressed because we may add this later, once we figure out isolation levels
    private Temporal extractTrestleObjectTemporal(OWLNamedIndividual individual, IRI temporalIRI) {
        final Set<OWLLiteral> existsFromProperty = this.ontology
                .getIndividualDataProperty(individual,
                        df.getOWLDataProperty(temporalIRI))
                .orElseGet(() -> this.ontology
                        .getIndividualDataProperty(individual, temporalExistsAtIRI)
                        .orElseThrow(() -> new IllegalStateException(String.format("Individual %s does not have existsFrom or ExistsTo Temporal", individual.toStringID()))));
//            We can just grab the first set member
//            TODO(nrobison): This should be either LocalDate or OffsetDateTime. In the future
        return parseToTemporal(existsFromProperty.stream().findFirst().orElseThrow(() -> new IllegalStateException(String.format("Individual %s does not have existsFrom or ExistsTo Temporal", individual.toStringID()))), OffsetDateTime.class);
    }

}

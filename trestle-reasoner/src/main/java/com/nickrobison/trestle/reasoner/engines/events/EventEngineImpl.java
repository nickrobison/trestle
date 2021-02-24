package com.nickrobison.trestle.reasoner.engines.events;

import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.events.TrestleEventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Supplier;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.*;

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
    public Completable addEvent(TrestleEventType event, OWLNamedIndividual individual, Temporal eventTemporal) {
        logger.debug("Adding event {} to {} at {}", event, individual, eventTemporal);
        switch (event) {
            case CREATED:
                return addTemporalEvent(TrestleEventType.CREATED, individual, eventTemporal);
            case DESTROYED:
                return addTemporalEvent(TrestleEventType.DESTROYED, individual, eventTemporal);
            default:
                return Completable.error(new IllegalArgumentException("Only CREATED or DESTROYED events are supported"));
        }
    }

    @Override
    public Completable adjustObjectEvents(List<OWLDataPropertyAssertionAxiom> objectExistenceAxioms) {
        return Observable.fromIterable(objectExistenceAxioms)
                .flatMapMaybe(axiom -> {
                    //            Move the creation event?
                    if (axiom.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsFromIRI)) {
                        logger.debug("Adjusting {} event for {} to {}", TrestleEventType.CREATED, axiom.getSubject(), axiom.getObject().toString());
                        return Maybe.just(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                                TrestleEventEngine.buildEventName(df, this.prefix, axiom.getSubject().asOWLNamedIndividual(), TrestleEventType.CREATED),
                                axiom.getObject()));
                    } else if (axiom.getProperty().asOWLDataProperty().getIRI().equals(temporalExistsToIRI)) {
                        logger.debug("Adjusting {} event for {} to {}", TrestleEventType.DESTROYED, axiom.getSubject(), axiom.getObject().toString());
                        return Maybe.just(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                                TrestleEventEngine.buildEventName(df, this.prefix, axiom.getSubject().asOWLNamedIndividual(), TrestleEventType.DESTROYED),
                                axiom.getObject()));
                    }
                    return Maybe.empty();
                })
                .toList()
                .flatMapCompletable(axiom -> {
//            Write the properties
                    final String updateQuery = this.qb.updateObjectProperties(axiom, trestleEventIRI);
                    return this.ontology.executeUpdateSPARQL(updateQuery);
                });
    }

    @Override
    public Completable addSplitMergeEvent(TrestleEventType type, OWLNamedIndividual subject, Set<OWLNamedIndividual> objects, Temporal eventTemporal) {
        logger.debug("Adding event {} to {} at {} with {}", type, subject, eventTemporal, objects);
        switch (type) {
            case MERGED:
                return this.addMergedEvent(subject, objects, eventTemporal);
            case SPLIT:
                return this.addSplitEvent(subject, objects, eventTemporal);
            default:
                return Completable.error(new IllegalArgumentException("Only SPLIT and MERGED events are supported"));
        }
    }

    private Completable addMergedEvent(OWLNamedIndividual subject, Set<OWLNamedIndividual> objects, Temporal eventTemporal) {

        final OWLNamedIndividual eventName = TrestleEventEngine.buildEventName(df, this.prefix, subject, TrestleEventType.MERGED);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);

        return addTemporalEvent(TrestleEventType.MERGED, subject, eventTemporal)
                .andThen(Completable.defer(() -> Observable.fromIterable(objects)
                        .flatMapCompletable(object -> this.ontology.writeIndividualObjectProperty(object, componentOfIRI, eventName))))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
    }

    private Completable addSplitEvent(OWLNamedIndividual subject, Set<OWLNamedIndividual> objects, Temporal eventTemporal) {
        final OWLNamedIndividual eventName = TrestleEventEngine.buildEventName(df, this.prefix, subject, TrestleEventType.SPLIT);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);

        return addTemporalEvent(TrestleEventType.SPLIT, subject, eventTemporal)
                .andThen(Completable.defer(() -> Observable.fromIterable(objects)
                        .flatMapCompletable(object -> this.ontology.writeIndividualObjectProperty(object, componentOfIRI, eventName))))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
    }

    private Completable addTemporalEvent(TrestleEventType event, OWLNamedIndividual individual, Temporal eventTemporal) {
//        Create the axioms
        final OWLNamedIndividual eventID = TrestleEventEngine.buildEventName(df, this.prefix, individual, event);
        final OWLClassAssertionAxiom classAxiom = df.getOWLClassAssertionAxiom(df.getOWLClass(trestleEventIRI), eventID);
        final OWLDataPropertyAssertionAxiom existsAtAxiom = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(temporalExistsAtIRI),
                eventID,
                TemporalParser.temporalToLiteral(eventTemporal));
        final OWLObjectPropertyAssertionAxiom objectAssertion = df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(event.getIRI()),
                individual,
                eventID);

//        Open the transaction (use one if it already exists, otherwise, we'll need to open a write transaction ourself
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        return this.ontology.createIndividual(classAxiom)
                .andThen(Completable.defer(() -> this.ontology.writeIndividualDataProperty(existsAtAxiom)))
                .andThen(Completable.defer(() -> this.ontology.writeIndividualObjectProperty(objectAssertion)))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
    }

    /**
     * Get the temporal values from the specified individual
     * Will first attempt to read a {@link OWLDataProperty} corresponding to the provided {@link IRI} and will then read {@link com.nickrobison.trestle.common.StaticIRI#temporalExistsAtIRI} if no value is found
     * Note: Will only work if either the individual existed before the transaction started, or the isolation level is READ_UNCOMMITTED
     *
     * @param individual  - {@link OWLNamedIndividual} to get temporals from
     * @param temporalIRI - {@link IRI} of temporal value to start with
     * @return - {@link io.reactivex.rxjava3.core.Single} {@link Temporal} corresponding to the value of the given {@link IRI} or {@link com.nickrobison.trestle.common.StaticIRI#temporalExistsAtIRI}
     */
    @SuppressWarnings({"squid:UnusedPrivateMethod"})
    // Suppressed because we may add this later, once we figure out isolation levels
    private Temporal extractTrestleObjectTemporal(OWLNamedIndividual individual, IRI temporalIRI) {
        final Set<OWLLiteral> existsFromProperty = Optional.of(this.ontology
                .getIndividualDataProperty(individual,
                        df.getOWLDataProperty(temporalIRI)).collect((Supplier<HashSet<OWLLiteral>>) HashSet::new, HashSet::add).blockingGet())
                .orElseGet(() -> Optional.of(this.ontology
                        .getIndividualDataProperty(individual, temporalExistsAtIRI).collect((Supplier<HashSet<OWLLiteral>>) HashSet::new, HashSet::add).blockingGet())
                        .orElseThrow(() -> new IllegalStateException(String.format("Individual %s does not have existsFrom or ExistsTo Temporal", individual.toStringID()))));
//            We can just grab the first set member
//            TODO(nrobison): This should be either LocalDate or OffsetDateTime. In the future
        return parseToTemporal(existsFromProperty.stream().findFirst().orElseThrow(() -> new IllegalStateException(String.format("Individual %s does not have existsFrom or ExistsTo Temporal", individual.toStringID()))), OffsetDateTime.class);
    }

}

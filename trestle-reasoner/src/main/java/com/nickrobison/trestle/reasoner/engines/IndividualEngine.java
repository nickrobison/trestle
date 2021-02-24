package com.nickrobison.trestle.reasoner.engines;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.common.exceptions.TrestleMissingFactException;
import com.nickrobison.trestle.reasoner.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.parser.ITypeConverter;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.reasoner.utils.TemporalPropertiesPair;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TrestleFact;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.TrestleRelation;
import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.CompletionException;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseToTemporal;

@SuppressWarnings("Duplicates")
@Metriced
public class IndividualEngine {

    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static final Logger logger = LoggerFactory.getLogger(IndividualEngine.class);
    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final ITypeConverter typeConverter;
    private final TrestleCache trestleCache;
    private final TrestleExecutorService individualThreadPool;
    private final TrestleExecutorService factThreadPool;


    @Inject
    public IndividualEngine(ITrestleOntology ontology,
                            QueryBuilder qb,
                            TrestleParser parser,
                            TrestleCache trestleCache,
                            TrestleExecutorFactory factory) {
        this.ontology = ontology;
        this.qb = qb;
        this.typeConverter = parser.typeConverter;
        this.trestleCache = trestleCache;
        individualThreadPool = factory.create("individual-pool");
        factThreadPool = factory.create("fact-pool");
    }


    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @return - {@link Single} {@link TrestleIndividual}
     */
    public Single<TrestleIndividual> getTrestleIndividual(String individualIRI) {
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(individualIRI)), null);
    }

    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @param transaction   - {@link TrestleTransaction} object to inherit from
     * @return - {@link Single} {@link TrestleIndividual}
     */
    public Single<TrestleIndividual> getTrestleIndividual(String individualIRI, @Nullable TrestleTransaction transaction) {
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(individualIRI)), transaction);
    }

    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individual - {@link OWLNamedIndividual}
     * @return - {@link Single} {@link TrestleIndividual}
     */
    public Single<TrestleIndividual> getTrestleIndividual(OWLNamedIndividual individual) {
        return getTrestleIndividual(individual, null);
    }


    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individual  - {@link OWLNamedIndividual}
     * @param transaction - {@link TrestleTransaction} object to inherit from
     * @return - {@link Single} {@link TrestleIndividual}
     */
    @Timed
    public Single<TrestleIndividual> getTrestleIndividual(OWLNamedIndividual individual, @Nullable TrestleTransaction transaction) {
        logger.debug("Building trestle individual {}", individual);
        @Nullable final TrestleIndividual cacheIndividual = this.trestleCache.getTrestleIndividual(individual);
        if (cacheIndividual != null) {
            logger.debug("Retrieved {} from cache", individual);
            return Single.just(cacheIndividual);
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(transaction, false);
        // Get the temporals
        final Single<TrestleIndividual> temporalSingle = ontology.getAllDataPropertiesForIndividual(individual).collect((Supplier<HashSet<OWLDataPropertyAssertionAxiom>>) HashSet::new, HashSet::add)
                .map(properties -> new TemporalPropertiesPair(individual, properties))
                .map(temporalPair -> TemporalObjectBuilder.buildTemporalFromProperties(temporalPair.getTemporalProperties(), null, temporalPair.getTemporalID()))
                .map(temporalObject -> new TrestleIndividual(individual.toStringID(), temporalObject.orElseThrow(() -> new CompletionException(new TrestleMissingIndividualException(individual)))));

        // Get the facts
        final Single<List<TrestleFact<Object>>> factSingle = ontology.getIndividualObjectProperty(individual, hasFactIRI)
                .flatMapSingle(fact -> buildTrestleFact(fact.getObject().asOWLNamedIndividual(), trestleTransaction)).toList();

        // Get the relationships
        String query = this.qb.buildIndividualRelationQuery(individual);
        final Single<List<TrestleRelation>> relationSingle = ontology.executeSPARQLResults(query)
                //                            We want the subProperties of Temporal/Spatial/Event relations. So we filter them out
                .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(temporalRelationIRI))
                .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(spatialRelationIRI))
                .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(eventRelationIRI))
                .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(componentRelationIRI))
//                            Filter out self
                .filter(result -> !result.unwrapIndividual("p").asOWLNamedIndividual().equals(individual))
                .map(result -> new TrestleRelation(result.unwrapIndividual("m").toStringID(),
                        ObjectRelation.getRelationFromIRI(IRI.create(result.unwrapIndividual("o").toStringID())),
                        result.unwrapIndividual("p").toStringID())).toList();

        // Get the events
        final String eQuery = this.qb.buildIndividualEventQuery(individual);
        final Single<List<TrestleEvent>> eventSingle = this.ontology.executeSPARQLResults(eQuery)
                .filter(result -> !result.unwrapIndividual("type").asOWLNamedIndividual().getIRI().equals(trestleEventIRI))
                .map(result -> {
                    final OWLNamedIndividual eventIndividual = result.unwrapIndividual("r").asOWLNamedIndividual();
                    final IRI typeIRI = result.unwrapIndividual("type").asOWLNamedIndividual().getIRI();
                    final TrestleEventType eventType = TrestleEventType.getEventClassFromIRI(typeIRI);
                    final Temporal temporal = parseToTemporal(result.unwrapLiteral("t"), OffsetDateTime.class);
                    return new TrestleEvent(eventType, individual, eventIndividual, temporal);
                })
                .toList();

        return Single.zip(temporalSingle, factSingle, relationSingle, eventSingle, (trestleIndividual, facts, relations, events) -> {
            facts.forEach(trestleIndividual::addFact);
            relations.forEach(trestleIndividual::addRelation);
            events.forEach(trestleIndividual::addEvent);
            return trestleIndividual;
        })
                .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
    }

    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     * @param clazz      - Class of object get get event for
     * @param individual - {@link OWLNamedIndividual} to gather events for
     * @return - {@link Flowable} of {@link TrestleEvent} for the given individual
     */
    public Flowable<TrestleEvent> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual) {
        return getIndividualEvents(clazz, individual, null);
    }


    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     * @param clazz       - Class of object get get event for
     * @param individual  - {@link OWLNamedIndividual} to gather events for
     * @param transaction - {@link TrestleTransaction} object to inherit from
     * @return - {@link Flowable} of {@link TrestleEvent} for the given individual
     */
    public Flowable<TrestleEvent> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual, @Nullable TrestleTransaction transaction) {

        final Class<? extends Temporal> temporalType = TemporalParser.getTemporalType(clazz);
        logger.debug("Retrieving events for {}", individual);
        //        Build the query string
        final String eventQuery = this.qb.buildIndividualEventQuery(individual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(transaction, false);

        return this.ontology.executeSPARQLResults(eventQuery)
                .filter(result -> !result
                        .unwrapIndividual("type")
                        .asOWLNamedIndividual()
                        .getIRI().equals(trestleEventIRI))
                .map(result -> {
                    final OWLNamedIndividual eventIndividual = result.unwrapIndividual("r").asOWLNamedIndividual();
                    final IRI eventTypeIRI = result.unwrapIndividual("type").asOWLNamedIndividual().getIRI();
                    final TrestleEventType eventType = TrestleEventType.getEventClassFromIRI(eventTypeIRI);
                    final Temporal temporal = parseToTemporal(result.unwrapLiteral("t"), temporalType);
                    return new TrestleEvent(eventType, individual, eventIndividual, temporal);
                })
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction));
    }


    /**
     * Build a TrestleFact from a given OWLIndividual
     * Retrieves all the asserted properties and types of a given Individual, in their native forms.
     *
     * @param factIndividual    - {@link OWLNamedIndividual} to construct fact from
     * @param transactionObject - {@link TrestleTransaction} object that gets passed from the parent function
     * @return - TrestleFact
     */
    private @NonNull Single<TrestleFact<Object>> buildTrestleFact(OWLNamedIndividual factIndividual, TrestleTransaction transactionObject) {
        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
        final @NonNull Flowable<OWLDataPropertyAssertionAxiom> propertiesFlow = ontology.getAllDataPropertiesForIndividual(factIndividual)
                // Share the data properties flow, but wait until everyone is subscribed, before emitting
                .publish()
                .autoConnect(3);

        // Build fact pair
        final @NonNull Single<OWLDataPropertyAssertionAxiom> valueFlow = propertiesFlow
                .filter(property -> !(property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalStartIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalEndIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalAtIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalPropertyIRI)))
                .firstOrError();

        // Get valid temporal object
        final Single<Optional<TemporalObject>> validTemporalFlow = propertiesFlow
                .filter(property -> property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI))
                .collect((Supplier<HashSet<OWLDataPropertyAssertionAxiom>>) HashSet::new, HashSet::add)
                .map(validTemporals -> TemporalObjectBuilder.buildTemporalFromProperties(validTemporals, null, "blank"));

        // Get DB temporal object
        final Single<Optional<TemporalObject>> dbTemporalFlow = propertiesFlow
                .filter(property -> property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI) ||
                        property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI))
                .collect((Supplier<HashSet<OWLDataPropertyAssertionAxiom>>) HashSet::new, HashSet::add)
                .map(validTemporals -> TemporalObjectBuilder.buildTemporalFromProperties(validTemporals, null, "blank"));

        return Single.zip(valueFlow, validTemporalFlow, dbTemporalFlow, (assertion, validTemporal, dbTemporal) -> {
            final Class<?> datatype = this.typeConverter.lookupJavaClassFromOWLDatatype(assertion, null);
            final Object literalObject = this.typeConverter.extractOWLLiteral(datatype, assertion.getObject());
            return new TrestleFact<>(
                    factIndividual.getIRI().toString(),
                    assertion.getProperty().asOWLDataProperty().getIRI().getShortForm(),
                    literalObject,
                    null,
                    null, validTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)),
                    dbTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)));
        })
                .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(tt))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(tt));
    }
}

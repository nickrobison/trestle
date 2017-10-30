package com.nickrobison.trestle.reasoner.engines;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.metrician.Metrician;
import com.nickrobison.trestle.common.exceptions.TrestleMissingFactException;
import com.nickrobison.trestle.common.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.reasoner.parser.TypeConverter;
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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.common.StaticIRI.componentRelationIRI;
import static com.nickrobison.trestle.common.StaticIRI.trestleEventIRI;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseToTemporal;

@SuppressWarnings("Duplicates")
@Metriced
public class IndividualEngine {

    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static final Logger logger = LoggerFactory.getLogger(IndividualEngine.class);
    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final TrestleCache trestleCache;
    private final TrestleExecutorService individualThreadPool;
    private final TrestleExecutorService factThreadPool;


    @Inject
    public IndividualEngine(ITrestleOntology ontology,
                            QueryBuilder qb,
                            TrestleCache trestleCache,
                            Metrician metrician) {
        final Config trestleConfig = ConfigFactory.load().getConfig("trestle");
        this.ontology = ontology;
        this.qb = qb;
        this.trestleCache = trestleCache;
        individualThreadPool = TrestleExecutorService.executorFactory("individual-pool",
                trestleConfig.getInt("threading.individual-pool.size"),
                metrician);
        factThreadPool = TrestleExecutorService.executorFactory("fact-pool",
                trestleConfig.getInt("threading.individual-pool.size"),
                metrician);
    }


    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @return - {@link TrestleIndividual}
     */
    public TrestleIndividual getTrestleIndividual(String individualIRI) {
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(individualIRI)), null);
    }

    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individualIRI - String of individual IRI
     * @param transaction   - {@link TrestleTransaction} object to inherit from
     * @return - {@link TrestleIndividual}
     */
    public TrestleIndividual getTrestleIndividual(String individualIRI, @Nullable TrestleTransaction transaction) {
        return getTrestleIndividual(df.getOWLNamedIndividual(parseStringToIRI(individualIRI)), transaction);
    }

    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individual - {@link OWLNamedIndividual}
     * @return - {@link TrestleIndividual}
     */
    public TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual) {
        return getTrestleIndividual(individual, null);
    }


    /**
     * Return a {@link TrestleIndividual} with all the available facts and properties
     * Attempts to retrieve from the cache, if enabled and present
     *
     * @param individual  - {@link OWLNamedIndividual}
     * @param transaction - {@link TrestleTransaction} object to inherit from
     * @return - {@link TrestleIndividual}
     */
    @Timed
    public TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual, @Nullable TrestleTransaction transaction) {

        logger.debug("Building trestle individual {}", individual);
        @Nullable final TrestleIndividual cacheIndividual = this.trestleCache.getTrestleIndividual(individual);
        if (cacheIndividual != null) {
            logger.debug("Retrieved {} from cache");
            return cacheIndividual;
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(transaction, false);

        final CompletableFuture<TrestleIndividual> temporalFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            final Set<OWLDataPropertyAssertionAxiom> individualDataProperties = ontology.getAllDataPropertiesForIndividual(individual);
            this.ontology.returnAndCommitTransaction(tt);
            return new TemporalPropertiesPair(individual, individualDataProperties);
        })
                .thenApply(temporalPair -> TemporalObjectBuilder.buildTemporalFromProperties(temporalPair.getTemporalProperties(), null, temporalPair.getTemporalID()))
                .thenApply(temporalObject -> new TrestleIndividual(individual.toStringID(), temporalObject.orElseThrow(() -> new CompletionException(new TrestleMissingIndividualException(individual)))));

//                Get all the facts
        final Optional<List<OWLObjectPropertyAssertionAxiom>> individualFacts = ontology.getIndividualObjectProperty(individual, hasFactIRI);
        final List<CompletableFuture<TrestleFact>> factFutureList = individualFacts.orElse(new ArrayList<>())
                .stream()
                .map(fact -> buildTrestleFact(fact.getObject().asOWLNamedIndividual(), trestleTransaction))
                .collect(Collectors.toList());

        CompletableFuture<List<TrestleFact>> factsFuture = sequenceCompletableFutures(factFutureList);

//                Get the relations
        final CompletableFuture<List<TrestleRelation>> relationsFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            String query = this.qb.buildIndividualRelationQuery(individual);
            try {
                return ontology.executeSPARQLResults(query);
            } catch (Exception e) {
                this.ontology.returnAndAbortTransaction(tt);
                throw new CompletionException(e.getCause());
            } finally {
                this.ontology.returnAndCommitTransaction(tt);
            }
        }, individualThreadPool)
                .thenApply(sparqlResults -> {
                    List<TrestleRelation> relations = new ArrayList<>();
                    sparqlResults.getResults()
                            .stream()
//                            We want the subProperties of Temporal/Spatial/Event relations. So we filter them out
                            .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(temporalRelationIRI))
                            .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(spatialRelationIRI))
                            .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(eventRelationIRI))
                            .filter(result -> !result.unwrapIndividual("o").asOWLNamedIndividual().getIRI().equals(componentRelationIRI))
//                            Filter out self
                            .filter(result -> !result.unwrapIndividual("p").asOWLNamedIndividual().equals(individual))
                            .forEach(result -> relations.add(new TrestleRelation(result.unwrapIndividual("m").toStringID(),
                                    ObjectRelation.getRelationFromIRI(IRI.create(result.unwrapIndividual("o").toStringID())),
                                    result.unwrapIndividual("p").toStringID())));
                    return relations;
                });

//        Get the events
        final CompletableFuture<List<TrestleEvent>> eventsFuture = CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
            final String query = this.qb.buildIndividualEventQuery(individual);
            try {
                return this.ontology.executeSPARQLResults(query);
            } catch (Exception e) {
                this.ontology.returnAndAbortTransaction(tt);
                throw new CompletionException(e.getCause());
            } finally {
                this.ontology.returnAndCommitTransaction(tt);
            }
        }, this.individualThreadPool)
                .thenApply(results -> {
                    List<TrestleEvent> events = new ArrayList<>();
                    return results.getResults()
                            .stream()
                            .filter(result -> !result.unwrapIndividual("type").asOWLNamedIndividual().getIRI().equals(trestleEventIRI))
                            .map(result -> {
                                final OWLNamedIndividual eventIndividual = result.unwrapIndividual("r").asOWLNamedIndividual();
                                final IRI typeIRI = result.unwrapIndividual("type").asOWLNamedIndividual().getIRI();
                                final TrestleEventType eventType = TrestleEventType.getEventClassFromIRI(typeIRI);
                                final Temporal temporal = parseToTemporal(result.unwrapLiteral("t"), OffsetDateTime.class);
                                return new TrestleEvent(eventType, individual, eventIndividual, temporal);
                            })
                            .collect(Collectors.toList());
                });

        final CompletableFuture<TrestleIndividual> individualFuture = temporalFuture.thenCombine(relationsFuture, (trestleIndividual, relations) -> {
            relations.forEach(trestleIndividual::addRelation);
            return trestleIndividual;
        })
                .thenCombine(factsFuture, (trestleIndividual, trestleFacts) -> {
                    trestleFacts.forEach(trestleIndividual::addFact);
                    return trestleIndividual;
                })
                .thenCombine(eventsFuture, (trestleIndividual, events) -> {
                    events.forEach(trestleIndividual::addEvent);
                    return trestleIndividual;
                });

        try {
            TrestleIndividual trestleIndividual = individualFuture.get();
            try {
                this.trestleCache.writeTrestleIndividual(individual, trestleIndividual);
            } catch (Exception e) {
                logger.error("Unable to write Trestle Individual {} to cache", individual, e);
            }
            return trestleIndividual;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Interruption exception building Trestle Individual {}", individual, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            throw new RuntimeException(e);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     * @param clazz      - Class of object get get event for
     * @param individual - {@link OWLNamedIndividual} to gather events for
     * @return - {@link Optional} {@link Set} of {@link TrestleEvent} for the given individual
     */
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual) {
        return getIndividualEvents(clazz, individual, null);
    }


    /**
     * Get all {@link TrestleEvent} for the given individual
     *
     * @param clazz       - Class of object get get event for
     * @param individual  - {@link OWLNamedIndividual} to gather events for
     * @param transaction - {@link TrestleTransaction} object to inherit from
     * @return - {@link Optional} {@link Set} of {@link TrestleEvent} for the given individual
     */
    public Optional<Set<TrestleEvent>> getIndividualEvents(Class<?> clazz, OWLNamedIndividual individual, @Nullable TrestleTransaction transaction) {

        final Class<? extends Temporal> temporalType = TemporalParser.getTemporalType(clazz);

        logger.debug("Retrieving events for {}", individual);
        //        Build the query string
        final String eventQuery = this.qb.buildIndividualEventQuery(individual);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(transaction, false);
        final TrestleResultSet resultSet;
        try {
            resultSet = this.ontology.executeSPARQLResults(eventQuery);
        } catch (Exception e) {
            logger.error("Unable to get events for individual: {}", individual);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
//        Parse out the events
//        I think I can suppress this, because if the above method throws an error, the catch statement will return an empty optional
        @SuppressWarnings({"dereference.of.nullable"}) final List<TrestleResult> results = resultSet.getResults();
        final Set<TrestleEvent> individualEvents = results
                .stream()
//                Filter out Trestle_Event from results
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
                .collect(Collectors.toSet());

        return Optional.of(individualEvents);
    }


    /**
     * Build a TrestleFact from a given OWLIndividual
     * Retrieves all the asserted properties and types of a given Individual, in their native forms.
     *
     * @param factIndividual    - OWLNamedIndividual to construct fact from
     * @param transactionObject - TrestleTransaction object that gets passed from the parent function
     * @return - TrestleFact
     */
    private CompletableFuture<TrestleFact> buildTrestleFact(OWLNamedIndividual factIndividual, TrestleTransaction transactionObject) {
        return CompletableFuture.supplyAsync(() -> {
            final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(transactionObject);
            try {
                return ontology.getAllDataPropertiesForIndividual(factIndividual);
            } finally {
                this.ontology.returnAndCommitTransaction(tt);
            }

        }, factThreadPool)
                .thenApply(dataProperties -> {
                    //            Build fact pair
                    final Optional<OWLDataPropertyAssertionAxiom> dataPropertyAssertion = dataProperties
                            .stream()
                            .filter(property -> !(property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalStartIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalEndIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalAtIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalPropertyIRI)))
                            .findFirst();

                    final OWLDataPropertyAssertionAxiom assertion = dataPropertyAssertion.orElseThrow(() -> new TrestleMissingFactException(factIndividual));
                    final Class<?> datatype = TypeConverter.lookupJavaClassFromOWLDatatype(assertion, null);
                    final Object literalObject = TypeConverter.extractOWLLiteral(datatype, assertion.getObject());
//            Get valid time
                    final Set<OWLDataPropertyAssertionAxiom> validTemporals = dataProperties
                            .stream()
                            .filter(property -> property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidFromIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidToIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalValidAtIRI))
                            .collect(Collectors.toSet());
                    final Optional<TemporalObject> validTemporal = TemporalObjectBuilder.buildTemporalFromProperties(validTemporals, null, "blank");
//            Database time
                    final Set<OWLDataPropertyAssertionAxiom> dbTemporals = dataProperties
                            .stream()
                            .filter(property -> property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseFromIRI) ||
                                    property.getProperty().asOWLDataProperty().getIRI().equals(temporalDatabaseToIRI))
                            .collect(Collectors.toSet());
                    final Optional<TemporalObject> dbTemporal = TemporalObjectBuilder.buildTemporalFromProperties(dbTemporals, null, "blank");
                    return new TrestleFact<>(
                            factIndividual.getIRI().toString(),
                            assertion.getProperty().asOWLDataProperty().getIRI().getShortForm(),
                            literalObject,
                            validTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)),
                            dbTemporal.orElseThrow(() -> new TrestleMissingFactException(factIndividual)));
                });
    }
}

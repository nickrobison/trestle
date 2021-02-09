package com.nickrobison.trestle.reasoner.engines.collection;

import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.object.ObjectEngineUtils;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngineUtils;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.relations.CollectionRelationType;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Supplier;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.uom.SI;

import javax.inject.Inject;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.*;

import static com.nickrobison.trestle.common.IRIUtils.extractTrestleIndividualName;
import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.StaticIRI.*;
import static com.nickrobison.trestle.reasoner.parser.TemporalParser.parseTemporalToOntologyDateTime;

/**
 * Created by nickrobison on 2/19/18.
 */
public class CollectionEngine implements ITrestleCollectionEngine {
    private static final Logger logger = LoggerFactory.getLogger(CollectionEngine.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private final String reasonerPrefix;
    private final ITrestleOntology ontology;
    private final QueryBuilder qb;
    private final ITrestleObjectReader objectReader;
    private final ITrestleObjectWriter objectWriter;
    private final IClassParser classParser;
    private final ObjectEngineUtils objectUtils;
    private final TrestleExecutorService collectionPool;

    @Inject
    public CollectionEngine(@ReasonerPrefix String reasonerPrefix,
                            ITrestleOntology ontology,
                            QueryBuilder queryBuilder,
                            ITrestleObjectReader objectReader,
                            ITrestleObjectWriter objectWriter,
                            TrestleParser trestleParser,
                            ObjectEngineUtils objectUtils,
                            TrestleExecutorFactory factory) {

        this.reasonerPrefix = reasonerPrefix;
        this.ontology = ontology;
        this.qb = queryBuilder;
        this.objectReader = objectReader;
        this.objectWriter = objectWriter;
        this.classParser = trestleParser.classParser;
        this.objectUtils = objectUtils;

        this.collectionPool = factory.create("collection-pool");
    }

    @Override
    public Flowable<String> getCollections() {
        return this.ontology.getInstances(df.getOWLClass(trestleCollectionIRI), true)
                .map(OWLIndividual::toStringID);
    }

    @Override
    public Single<Map<String, List<String>>> getRelatedCollections(String individual, @Nullable String collectionID, double relationStrength) {
        final String collectionQuery;
        final OWLNamedIndividual owlIndividual = df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, individual));
        if (collectionID != null) {
            collectionQuery = this.qb.buildCollectionRetrievalQuery(
                    owlIndividual,
                    df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, collectionID)),
                    relationStrength);
        } else {
            collectionQuery = this.qb.buildCollectionRetrievalQuery(
                    owlIndividual,
                    null,
                    relationStrength);
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        return this.ontology.executeSPARQLResults(collectionQuery)

                .map(result -> Pair.of(
                        result.getIndividual("collection").orElseThrow(() -> new RuntimeException("collection is null")).toStringID(),
                        result.getIndividual("individual").orElseThrow(() -> new RuntimeException("individual is null")).toStringID()
                ))
                .groupBy(Pair::getLeft, Pair::getRight)
                .flatMapSingle(group -> group.toList().map(lst -> Pair.of(group.getKey(), lst)))
                .collect((Supplier<Map<String, List<String>>>) HashMap::new, (map, vals) -> map.put(vals.getLeft(), vals.getRight()))
                .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));

//
//        try {
//            final List<TrestleResult> resultSet = this.ontology.executeSPARQLResults(collectionQuery).toList().blockingGet();
//            resultSet
//                    .forEach(result -> collectionIndividuals.put(result.getIndividual("collection").orElseThrow(() -> new RuntimeException("collection is null")).toStringID(), result.getIndividual("individual").orElseThrow(() -> new RuntimeException("individual is null")).toStringID()));
//
//            if (collectionIndividuals.keySet().size() == 0) {
//                logger.info("Individual {} has no related collections", individual);
//                return Optional.empty();
//            }
//            return Optional.of(Multimaps.asMap(collectionIndividuals));
//        } catch (RuntimeException e) {
//            logger.error("Problem getting collections related to individual: {}", individual, e);
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            return Optional.empty();
//        } finally {
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        }
    }

    @Override
    public Flowable<String> STIntersectCollection(String wkt, double buffer, double strength, Temporal validAt, @Nullable Temporal dbAt) {
        return STIntersectCollection(wkt, buffer, SI.METRE, strength, validAt, dbAt);
    }

    @Override
    public Flowable<String> STIntersectCollection(String wkt, double buffer, Unit<Length> bufferUnit, double strength, Temporal validAt, @Nullable Temporal dbAt) {
        final String queryString;
        final OffsetDateTime atTemporal;
        final OffsetDateTime dbTemporal;
        if (validAt == null) {
            atTemporal = null;
        } else {
            atTemporal = parseTemporalToOntologyDateTime(validAt, ZoneOffset.UTC);
        }
        if (dbAt == null) {
            dbTemporal = OffsetDateTime.now();
        } else {
            dbTemporal = parseTemporalToOntologyDateTime(dbAt, ZoneOffset.UTC);
        }

//        Apply buffer
        final String wktBuffer = SpatialEngineUtils.addWKTBuffer(wkt, buffer, bufferUnit);

        try {
            queryString = qb.buildTemporalSpatialCollectionIntersection(wktBuffer, strength, atTemporal, dbTemporal);
        } catch (UnsupportedFeatureException e) { // Why should we ever throw this?
            logger.error("Database does not support spatial queries");
            return Flowable.error(e);
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        return this.ontology.executeSPARQLResults(queryString)
                .map(result -> result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID())
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
//
//        try {
//            final List<TrestleResult> resultSet = this.ontology.executeSPARQLResults(queryString).toList().blockingGet();
//            final Set<String> intersectedCollectionURIs = resultSet
//                    .stream()
//                    .map(result -> )
//                    .collect(Collectors.toSet());
//            return Optional.of(intersectedCollectionURIs);
//        } catch (RuntimeException e) {
//            logger.error("Problem intersecting spatial collection", e);
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            return Optional.empty();
//        } finally {
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        }
    }

    @Override
    public @NonNull <T> Flowable<T> getCollectionMembers(Class<T> clazz, String collectionID, double strength, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection) {


        final OWLClass datasetClass = this.classParser.getObjectClass(clazz);
        final IRI collectionIRI = parseStringToIRI(this.reasonerPrefix, collectionID);
        final String retrievalStatement = qb.buildCollectionObjectRetrieval(datasetClass, collectionIRI, strength);

        final OffsetDateTime atTemporal;
        if (temporalIntersection != null) {
            atTemporal = parseTemporalToOntologyDateTime(temporalIntersection, ZoneOffset.UTC);
        } else {
            atTemporal = OffsetDateTime.now();
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);

        //noinspection OptionalGetWithoutIsPresent
        return this.ontology.executeSPARQLResults(retrievalStatement)
                .map(result -> result.getIndividual("m"))
                .filter(Optional::isPresent)
                .map(individual -> individual.get().toStringID())
                .flatMap(iri -> this.objectUtils.getAdjustedQueryTemporal(iri, atTemporal, trestleTransaction)
                        .flatMapPublisher(adjustedIntersection -> this.objectReader.readTrestleObject(clazz, iri, adjustedIntersection, null).toFlowable()))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
//
//        try {
//            Set<String> individualIRIs = this.ontology.executeSPARQLResults(retrievalStatement).toList().blockingGet()
//                    .stream()
//                    .map(result -> result.getIndividual("m"))
//                    .filter(Optional::isPresent)
//                    .map(individual -> individual.get().toStringID())
//                    .collect(Collectors.toSet());
//
////        Try to retrieve the object members in an async fashion
////        We need to figure out the exists time of each object, so if the intersection point comes after the exists interval of the object, we grab the latest version of that object. Likewise intersection -> before -> object, grab the earliest
//            final List<CompletableFuture<T>> completableFutureList = individualIRIs
//                    .stream()
//                    .map(iri -> CompletableFuture.supplyAsync(() -> {
//                        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
//                        try {
//                            final Temporal adjustedIntersection = this.objectUtils.getAdjustedQueryTemporal(iri, atTemporal, tt).blockingGet();
//                            return this.objectReader.readTrestleObject(clazz, iri, adjustedIntersection, null).blockingGet();
//                        } catch (MissingOntologyEntity e) {
//                            logger.error("Cannot find ontology individual {}", e.getIndividual(), e);
//                            this.ontology.returnAndAbortTransaction(tt);
//                            throw new CompletionException(e);
//                        } catch (TrestleClassException e) {
//                            logger.error("Unregistered class", e);
//                            this.ontology.returnAndAbortTransaction(tt);
//                            throw new CompletionException(e);
//                        } finally {
//                            this.ontology.returnAndCommitTransaction(tt);
//                        }
//                    }, this.collectionPool))
//                    .collect(Collectors.toList());
//            final CompletableFuture<List<T>> collectionObjectsFuture = sequenceCompletableFutures(completableFutureList);
//            List<T> objects = collectionObjectsFuture.get();
//            return Optional.of(objects);
//        } catch (InterruptedException e) {
//            logger.error("Object retrieval for collection {}, interrupted", collectionID, e.getCause());
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            Thread.currentThread().interrupt();
//            return Optional.empty();
//        } catch (ExecutionException e) {
//            logger.error("Unable to retrieve all objects for collection {}", collectionID, e.getCause());
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            return Optional.empty();
//        } finally {
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        }
//        return null;
    }

    @Override
    public Completable addObjectToCollection(String collectionIRI, Object inputObject, CollectionRelationType relationType, double strength) {

        //        Create the collection relation
        final IRI collection = parseStringToIRI(this.reasonerPrefix, collectionIRI);
        final OWLNamedIndividual collectionIndividual = df.getOWLNamedIndividual(collection);
        final OWLNamedIndividual individual = this.classParser.getIndividual(inputObject);
        final IRI relationIRI = IRI.create(this.reasonerPrefix, String.format("relation:%s:%s",
                extractTrestleIndividualName(collection),
                extractTrestleIndividualName(individual.getIRI())));
        final OWLNamedIndividual relationIndividual = df.getOWLNamedIndividual(relationIRI);
        final OWLClass relationClass = df.getOWLClass(trestleRelationIRI);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        try {
            return this.objectWriter.writeTrestleObject(inputObject) //        Write the object
                    //        Write the collection properties
                    .andThen(Completable.defer(() -> ontology.createIndividual(df.getOWLClassAssertionAxiom(relationClass, relationIndividual))))
                    //        Write the relation to the collection
                    .andThen(Completable.defer(() -> ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                            df.getOWLObjectProperty(relationOfIRI),
                            relationIndividual,
                            individual))))
                    .andThen(Completable.defer(() -> ontology.writeIndividualDataProperty(relationIndividual,
                            df.getOWLDataProperty(relationStrengthIRI),
                            df.getOWLLiteral(strength))))
                    .andThen(Completable.defer(() -> ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                            df.getOWLObjectProperty(relatedToIRI),
                            relationIndividual,
                            collectionIndividual
                    ))))
                    //            If the collection doesn't exist, create it.
//                    .andThen(Completable.defer(() -> ontology.createIndividual(df.getOWLClassAssertionAxiom(df.getOWLClass(trestleCollectionIRI), collectionIndividual))))
                    .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                    .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));

        } catch (Exception e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Completable.error(e);
        }

//        try {

//            this.objectWriter.writeTrestleObject(inputObject).blockingAwait();
////        TODO(nrobison): Implement relation types
////        switch (relationType) {
////            case SEMANTIC:
////                relationClass = df.getOWLClass(semanticRelationIRI);
////                break;
////            case SPATIAL:
////                relationClass = df.getOWLClass(spatialRelationIRI);
////                break;
////            case TEMPORAL:
////                relationClass = df.getOWLClass(temporalRelationIRI);
////                break;
////            default:
////                relationClass = df.getOWLClass(trestleCollectionIRI);
////                break;
////        }
////        Write the collection properties
//            ontology.createIndividual(df.getOWLClassAssertionAxiom(relationClass, relationIndividual)).blockingAwait();
//            ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
//                    df.getOWLObjectProperty(relationOfIRI),
//                    relationIndividual,
//                    individual)).blockingAwait();
//            ontology.writeIndividualDataProperty(relationIndividual,
//                    df.getOWLDataProperty(relationStrengthIRI),
//                    df.getOWLLiteral(strength)).blockingAwait();
//
////        Write the relation to the collection
////            TODO(nrobison): This is gross, catching exceptions is really expensive.
//            try {
//                ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
//                        df.getOWLObjectProperty(relatedToIRI),
//                        relationIndividual,
//                        collectionIndividual
//                )).blockingAwait();
//            } catch (Exception e) { // I think this can actually be removed, not sure it's ever called
////            If the collection doesn't exist, create it.
//                ontology.createIndividual(df.getOWLClassAssertionAxiom(df.getOWLClass(trestleCollectionIRI), collectionIndividual)).blockingAwait();
////            Try again
//                ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
//                        df.getOWLObjectProperty(relatedToIRI),
//                        relationIndividual,
//                        collectionIndividual)).blockingAwait();
//            }
//        } catch (MissingOntologyEntity | TrestleClassException e) {
//            logger.error("Problem adding individual {} to collection {}", individual, collectionIndividual, e);
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//        } finally {
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        }
    }

    @Override
    public Completable removeCollection(String collectionIRI) {
        final IRI collection = parseStringToIRI(this.reasonerPrefix, collectionIRI);
        final OWLNamedIndividual collectionIndividual = df.getOWLNamedIndividual(collection);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        return this.ontology.getIndividualObjectProperty(collectionIndividual, relatedByIRI)
                .flatMapCompletable(relation -> this.removeRelation(relation.getObject().asOWLNamedIndividual(),
                        relation.getSubject().asOWLNamedIndividual(), null))
                .andThen(Completable.defer(() -> this.ontology.removeIndividual(collectionIndividual)))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
//        try {
////            Get all the relations
//            final Optional<List<OWLObjectPropertyAssertionAxiom>> relationProperties = Optional.of(this.ontology.getIndividualObjectProperty(collectionIndividual, relatedByIRI).toList().blockingGet());
//            //                Remove each of the relations
//            relationProperties.ifPresent(owlObjectPropertyAssertionAxioms -> owlObjectPropertyAssertionAxioms
//                    .forEach(relation -> {
//                        this.removeRelation(relation.getObject().asOWLNamedIndividual(),
//                                relation.getSubject().asOWLNamedIndividual(), trestleTransaction);
//                    }));
//
////            Remove the collection
//            this.ontology.removeIndividual(collectionIndividual).blockingAwait();
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        } catch (Exception e) {
//            this.ontology.returnAndAbortTransaction(trestleTransaction);
//            throw e;
//        }
    }

    @Override
    public Completable removeObjectFromCollection(String collectionIRI, Object inputObject, boolean removeEmptyCollection) {
//        Remove the relation
        final IRI collection = parseStringToIRI(this.reasonerPrefix, collectionIRI);
        final OWLNamedIndividual collectionIndividual = df.getOWLNamedIndividual(collection);
        final OWLNamedIndividual individual = this.classParser.getIndividual(inputObject);
        final IRI relationIRI = IRI.create(this.reasonerPrefix, String.format("relation:%s:%s",
                extractTrestleIndividualName(collection),
                extractTrestleIndividualName(individual.getIRI())));
        final OWLNamedIndividual relationIndividual = df.getOWLNamedIndividual(relationIRI);

        logger.debug("Removing {} from {}", individual, collectionIndividual);
        final TrestleTransaction emptyTransaction = this.ontology.createandOpenNewTransaction(true);
        return this.removeRelation(relationIndividual, collectionIndividual, null)
                .andThen(Completable.defer(() -> {
                    if (!removeEmptyCollection) {
                        return Completable.complete();
                    }
                    // Do the removal in the final transaction
                    return this.ontology.getIndividualObjectProperty(collection, relatedByIRI)
                            .toList()
                            .flatMapCompletable(properties -> {
                                if (properties.isEmpty()) {
                                    logger.debug("{} is empty, removing", collectionIndividual);
                                    return this.ontology.removeIndividual(collectionIndividual);
                                }
                                logger.debug("{} properties remaining for {}, no removing", properties.size(), collectionIndividual);
                                return Completable.complete();
                            });
                }))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(emptyTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(emptyTransaction));

//
//        if (removeEmptyCollection) {
//
//
//            try {
//                logger.debug("Removing {}, if empty", collectionIndividual);
////                Get related by relations and see if we need if any are left
//                final Optional<List<OWLObjectPropertyAssertionAxiom>> relatedProperties = Optional.of(this.ontology.getIndividualObjectProperty(collection, relatedByIRI).toList().blockingGet());
//
//                //noinspection ConstantConditions - We will remove this soon
//                if (relatedProperties.isEmpty()) {
//                    logger.debug("{} is empty, removing", collectionIndividual);
//                    this.ontology.removeIndividual(collectionIndividual).blockingAwait();
//                }
//                this.ontology.returnAndCommitTransaction(emptyTransaction);
//            } catch (Exception e) {
//                this.ontology.returnAndAbortTransaction(emptyTransaction);
//                throw e;
//            }
//        }
    }

    @Override
    public Single<Boolean> collectionsAreAdjacent(String subjectCollectionID, String objectCollectionID, double strength) {
        final IRI iri1 = parseStringToIRI(this.reasonerPrefix, subjectCollectionID);
        final OWLNamedIndividual matchingIndividual = df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, objectCollectionID));

        final String adjacentQuery = this.qb.buildAdjecentCollectionQuery(df.getOWLNamedIndividual(iri1), strength);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        return this.ontology.executeSPARQLResults(adjacentQuery)
                .map(result -> result.unwrapIndividual("collection"))
                .any(collection -> collection.equals(matchingIndividual))
                .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(trestleTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(trestleTransaction));
//
//
//        try {
//            final List<TrestleResult> trestleResultSet = this.ontology.executeSPARQLResults(adjacentQuery).toList().blockingGet();
//            return trestleResultSet
//                    .stream()
//                    .map(result -> result.unwrapIndividual("collection"))
//                    .anyMatch(collection -> collection.equals(matchingIndividual));
//        } finally {
//            this.ontology.returnAndCommitTransaction(trestleTransaction);
//        }
    }

    /**
     * Remove the Trestle_Relation associated with the given Trestle_Collection
     *
     * @param relation           - {@link OWLNamedIndividual} Trestle_Relation
     * @param collection         - {@link OWLNamedIndividual} Trestle_Collection
     * @param trestleTransaction - {@link TrestleTransaction} optional transaction to use
     * @return {@link Completable} when finished
     */
    private Completable removeRelation(OWLNamedIndividual relation, OWLNamedIndividual collection, @Nullable TrestleTransaction trestleTransaction) {

        final TrestleTransaction removeTransaction = this.ontology.createandOpenNewTransaction(trestleTransaction);
        // Remove all the relationship properties
        return this.ontology.removeIndividualObjectProperty(relation, df.getOWLObjectProperty(relatedToIRI), collection)
                .andThen(Completable.defer(() -> this.ontology.removeIndividualObjectProperty(relation, df.getOWLObjectProperty(relationOfIRI), null)))
                .andThen(Completable.defer(() -> this.ontology.removeIndividualDataProperty(relation, df.getOWLDataProperty(relationStrengthIRI), null)))
                .andThen(Completable.defer(() -> this.ontology.removeIndividual(relation)))
                .doOnComplete(() -> this.ontology.returnAndCommitTransaction(removeTransaction))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(removeTransaction));
//        final Completable relationOf = this.ontology.removeIndividualObjectProperty(relation, df.getOWLObjectProperty(relationOfIRI), null);
//        final Completable strength = this.ontology.removeIndividualDataProperty(relation, df.getOWLDataProperty(relationStrengthIRI), null);

        // Once the relations are removed, remove the individual as well.
//        return relatedTo


//        try {
////            Remove the from and two relations
//            this.ontology.removeIndividualObjectProperty(relation, df.getOWLObjectProperty(relatedToIRI), collection).blockingAwait();
//            this.ontology.removeIndividualObjectProperty(relation, df.getOWLObjectProperty(relationOfIRI), null).blockingAwait();
////            Remove the strength
//            this.ontology.removeIndividualDataProperty(relation, df.getOWLDataProperty(relationStrengthIRI), null).blockingAwait();
////            Remove the individual
//            this.ontology.removeIndividual(relation).blockingAwait();
//            this.ontology.returnAndCommitTransaction(removeTransaction);
//        } catch (Exception e) {
//            this.ontology.returnAndAbortTransaction(removeTransaction);
//        }
    }
}

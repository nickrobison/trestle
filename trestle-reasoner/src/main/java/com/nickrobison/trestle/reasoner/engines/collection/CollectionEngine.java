package com.nickrobison.trestle.reasoner.engines.collection;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectWriter;
import com.nickrobison.trestle.reasoner.engines.object.ObjectEngineUtils;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngineUtils;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.relations.CollectionRelationType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.extractTrestleIndividualName;
import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.common.LambdaUtils.sequenceCompletableFutures;
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
    public Optional<Map<String, List<String>>> getRelatedCollections(String individual, @Nullable String collectionID, double relationStrength) {
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
        ListMultimap<String, String> collectionIndividuals = ArrayListMultimap.create();

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(collectionQuery);
            resultSet.getResults()
                    .forEach(result -> collectionIndividuals.put(result.getIndividual("collection").orElseThrow(() -> new RuntimeException("collection is null")).toStringID(), result.getIndividual("individual").orElseThrow(() -> new RuntimeException("individual is null")).toStringID()));

            if (collectionIndividuals.keySet().size() == 0) {
                logger.info("Individual {} has no related collections", individual);
                return Optional.empty();
            }
            return Optional.of(Multimaps.asMap(collectionIndividuals));
        } catch (RuntimeException e) {
            logger.error("Problem getting collections related to individual: {}", individual, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public Optional<Set<String>> STIntersectCollection(String wkt, double buffer, double strength, Temporal validAt, @Nullable Temporal dbAt) {
        return STIntersectCollection(wkt, buffer, SI.METER, strength, validAt, dbAt);
    }

    @Override
    public Optional<Set<String>> STIntersectCollection(String wkt, double buffer, Unit<Length> bufferUnit, double strength, Temporal validAt, @Nullable Temporal dbAt) {
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
        } catch (UnsupportedFeatureException e) {
            logger.error("Database does not support spatial queries");
            return Optional.empty();
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(queryString);
            final Set<String> intersectedCollectionURIs = resultSet.getResults()
                    .stream()
                    .map(result -> result.getIndividual("m").orElseThrow(() -> new RuntimeException("individual is null")).toStringID())
                    .collect(Collectors.toSet());
            return Optional.of(intersectedCollectionURIs);
        } catch (RuntimeException e) {
            logger.error("Problem intersecting spatial collection", e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public <T> Optional<List<T>> getCollectionMembers(Class<T> clazz, String collectionID, double strength, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection) {


        final OWLClass datasetClass = this.classParser.getObjectClass(clazz);
        final String retrievalStatement = qb.buildCollectionObjectRetrieval(datasetClass, parseStringToIRI(this.reasonerPrefix, collectionID), strength);

        final OffsetDateTime atTemporal;
        if (temporalIntersection != null) {
            atTemporal = parseTemporalToOntologyDateTime(temporalIntersection, ZoneOffset.UTC);
        } else {
            atTemporal = OffsetDateTime.now();
        }

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        Set<String> individualIRIs = this.ontology.executeSPARQLResults(retrievalStatement)
                .getResults()
                .stream()
                .map(result -> result.getIndividual("m"))
                .filter(Optional::isPresent)
                .map(individual -> individual.get().toStringID())
                .collect(Collectors.toSet());

//        Try to retrieve the object members in an async fashion
//        We need to figure out the exists time of each object, so if the intersection point comes after the exists interval of the object, we grab the latest version of that object. Likewise intersection -> before -> object, grab the earliest
        final List<CompletableFuture<T>> completableFutureList = individualIRIs
                .stream()
                .map(iri -> CompletableFuture.supplyAsync(() -> {
                    final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                    try {
                        final Temporal adjustedIntersection = this.objectUtils.getAdjustedQueryTemporal(iri, atTemporal, tt);
                        return this.objectReader.readTrestleObject(clazz, iri, adjustedIntersection, null);
                    } catch (MissingOntologyEntity e) {
                        logger.error("Cannot find ontology individual {}", e.getIndividual(), e);
                        this.ontology.returnAndAbortTransaction(tt);
                        throw new CompletionException(e);
                    } catch (TrestleClassException e) {
                        logger.error("Unregistered class", e);
                        this.ontology.returnAndAbortTransaction(tt);
                        throw new CompletionException(e);
                    } finally {
                        this.ontology.returnAndCommitTransaction(tt);
                    }
                }, this.collectionPool))
                .collect(Collectors.toList());
        final CompletableFuture<List<T>> collectionObjectsFuture = sequenceCompletableFutures(completableFutureList);
        try {
            List<T> objects = collectionObjectsFuture.get();
            return Optional.of(objects);
        } catch (InterruptedException e) {
            logger.error("Object retrieval for collection {}, interrupted", collectionID, e.getCause());
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            logger.error("Unable to retrieve all objects for collection {}", collectionID, e.getCause());
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public void addObjectToCollection(String collectionIRI, Object inputObject, CollectionRelationType relationType, double strength) {

        //        Create the collection relation
        final IRI collection = parseStringToIRI(this.reasonerPrefix, collectionIRI);
        final OWLNamedIndividual collectionIndividual = df.getOWLNamedIndividual(collection);
        final OWLNamedIndividual individual = this.classParser.getIndividual(inputObject);
        final IRI relationIRI = IRI.create(String.format("relation:%s:%s",
                extractTrestleIndividualName(collection),
                extractTrestleIndividualName(individual.getIRI())));
        final OWLNamedIndividual relationIndividual = df.getOWLNamedIndividual(relationIRI);
        final OWLClass relationClass = df.getOWLClass(trestleRelationIRI);
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);

        try {
            //        Write the object
            this.objectWriter.writeTrestleObject(inputObject);
//        TODO(nrobison): Implement relation types
//        switch (relationType) {
//            case SEMANTIC:
//                relationClass = df.getOWLClass(semanticRelationIRI);
//                break;
//            case SPATIAL:
//                relationClass = df.getOWLClass(spatialRelationIRI);
//                break;
//            case TEMPORAL:
//                relationClass = df.getOWLClass(temporalRelationIRI);
//                break;
//            default:
//                relationClass = df.getOWLClass(trestleCollectionIRI);
//                break;
//        }
//        Write the collection properties
            ontology.createIndividual(df.getOWLClassAssertionAxiom(relationClass, relationIndividual));
            ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                    df.getOWLObjectProperty(relationOfIRI),
                    relationIndividual,
                    individual));
            ontology.writeIndividualDataProperty(relationIndividual,
                    df.getOWLDataProperty(relationStrengthIRI),
                    df.getOWLLiteral(strength));

//        Write the relation to the collection
//            TODO(nrobison): This is gross, catching exceptions is really expensive.
            try {
                ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(relatedToIRI),
                        relationIndividual,
                        collectionIndividual
                ));
            } catch (MissingOntologyEntity missingOntologyEntity) {
//            If the collection doesn't exist, create it.
                logger.debug("Missing collection {}, creating", missingOntologyEntity.getIndividual());
                ontology.createIndividual(df.getOWLClassAssertionAxiom(df.getOWLClass(trestleCollectionIRI), collectionIndividual));
//            Try again
                ontology.writeIndividualObjectProperty(df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(relatedToIRI),
                        relationIndividual,
                        collectionIndividual));
            }
        } catch (MissingOntologyEntity | TrestleClassException e) {
            logger.error("Problem adding individual {} to collection {}", individual, collectionIndividual, e);
            this.ontology.returnAndAbortTransaction(trestleTransaction);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    @Override
    public boolean collectionsAreAdjacent(String subjectCollectionID, String objectCollectionID, double strength) {
        final IRI iri1 = parseStringToIRI(this.reasonerPrefix, subjectCollectionID);
        final OWLNamedIndividual matchingIndividual = df.getOWLNamedIndividual(parseStringToIRI(this.reasonerPrefix, objectCollectionID));

        final String adjacentQuery = this.qb.buildAdjecentCollectionQuery(df.getOWLNamedIndividual(iri1), strength);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            final TrestleResultSet trestleResultSet = this.ontology.executeSPARQLResults(adjacentQuery);
            return trestleResultSet
                    .getResults()
                    .stream()
                    .map(result -> result.unwrapIndividual("collection"))
                    .anyMatch(collection -> collection.equals(matchingIndividual));
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }
}

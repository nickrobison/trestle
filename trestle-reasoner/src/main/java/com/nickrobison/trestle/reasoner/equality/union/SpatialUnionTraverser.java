package com.nickrobison.trestle.reasoner.equality.union;

import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.spatialUnionIRI;
import static com.nickrobison.trestle.common.StaticIRI.trestleObjectIRI;

public class SpatialUnionTraverser {

    private static final Logger logger = LoggerFactory.getLogger(SpatialUnionTraverser.class);
    private static final String TEMPORALS_ERROR = "Cannot get temporals for object";
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private enum TemporalDirection {
        FORWARD,
        BACKWARD
    }

    private final ITrestleOntology ontology;
    private final QueryBuilder qb;

    @Inject
    public SpatialUnionTraverser(ITrestleOntology ontology) {
        this.ontology = ontology;
        this.qb = ontology.getUnderlyingQueryBuilder();
    }

    /**
     * Traverse spatial union to determine individuals that are equivalent to the given {@link OWLNamedIndividual} at the given query {@link Temporal}
     * If no individuals satisfy the equality constraints, an empty {@link List} is returned
     *
     * @param clazz         - {@link Class} of type {@link T} of individual
     * @param subject       - {@link OWLNamedIndividual} to determine equality of
     * @param queryTemporal - {@link Temporal} traverse union to given time
     * @param <T>           - Generic type parameter
     * @return - {@link List} of {@link OWLNamedIndividual} that are equivalent to the given individual at the specified query time
     */
    public <T extends @NonNull Object> List<OWLNamedIndividual> traverseUnion(Class<T> clazz, OWLNamedIndividual subject, Temporal queryTemporal) {
        return traverseUnion(clazz, Collections.singletonList(subject), queryTemporal);
    }


    /**
     * Traverse spatial union to determine individuals that are equivalent to the given {@link List} of {@link OWLNamedIndividual} at the given query {@link Temporal}
     * If no individuals satisfy the equality constraints, an empty {@link List} is returned
     *
     * @param clazz         - {@link Class} of type {@link T} of individual
     * @param subject       - {@link List} of {@link OWLNamedIndividual} to determine equality of
     * @param queryTemporal - {@link Temporal} traverse union to given time
     * @param <T>           - Generic type parameter
     * @return - {@link List} of {@link OWLNamedIndividual} that are equivalent to the given individual at the specified query time
     */
    public <T extends @NonNull Object> List<OWLNamedIndividual> traverseUnion(Class<T> clazz, List<OWLNamedIndividual> subjects, Temporal queryTemporal) {

//        Get the base temporal type of the input class
        final Class<? extends Temporal> temporalType = TemporalParser.getTemporalType(clazz);

//        Setup the sets to hold the various objects we encounter
        final Set<STObjectWrapper> seenObjects = new HashSet<>();
        final Queue<STObjectWrapper> invalidObjects;
        final Set<STObjectWrapper> validObjects = new HashSet<>();

//        Start the transaction
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
//            TemporalDirection temporalDirection = null;
            Set<STObjectWrapper> stObjects = new HashSet<>();
            for (OWLNamedIndividual subject : subjects) {
                //        Get the existence temporals for the object
                final Set<OWLDataPropertyAssertionAxiom> individualExistenceProperties = this.ontology.getAllDataPropertiesForIndividual(subject);
                final Optional<TemporalObject> temporals = TemporalObjectBuilder.buildTemporalFromProperties(individualExistenceProperties, temporalType, null, null);
                final STObjectWrapper stObject = new STObjectWrapper(subject, trestleObjectIRI, temporals.orElseThrow(() -> new IllegalStateException(TEMPORALS_ERROR)));
                stObjects.add(stObject);
            }

            final Optional<TemporalDirection> temporalDirection = determineQueryDirection(stObjects, queryTemporal);
            if (!temporalDirection.isPresent()) {
                logger.debug("All objects are valid at {}, returning self-set", queryTemporal);
                return subjects;
            }
            logger.debug("Executing query in the {} direction", temporalDirection.get());
            if (temporalDirection.get() == TemporalDirection.FORWARD) {
                invalidObjects = new PriorityQueue<>(forwardComparator);
            } else {
                invalidObjects = new PriorityQueue<>(backwardComparator);
            }
            seenObjects.addAll(stObjects);
            final Set<STObjectWrapper> currentInvalidObjects = getInvalidObjects(stObjects, queryTemporal);
            invalidObjects.addAll(currentInvalidObjects);
            final Set<STObjectWrapper> currentValidObjects = new HashSet<>(stObjects);
            currentValidObjects.removeAll(currentInvalidObjects);
            validObjects.addAll(currentValidObjects);

            final Set<STObjectWrapper> equivalence = getEquivalence(validObjects, invalidObjects, seenObjects, queryTemporal, temporalDirection.get());
            if (!equivalence.isEmpty()) {
                logger.debug("Found equivalence: {}", equivalence);
            }
            return equivalence.stream().map(STObjectWrapper::getIndividual).collect(Collectors.toList());
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    /**
     * Recursive function to return a {@link Set} of {@link STObjectWrapper} that represent the {@link OWLNamedIndividual}s equivalent to the input Set
     *
     * @param validObjects      - {@link Set} of {@link STObjectWrapper} of valid objects that make up the equivalence set
     * @param invalidObjects    - {@link Queue} of {@link STObjectWrapper} representing invalid objects that could contain additional valid objects
     * @param seenObjects       - {@link Set} of {@link STObjectWrapper} of objects that have already been seen by the equivalence algorithm
     * @param queryTemporal     - {@link Temporal} query temporal
     * @param temporalDirection - {@link TemporalDirection} determining whether the algorithm is moving forwards or backwards in time
     * @return - {@link Set} of {@link STObjectWrapper} that are equivalent to the first object in the {@link Queue} or represent all valid objects for the input set
     */
    private Set<STObjectWrapper> getEquivalence(Set<STObjectWrapper> validObjects, Queue<STObjectWrapper> invalidObjects, Set<STObjectWrapper> seenObjects, Temporal queryTemporal, TemporalDirection temporalDirection) {
//        This is a null safe way of checking that the queue is empty
        final STObjectWrapper object = invalidObjects.poll();
        if (object == null) {
            return validObjects;
        }

        final Set<STObjectWrapper> eqObjects = executeEqualityQuery(object, temporalDirection, seenObjects);
        if (eqObjects.isEmpty()) {
            return new HashSet<>();
        }

        seenObjects.addAll(eqObjects);

        final Set<STObjectWrapper> currentInvalidObjects = getInvalidObjects(eqObjects, queryTemporal);
        invalidObjects.addAll(currentInvalidObjects);
//        This is to make Error Prone happy, but this feels terrible
        final Set<STObjectWrapper> collect = eqObjects
                .stream()
                .filter(eqObj -> !currentInvalidObjects.contains(eqObj))
                .collect(Collectors.toCollection(() -> validObjects));

        if (invalidObjects.isEmpty()) {
            return validObjects;
        }

        return getEquivalence(validObjects, invalidObjects, seenObjects, queryTemporal, temporalDirection);
    }

    /**
     * Execute equality query to determine the {@link Set} of {@link STObjectWrapper} that are equivalent to the given {@link STObjectWrapper} input object
     *
     * @param inputObject - {@link STObjectWrapper} to determine equality of
     * @param direction   - {@link TemporalDirection} whether the query is moving forwards or backwards in time
     * @param seenObjects - {@link Set} of {@link STObjectWrapper} of objects that have already been processed by the algorithm
     * @return - {@link Set} of {@link STObjectWrapper} objects that are equivalent to the given input object
     */
    private Set<STObjectWrapper> executeEqualityQuery(STObjectWrapper inputObject, TemporalDirection direction, Set<STObjectWrapper> seenObjects) {
        logger.trace("Executing equality query for {}", inputObject.getIndividual());
        Set<STObjectWrapper> equivalentObjects = new HashSet<>();
        final String equivalenceQuery = this.qb.buildSTEquivalenceQuery(inputObject.getIndividual());
        final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(equivalenceQuery);
        final Set<STObjectWrapper> queryObjects = resultSet
                .getResults()
                .stream()
//                Build the STObjectWrapper
                .map(SpatialUnionTraverser::extractSTObjectWrapperFromResults)
//                Filter out self object
                .filter(object -> !object.getIndividual().equals(inputObject.getIndividual()))
//                Filter out objects that are pointed in the wrong direction
                .filter(object -> {
                    if (direction == TemporalDirection.FORWARD) {
                        return object.getExistenceTemporal().compareTo(inputObject.getExistenceTemporal().getIdTemporal()) != -1;
                    } else {
                        return object.getExistenceTemporal().compareTo(inputObject.getExistenceTemporal().getIdTemporal()) != 1;
                    }
                })
                .collect(Collectors.toSet());

        for (STObjectWrapper queryObject : queryObjects) {
//            Unions first
            if (queryObject.getType().equals(spatialUnionIRI)) {
                logger.trace("{} is a union", queryObject.getIndividual());
                if (isCompleteUnion(queryObject, seenObjects)) {
                    logger.trace("{} is a complete union", queryObject.getIndividual());
                    final Set<STObjectWrapper> unionEqualities = executeEqualityQuery(queryObject, direction, seenObjects);
                    if (unionEqualities.isEmpty()) {
                        return new HashSet<>();
                    }
                    equivalentObjects.addAll(unionEqualities);
                    seenObjects.addAll(unionEqualities);
                } else {
                    logger.trace("{} is not a complete union", queryObject.getIndividual());
                    return new HashSet<>();
                }
            }

            equivalentObjects.add(queryObject);
        }

        return equivalentObjects;
    }

    /**
     * Determines is the given {@link STObjectWrapper} representing a SpatialUnion has an object that have not been seen yet
     * If so, it's not a complete union, and we can't do anything with it
     *
     * @param union        {@link STObjectWrapper} SpatialUnion object
     * @param seenObjects- {@link Set} of {@link STObjectWrapper} of seen objects
     * @return - {@code true} is complete union (we have everything), {@code false} is not a complete union, we need more info
     */
    private boolean isCompleteUnion(STObjectWrapper union, Set<STObjectWrapper> seenObjects) {
        final String unionQuery = this.qb.buildSTUnionComponentQuery(union.getIndividual());
        final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(unionQuery);
        final Optional<STObjectWrapper> hasUnseenObjects = resultSet.getResults()
                .stream()
                .map(SpatialUnionTraverser::extractSTObjectWrapperFromResults)
                .filter(object -> !seenObjects.contains(object))
                .findAny();
        return !hasUnseenObjects.isPresent();
    }

    /**
     * Class specific method for building an {@link STObjectWrapper} object from a {@link TrestleResult}
     * Expects the following query variables: ?object, ?start, ?end (Optional), ?type (Optional)
     *
     * @param result - {@link TrestleResult} result to parse
     * @return - {@link STObjectWrapper}
     */
    private static STObjectWrapper extractSTObjectWrapperFromResults(TrestleResult result) {
        return new STObjectWrapper(result.unwrapIndividual("object").asOWLNamedIndividual(),
                result.getIndividual("type").orElse(df.getOWLNamedIndividual(trestleObjectIRI)).asOWLNamedIndividual().getIRI(),
                TemporalObjectBuilder.buildTemporalFromResults(TemporalScope.EXISTS,
                        Optional.empty(),
                        result.getLiteral("start"),
                        result.getLiteral("end")).orElseThrow(() -> new IllegalStateException(TEMPORALS_ERROR)));
    }

    /**
     * Filter input object set to return objects that are before or after the query {@link Temporal}
     *
     * @param inputObjects - {@link Set} of {@link STObjectWrapper}
     * @param queryDate    - {@link Temporal} query date
     * @return - {@link Set} of {@link STObjectWrapper} that exist before or after the query {@link Temporal}
     */
    private Set<STObjectWrapper> getInvalidObjects(Set<STObjectWrapper> inputObjects, Temporal queryDate) {
        return inputObjects
                .stream()
                .filter(object -> object.getExistenceTemporal().compareTo(queryDate) != 0)
                .collect(Collectors.toSet());
    }

    /**
     * Determine which direction to execute the traversal query
     * If all the objects exist at the queryTemporal point, then we return an empty optional
     * throws {@link IllegalStateException} if some of the objects are pointed in the wrong direction
     *
     * @param inputObjects  - {@link Set} of {@link STObjectWrapper} representing intitial input objects
     * @param queryTemporal - {@link Temporal} query temporal
     * @return - {@link Optional} {@link TemporalDirection} if not all of the objects exist at the query point
     */
    private static Optional<TemporalDirection> determineQueryDirection(Set<STObjectWrapper> inputObjects, Temporal queryTemporal) {
        TemporalDirection direction = null;
        for (STObjectWrapper inputObject : inputObjects) {
            final int comparison = inputObject.getExistenceTemporal().compareTo(queryTemporal);
            if (direction != null && (direction == TemporalDirection.FORWARD && comparison == 1) || (direction == TemporalDirection.BACKWARD && comparison == -1)) {
                throw new IllegalStateException("Input objects have different temporal directions");
            } else if (direction == null && comparison == 1) {
                direction = TemporalDirection.BACKWARD;
            } else if (direction == null && comparison == -1) {
                direction = TemporalDirection.FORWARD;
            }
        }

        return Optional.ofNullable(direction);
    }

    static Comparator<STObjectWrapper> forwardComparator = (object1, object2) -> TemporalUtils.compareTemporals(object1.getExistenceTemporal().getIdTemporal(), object2.getExistenceTemporal().getIdTemporal());

    static Comparator<STObjectWrapper> backwardComparator = (object1, object2) -> TemporalUtils.compareTemporals(object2.getExistenceTemporal().getIdTemporal(), object1.getExistenceTemporal().getIdTemporal());
}

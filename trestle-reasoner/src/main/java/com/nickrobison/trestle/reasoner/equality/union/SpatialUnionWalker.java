package com.nickrobison.trestle.reasoner.equality.union;

import com.nickrobison.trestle.common.TemporalUtils;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.plugin.dom.exception.InvalidStateException;

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.spatialUnionIRI;
import static com.nickrobison.trestle.common.StaticIRI.trestleObjectIRI;

public class SpatialUnionWalker {

    private static final Logger logger = LoggerFactory.getLogger(SpatialUnionWalker.class);
    private static final String TEMPORALS_ERROR = "Cannot get temporals for object";
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private enum TemporalDirection {
        FORWARD,
        BACKWARD
    }

    private final ITrestleOntology ontology;
    private final QueryBuilder qb;

    public SpatialUnionWalker(ITrestleOntology ontology) {
        this.ontology = ontology;
        this.qb = ontology.getUnderlyingQueryBuilder();
    }

    public <T> void traverseUnion(Class<T> clazz, OWLNamedIndividual subject, Temporal queryTemporal) {
        traverseUnion(clazz, Collections.singletonList(subject), queryTemporal);
    }


    public <T> void traverseUnion(Class<T> clazz, List<OWLNamedIndividual> subjects, Temporal queryTemporal) {

        final Set<STObjectWrapper> seenObjects = new HashSet<>();
        final Queue<STObjectWrapper> invalidObjects;
        final Set<STObjectWrapper> validObjects = new HashSet<>();
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
            TemporalDirection temporalDirection = null;
            Set<STObjectWrapper> stObjects = new HashSet<>();
            for (OWLNamedIndividual subject : subjects) {
                //        Get the existence temporals for the object
                final Set<OWLDataPropertyAssertionAxiom> individualExistenceProperties = this.ontology.getAllDataPropertiesForIndividual(subject);
                final Optional<TemporalObject> temporals = TemporalObjectBuilder.buildTemporalFromProperties(individualExistenceProperties, OffsetDateTime.class, null, null);
                final STObjectWrapper stObject = new STObjectWrapper(subject, trestleObjectIRI, temporals.orElseThrow(() -> new IllegalStateException(TEMPORALS_ERROR)));
                stObjects.add(stObject);
                final TemporalDirection currentTemporalDirection;
//                If the query temporal is before the object temporal, then we need to go backwards in time
                if (TemporalUtils.compareTemporals(queryTemporal, stObject.getExistenceTemporal().getIdTemporal()) == -1) {
                    currentTemporalDirection = TemporalDirection.BACKWARD;
                } else {
                    currentTemporalDirection = TemporalDirection.FORWARD;
                }

                if (temporalDirection == null) {
                    temporalDirection = currentTemporalDirection;
                } else if (currentTemporalDirection != temporalDirection) {
                    throw new IllegalStateException("Input objects have opposing temporal directions");
                }
            }
            if (temporalDirection == TemporalDirection.FORWARD) {
                invalidObjects = new PriorityQueue<>(ForwardComparator);
            } else {
                invalidObjects = new PriorityQueue<>(BackwardComparator);
            }
            seenObjects.addAll(stObjects);
            final Set<STObjectWrapper> currentInvalidObjects = getInvalidObjects(stObjects, queryTemporal);
            invalidObjects.addAll(currentInvalidObjects);
            final HashSet<STObjectWrapper> currentValidObjects = new HashSet<>(stObjects);
            currentValidObjects.removeAll(currentInvalidObjects);
            validObjects.addAll(currentValidObjects);

            @Nullable final Set<STObjectWrapper> equivalence = getEquivalence(validObjects, invalidObjects, seenObjects, queryTemporal, temporalDirection);
            logger.debug("Found equivalence:", equivalence);
        } finally {
            this.ontology.returnAndCommitTransaction(trestleTransaction);
        }
    }

    private @Nullable Set<STObjectWrapper> getEquivalence(Set<STObjectWrapper> validObjects, Queue<STObjectWrapper> invalidObjects, Set<STObjectWrapper> seenObjects, Temporal queryTemporal, TemporalDirection temporalDirection) {
        if (invalidObjects.isEmpty()) {
            return validObjects;
        }

        final STObjectWrapper object = invalidObjects.poll();

        @Nullable final Set<STObjectWrapper> eqObjects = executeEqualityQuery(object, temporalDirection, seenObjects);
        if (eqObjects == null || eqObjects.isEmpty()) {
            return null;
        }

        seenObjects.addAll(eqObjects);

        final Set<STObjectWrapper> currentInvalidObjects = getInvalidObjects(eqObjects, queryTemporal);
        invalidObjects.addAll(currentInvalidObjects);
        eqObjects
                .stream()
                .filter(eqObj -> !currentInvalidObjects.contains(eqObj))
                .collect(Collectors.toCollection(() -> validObjects));

        if (invalidObjects.isEmpty()) {
            return validObjects;
        }

        return getEquivalence(validObjects, invalidObjects, seenObjects, queryTemporal, temporalDirection);
    }

    private @Nullable Set<STObjectWrapper> executeEqualityQuery(STObjectWrapper inputObject, TemporalDirection direction, Set<STObjectWrapper> seenObjects) {
        Set<STObjectWrapper> equivalentObjects = new HashSet<>();
        final String equivalenceQuery = this.qb.buildSTEquivalenceQuery(inputObject.getIndividual());
        final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(equivalenceQuery);
        final Set<STObjectWrapper> queryObjects = resultSet
                .getResults()
                .stream()
//                Build the STObjectWrapper
                .map(SpatialUnionWalker::extractSTObjectWrapperFromResults)
//                Filter out self object
                .filter(object -> !object.getIndividual().equals(inputObject.getIndividual()))
//                Filter out objects that are pointed in the wrong direction
                .filter(object -> {
                    if (direction.equals(TemporalDirection.FORWARD)) {
                        return object.getExistenceTemporal().compareTo(inputObject.getExistenceTemporal().getIdTemporal()) == 1;
                    } else {
                        return object.getExistenceTemporal().compareTo(inputObject.getExistenceTemporal().getIdTemporal()) == -1;
                    }
                })
                .collect(Collectors.toSet());

        for (STObjectWrapper queryObject : queryObjects) {
//            Unions first
            if (queryObject.getType().equals(spatialUnionIRI)) {
                if (isCompleteUnion(queryObject, seenObjects)) {
                    final Set<STObjectWrapper> unionEqualities = executeEqualityQuery(queryObject, direction, seenObjects);
                    if (unionEqualities == null || unionEqualities.isEmpty()) {
                        return null;
                    }
                    equivalentObjects.addAll(unionEqualities);
                    seenObjects.addAll(unionEqualities);
                } else {
                    return null;
                }
            }

            equivalentObjects.add(queryObject);
        }

        return equivalentObjects;
    }

    private boolean isCompleteUnion(STObjectWrapper union, Set<STObjectWrapper> seenObjects) {
        final String unionQuery = this.qb.buildSTUnionComponentQuery(union.getIndividual());
        final TrestleResultSet resultSet = this.ontology.executeSPARQLResults(unionQuery);
        final Optional<STObjectWrapper> hasUnseenObjects = resultSet.getResults()
                .stream()
                .map(SpatialUnionWalker::extractSTObjectWrapperFromResults)
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
                        result.getLiteral("end")).orElseThrow(() -> new InvalidStateException(TEMPORALS_ERROR)));
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

    static Comparator<STObjectWrapper> ForwardComparator = (object1, object2) -> TemporalUtils.compareTemporals(object1.getExistenceTemporal().getIdTemporal(), object2.getExistenceTemporal().getIdTemporal());

    static Comparator<STObjectWrapper> BackwardComparator = (object1, object2) -> TemporalUtils.compareTemporals(object2.getExistenceTemporal().getIdTemporal(), object1.getExistenceTemporal().getIdTemporal());
}

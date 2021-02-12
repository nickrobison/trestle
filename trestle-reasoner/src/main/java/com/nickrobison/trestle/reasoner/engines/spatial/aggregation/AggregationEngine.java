package com.nickrobison.trestle.reasoner.engines.spatial.aggregation;

import com.nickrobison.trestle.common.LambdaUtils;
import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.reasoner.engines.object.ITrestleObjectReader;
import com.nickrobison.trestle.reasoner.engines.spatial.SpatialEngineUtils;
import com.nickrobison.trestle.reasoner.exceptions.NoValidStateException;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.ITypeConverter;
import com.nickrobison.trestle.reasoner.parser.TemporalParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorFactory;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.locationtech.jts.geom.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;
import static com.nickrobison.trestle.reasoner.engines.object.TrestleObjectReader.MISSING_FACT_ERROR;

/**
 * Created by nickrobison on 3/24/18.
 */
@SuppressWarnings("TypeParameterExplicitlyExtendsObject")
public class AggregationEngine {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private final String reasonerPrefix;
    private final ITrestleObjectReader reader;
    private final IClassParser parser;
    private final ITypeConverter typeConverter;
    private final QueryBuilder qb;
    private final ITrestleOntology ontology;
    private final TrestleExecutorService aggregationPool;

    @Inject
    public AggregationEngine(@ReasonerPrefix String reasonerPrefix,
                             ITrestleObjectReader objectReader,
                             QueryBuilder queryBuilder,
                             ITrestleOntology ontology,
                             TrestleParser trestleParser,
                             TrestleExecutorFactory factory) {
        this.reasonerPrefix = reasonerPrefix;
        this.reader = objectReader;
        this.qb = queryBuilder;
        this.ontology = ontology;
        this.parser = trestleParser.classParser;
        this.typeConverter = trestleParser.typeConverter;
        this.aggregationPool = factory.create("aggregation-pool");

    }

    public <T extends @NonNull Object> Optional<Geometry> aggregateDataset(Class<T> clazz, AggregationRestriction restriction, @Nullable AggregationOperation operation) {
        final OffsetDateTime atTemporal = OffsetDateTime.now();
        final OffsetDateTime dbTemporal = OffsetDateTime.now();


        final OWLClass objectClass = this.parser.getObjectClass(clazz);
        final Integer classProjection = this.parser.getClassProjection(clazz);

//        Special handling of WKT values
        final IRI factIRI;
        if (restriction.getFact().equals("asWKT")) {
            factIRI = IRI.create(StaticIRI.GEOSPARQLPREFIX, "asWKT");
        } else {
            factIRI = this.parser.getFactIRI(clazz, restriction.getFact())
                    .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, restriction.getFact(), objectClass)));
        }

        final Class<?> factDatatype = this.parser.getFactDatatype(clazz, factIRI.toString())
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, restriction.getFact(), objectClass)));


        final String intersectionQuery = buildAggregationQuery(clazz, objectClass, factIRI, factDatatype, restriction, operation, atTemporal, dbTemporal);

        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        try {
//            First, do the TS intersection, to figure out a list of individuals to aggregate with
            final CompletableFuture<Geometry> unionGeomFuture = CompletableFuture.supplyAsync(() -> {
                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                final Instant start = Instant.now();
                try {
                    logger.debug("Performing aggregation restriction query");
                    return this.ontology.executeSPARQLResults(intersectionQuery).toList().blockingGet();
                } finally {
                    logger.debug("Finished, took {} ms", Duration.between(start, Instant.now()).toMillis());
                    this.ontology.returnAndCommitTransaction(tt);
                }
            }, this.aggregationPool)
                    .thenApply(resultSet -> resultSet
                            .stream()
                            .map(result -> result.unwrapIndividual("m"))
                            .map(AsOWLNamedIndividual::asOWLNamedIndividual)
                            .collect(Collectors.toList()))
                    .thenApply(individuals -> individuals
                            .stream()
                            .map(HasIRI::getIRI)
                            .map(individual -> CompletableFuture.supplyAsync(() -> {
                                final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
                                try {
                                    return this.reader.readTrestleObject(clazz, individual, false, null, null, tt);
                                } catch (NoValidStateException e) {
                                    logger.warn("Cannot read {}", individual, e);
                                    return null;
                                } finally {
                                    this.ontology.returnAndCommitTransaction(tt);
                                }
                            }, this.aggregationPool))
                            .collect(Collectors.toList()))
                    .thenCompose(LambdaUtils::sequenceCompletableFutures)
                    .thenApply((objects) -> {
                        logger.debug("Intersection complete, performing union with {} objects", objects.size());
                        return objects
                                .stream()
                                .filter(Objects::nonNull)
                                .map((obj) -> SpatialEngineUtils.buildObjectGeometry(obj, classProjection))
                                .collect(Collectors.toList());
                    })
                    .thenApply((geoms) -> {
                        logger.debug("Performing actual spatial union.");
                        final List<Polygon> jtsExteriorRings = SpatialEngineUtils.getJTSExteriorRings(geoms, classProjection);
                        return new GeometryCollection(jtsExteriorRings.toArray(new Geometry[0]), new GeometryFactory(new PrecisionModel(), classProjection)).union();
                    });
//            Now we need to combine everything into a multi geometry


            final Geometry unionGeom = unionGeomFuture.get();
            this.ontology.returnAndCommitTransaction(trestleTransaction);
            return Optional.of(unionGeom);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            return Optional.empty();
        } catch (ExecutionException e) {
            this.ontology.returnAndAbortTransaction(trestleTransaction);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Build the spatial adjacency graph for a given class.
     *
     * @param <T>         - {@link T} type parameter for object class
     * @param <B>         - {@link B} type parameter for return type from {@link Computable} function
     * @param clazz       - Java {@link Class} of objects to retrieve
     * @param objectID    - {@link String} ID of object to begin graph computation with
     * @param edgeCompute - {@link Computable} function to use for computing edge weights
     * @param filter      - {@link Filterable} function to use for determining whether or not to compute the given node
     * @param validAt     - {@link Temporal} optional validAt restriction
     * @param dbAt        - {@link Temporal} optional dbAt restriction
     * @return - {@link Single} {@link AdjacencyGraph} for object
     */
    public <T extends @NonNull Object, B extends Number> Single<AdjacencyGraph<T, B>> buildSpatialGraph(Class<T> clazz, String objectID, Computable<T, T, B> edgeCompute, Filterable<T> filter, @Nullable Temporal validAt, @Nullable Temporal dbAt) {
        final AdjacencyGraph<T, B> adjacencyGraph = new AdjacencyGraph<>();
        final IRI startIRI = parseStringToIRI(this.reasonerPrefix, objectID);

        Set<String> visited = new HashSet<>();
        Queue<String> individualQueue = new ArrayDeque<>();
        individualQueue.add(startIRI.toString());

        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(false);


        return Flowable.fromIterable(individualQueue)
                .takeWhile(Objects::nonNull)
                .doOnEach(notification -> visited.add(notification.getValue()))
                .flatMapCompletable(fromID -> {
                    final Single<T> initialSingle = this.reader.readTrestleObject(clazz, fromID, validAt, dbAt);

                    return initialSingle
                            .flatMapCompletable(from -> {
                                final Flowable<Edge<T, B>> relatedObjectsFlow = this.reader.getRelatedObjects(clazz, fromID, ObjectRelation.SPATIAL_MEETS, validAt, dbAt)
                                        .filter(filter::filter)
//                        Remove self
                                        .filter(related -> !this.parser.getIndividual(related).toStringID().equals(startIRI.toString()))
                                        .map(related -> new Edge<>(from, related, edgeCompute.compute(from, related))).publish().autoConnect(2);

                                final Single<AdjacencyGraph<T, B>> adjacencyGraphSingle = relatedObjectsFlow.collectInto(adjacencyGraph, AdjacencyGraph::addEdge);

                                final Single<Queue<String>> individualCompletable = relatedObjectsFlow
                                        .map(edge -> this.parser.getIndividual(edge.to))
                                        .map(OWLIndividual::toStringID)
                                        .filter(individual -> !visited.contains(individual))
                                        .collectInto(individualQueue, Queue::add);

                                return Completable.fromSingle(adjacencyGraphSingle);
                            });
                })
                .andThen(Single.just(adjacencyGraph))
                .doOnSuccess(success -> this.ontology.returnAndCommitTransaction(tt))
                .doOnError(error -> this.ontology.returnAndAbortTransaction(tt));
//
//        while (!individualQueue.isEmpty()) {
//            final String fromID = individualQueue.poll();
//            if (fromID == null) {
//                continue;
//            }
//            visited.add(fromID);
//
//            try {
////            Get the initial individual
//
//
//                final T from = this.reader.readTrestleObject(clazz, fromID, validAt, dbAt).blockingGet();
//
//
////            Get everything it touches
//                final List<Edge<T, B>> relatedEdges = this.reader.getRelatedObjects(clazz, fromID, ObjectRelation.SPATIAL_MEETS, validAt, dbAt).toList().blockingGet()
//                        .stream()
//                        .filter(filter::filter)
////                        Remove self
//                        .filter(related -> !this.parser.getIndividual(related).toStringID().equals(startIRI.toString()))
//                        .map(related -> new Edge<>(from, related, edgeCompute.compute(from, related)))
//                        .collect(Collectors.toList());
//
////            Add everything to the graph
//                relatedEdges.forEach(adjacencyGraph::addEdge);
////                Only add things we haven't seen before
//                relatedEdges
//                        .stream()
//                        .map(edge -> this.parser.getIndividual(edge.to))
//                        .map(OWLIndividual::toStringID)
//                        .filter(individual -> !visited.contains(individual))
//                        .forEach(individualQueue::add);
//            } catch (TrestleClassException | MissingOntologyEntity | NoValidStateException e) {
//                logger.error("Can't read individual", e);
//            }
//        }
//
//        return adjacencyGraph;
    }

    private String buildAggregationQuery(Class<?> clazz, OWLClass datasetClass, IRI factIRI, Class<?> factDatatype, AggregationRestriction restriction, @Nullable AggregationOperation operation, OffsetDateTime atTemporal, OffsetDateTime dbTemporal) {

//        final String filterStatement;
//        if (operation != null) {
//            filterStatement = operation.buildFilterString();
//        } else {
//            filterStatement = "FILTER((?ef <= ?existsFrom^^xsd:dateTime) && " +
//                    "(!bound(?et) || (?et > ?existsTo^^xsd:dateTime)))";
//        }

        final OffsetDateTime existsFrom = LocalDate.of(1990, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        final OffsetDateTime existsTo = LocalDate.of(2015, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

        final String restrictionQuery;
//        Are we a spatial value? If so, parse it out
        if (restriction.fact.equals("asWKT")) {
            final String wkt = (String) this.typeConverter.reprojectSpatial(restriction.getValue(), 4326);
            restrictionQuery = this.qb.buildSpatialRestrictionFragment(datasetClass, wkt, atTemporal, dbTemporal);
        } else {
            final OWLDatatype owlDatatype = this.typeConverter.getDatatypeFromJavaClass(factDatatype);
            restrictionQuery = this.qb.buildPropertyRestrictionFragment(datasetClass,
                    df.getOWLDataProperty(factIRI),
                    df.getOWLLiteral(restriction.getValue().toString(), owlDatatype),
                    existsFrom, existsTo,
                    atTemporal, dbTemporal);
        }
//        Add the aggregation operations
        final String fullQueryString;
        if (operation == null) {
            fullQueryString = this.qb.prefixizeQuery(restrictionQuery);
        } else {
//            Special handling of existsFrom
            if (operation.getProperty().equals("trestle:existsFrom")) {
                final OffsetDateTime existsTemporal = TemporalParser.parseTemporalToOntologyDateTime(LocalDate.parse(operation.getValue().toString(), DateTimeFormatter.ISO_LOCAL_DATE), ZoneOffset.UTC);
                fullQueryString = qb.buildExistenceAggregationQuery(restrictionQuery, existsTemporal, existsTo, operation.getType().toString());
            } else {
                //            Get the aggregation fact and data type
                final IRI opFactIRI = this.parser.getFactIRI(clazz, operation.getProperty())
                        .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, operation.getProperty(), clazz)));
                final Class<?> opFactDataType = this.parser.getFactDatatype(clazz, opFactIRI.toString())
                        .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_FACT_ERROR, operation.getProperty(), clazz)));
                fullQueryString = this.qb.buildAggregationQuery(restrictionQuery,
                        df.getOWLDataProperty(opFactIRI),
                        df.getOWLLiteral(operation.getValue().toString(), this.typeConverter.getDatatypeFromJavaClass(opFactDataType)),
                        operation.getType().toString());
            }
        }
        return fullQueryString;
    }


    public static class AggregationRestriction {

        private String fact;
        private Object value;

        public AggregationRestriction() {
//            Not used
        }

        public String getFact() {
            return fact;
        }

        public void setFact(String fact) {
            this.fact = fact;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public static class AdjacencyGraph<A, B extends Number> {

        private Set<A> nodes;
        private Set<Edge<A, B>> edges;
        private Map<A, List<Edge<A, B>>> adj;

        public AdjacencyGraph() {
            this.nodes = new HashSet<>();
            this.edges = new HashSet<>();
            this.adj = new HashMap<>();
        }

        public void addEdge(Edge<A, B> edge) {
            this.nodes.add(edge.from);
            this.edges.add(edge);
            this.updateNodeEdge(edge.from, edge);
            this.updateNodeEdge(edge.to, edge);
        }

        public void removeEdge(Edge<A, B> edge) {
            this.edges.remove(edge);
            this.removeNodeEdge(edge.from, edge);
            this.removeNodeEdge(edge.to, edge);
        }

        public Optional<List<Edge<A, B>>> getNodeEdges(A node) {
            //noinspection Convert2MethodRef
            return getNodeEdges(node, (a, b) -> noopComparator(a, b));
        }

        public Optional<List<Edge<A, B>>> getNodeEdges(A node, Comparator<Edge<A, B>> comparator) {
            final List<Edge<A, B>> edges = this.adj.get(node);
            if (edges == null) {
                return Optional.empty();
            }
            return Optional.of(edges
                    .stream()
                    .sorted(comparator)
                    .collect(Collectors.toList()));
        }

        public List<Edge<A, B>> getEdges() {
            //noinspection Convert2MethodRef
            return getEdges((a, b) -> noopComparator(a, b));
        }

        public List<Edge<A, B>> getEdges(Comparator<Edge<A, B>> comparator) {
            final List<Edge<A, B>> collect = edges
                    .stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
//            Deduplicate it
            final List<Edge<A, B>> output = new ArrayList<>();
            Set<Integer> seen = new HashSet<>();
            for (Edge<A, B> edge : collect) {
                final int hash = edge.from.hashCode() ^ edge.to.hashCode();
                if (seen.contains(hash)) {
                    continue;
                }

                output.add(edge);
                seen.add(hash);
            }

            return output;
        }

        private void updateNodeEdge(A node, Edge<A, B> edge) {
            final List<Edge<A, B>> nodeEdges = this.adj.get(node);
            if (nodeEdges == null) {
                List<Edge<A, B>> edges = new ArrayList<>();
                edges.add(edge);
                this.adj.put(node, edges);
            } else {
                nodeEdges.add(edge);
                this.adj.put(node, nodeEdges);
            }
        }

        private void removeNodeEdge(A node, Edge<A, B> edge) {
            final List<Edge<A, B>> nodeEdges = this.adj.get(node);
            if (nodeEdges != null) {
                nodeEdges.remove(edge);
            }
        }

        private int noopComparator(Edge<A, B> a, Edge<A, B> b) {
            return 0;
        }
    }


    public static class Edge<A, B extends Number> {

        private final A from;
        private final A to;
        private final B weight;

        public Edge(A from, A to, B weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        public A getFrom() {
            return from;
        }

        public A getTo() {
            return to;
        }

        public B getWeight() {
            return weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge<?, ?> edge = (Edge<?, ?>) o;
            return Objects.equals(from, edge.from) &&
                    Objects.equals(to, edge.to);
        }

        @Override
        public int hashCode() {

            return from.hashCode() ^ to.hashCode();
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "from=" + from +
                    ", to=" + to +
                    ", weight=" + weight +
                    '}';
        }
    }
}

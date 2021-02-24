package com.nickrobison.trestle.regionalization;

import com.nickrobison.trestle.datasets.TigerCountyObject;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.AggregationEngine;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.Computable;
import com.nickrobison.trestle.reasoner.engines.spatial.aggregation.Filterable;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.relations.CollectionRelationType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.math3.util.FastMath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Created by nickrobison on 7/21/18.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Disabled
public class RegionalizationTests {

    public static final Supplier<RuntimeException> NOT_FOUND = () -> new RuntimeException("Not found");
    private static TrestleReasoner reasoner;
    //    Just the counties we care about
    private static Map<String, Integer> counties;
    //        Set to August 1st, 2013
    public static final OffsetDateTime VALID_AT = LocalDate.of(2015, 8, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

    @BeforeAll
    public static void setup() {
        final Config config = ConfigFactory.parseResources("tiger-loader.conf");
        final String connectStr = config.getString("trestle.graphdb.connection_string");
        final String username = config.getString("trestle.graphdb.username");
        final String password = config.getString("trestle.graphdb.password");
        final String reponame = config.getString("trestle.graphdb.repo_name");
        final String ontLocation = config.getString("trestle.ontology.location");
        final String ontPrefix = config.getString("trestle.ontology.prefix");

        reasoner = new TrestleBuilder()
                .withDBConnection(connectStr, username, password)
                .withName(reponame)
                .withOntology(IRI.create(ontLocation))
                .withPrefix(ontPrefix)
                .withInputClasses(TigerCountyObject.class)
                .withoutMetrics()
                .build();

//        Setup Map
        counties = new HashMap<>();
        counties.put("Douglas County", 53017);
        counties.put("Chelan County", 53007);
        counties.put("Okanogan County", 53047);
        counties.put("Kittitas County", 53037);
        counties.put("Grant County", 53025);
        counties.put("Whatcom County", 53073);
        counties.put("Skagit County", 53057);
        counties.put("Snohomish County", 53061);
        counties.put("King County", 53033);
        counties.put("Pierce County", 53053);
        counties.put("Yakima County", 53077);
        counties.put("Benton County", 53005);
        counties.put("Franklin County", 53021);
        counties.put("Adams County", 53001);
        counties.put("Lincoln County", 53043);
        counties.put("Ferry County", 53019);

    }

    @Test
    public void buildGraph() {

        final AggregationEngine.AdjacencyGraph<TigerCountyObject, Integer> county_graph =
                reasoner.buildSpatialGraph(
//                        1.
                        TigerCountyObject.class,
//                        2.
                        counties.get("Douglas County").toString(),
//                        3.
                        new CountyCompute(),
//                        4.
                        new CountyFilter(counties),
//                        5.
                        VALID_AT,
//                        6.
                        null).blockingGet();

//        for (Map.Entry<String, Integer> entry : counties.entrySet()) {
//            computeEdges(entry.getValue().toString(), computedEdges);
//        }
//
//        //        Put it all into a queue and sort by value
//        Queue<Edge> sortedEdges = new PriorityQueue<>(Comparator.comparing(edge -> edge.value));
//        List<Edge> clusterEdges = new ArrayList<>();
//        sortedEdges.addAll(computedEdges);
//
//        while (!sortedEdges.isEmpty()) {
//            final Edge shortest = sortedEdges.poll();
//            if (shortest == null) {
//                continue;
//            }
//
////            compareClusters(shortest)
//
////            Are the two objects in the same cluster?
//            final Map<String, List<String>> relatedCollections = reasoner.getRelatedCollections(shortest.Aid, null, 0.01)
//                    .orElseThrow(() -> new RuntimeException("Should have something"));
//
////            If the map is empty, then they're in two separate clusters.
//            if (separateClusters(relatedCollections, shortest.Aid, shortest.Bid)) {
//
//            }
//
////
////            reasoner.co
//        }

        assertFalse(county_graph.getEdges().isEmpty(), "Should have edges");


    }

    private boolean separateClusters(Map<String, List<String>> collections, String A, String B) {
        if (collections.isEmpty()) {
            return true;
        }

        return false;
    }

    public void computeEdges(String county, Set<Edge> computedEdges) throws TrestleClassException, MissingOntologyEntity {

        final TigerCountyObject self = reasoner.readTrestleObject(TigerCountyObject.class, county, VALID_AT, null).blockingGet();
        final List<TigerCountyObject> spatiallyAdjacentObjects = reasoner.getRelatedObjects(TigerCountyObject.class, county,
                ObjectRelation.SPATIAL_MEETS,
                VALID_AT, null)
                .toList()
                .blockingGet()
                .stream()
                .filter(c -> counties.containsKey(c.getCounty()))
                .collect(Collectors.toList());

        final int self_pop = self.getPop_estimate();

        for (final TigerCountyObject adjCounty : spatiallyAdjacentObjects) {
            final int length = FastMath.abs(self_pop - adjCounty.getPop_estimate());
            computedEdges.add(new Edge(self.getCounty(), self.getGeoid(), adjCounty.getCounty(), adjCounty.getGeoid(), length));

//            Add it as its own cluster.

            reasoner.addObjectToCollection(String.format("%s:collection", adjCounty.getCounty()),
                    adjCounty, CollectionRelationType.SEMANTIC, 1.0);
        }
    }

    public void compareClusters(Edge edge) {
//        1.
        String collectionA = getFirstCollection(reasoner.getRelatedCollections(edge.Aid, null, 0.1).blockingGet());
        String collectionB = getFirstCollection(reasoner.getRelatedCollections(edge.Bid, null, 0.1).blockingGet());

        if (!collectionA.equals(collectionB)) {
            if (reasoner.collectionsAreAdjacent(collectionA, collectionB, 0.1).blockingGet()) {
                if (edge.value >= getClusterAvgDistance(collectionA, collectionB)) {

                    addShortestEdgeToCluster(collectionA, collectionB);

//                    2.
                    final List<TigerCountyObject> collectionAObjects = getCollectionObjects(collectionA);
                    final List<TigerCountyObject> collectionBObjects = getCollectionObjects(collectionB);

//                    3.
                    final List<String> otherCollections = reasoner.getCollections()
                            .toList()
                            .blockingGet()
                            .stream()
                            .filter(collection -> !(collection.equals(collectionA) || collection.equals(collectionB)))
                            .collect(Collectors.toList());

                    for (String collection : otherCollections) {
                        final List<TigerCountyObject> collectionObjects = getCollectionObjects(collection);
                        final int avgDistance = (computeClusterAvgDistance(collectionObjects, collectionAObjects) * computeNumEdges(collectionObjects, collectionAObjects) +
                                computeClusterAvgDistance(collectionObjects, collectionBObjects) * computeNumEdges(collectionObjects, collectionBObjects)) /
                                (computeNumEdges(collectionObjects, collectionAObjects) + computeNumEdges(collectionObjects, collectionBObjects));
                        setClusterAvgDistance(collection, collectionA, avgDistance);

                        removeEdges(collection, collectionB);
                        removeEdges(collection, collectionA);
                        addEdge(collection, collectionA, avgDistance);
                    }

                    for (TigerCountyObject bCounty : collectionBObjects) {
//                        4.
                        reasoner.removeObjectFromCollection(collectionB, bCounty, true);
                        reasoner.addObjectToCollection(collectionA, bCounty, CollectionRelationType.SEMANTIC, 1.0);
                    }
                }
            }
        }
    }

    public String getFirstCollection(Map<String, List<String>> collections) {
        return collections
                .keySet()
                .stream()
                .findFirst()
                .orElseThrow(NOT_FOUND);
    }

    public List<TigerCountyObject> getCollectionObjects(String collection) {
        return reasoner.getCollectionMembers(TigerCountyObject.class, collection, 0.1, null, VALID_AT).toList().blockingGet();
    }

    public int getClusterAvgDistance(String A, String B) {
        return 0;
    }

    public void setClusterAvgDistance(String A, String B, int distance) {

    }

    public <T> int computeClusterAvgDistance(List<T> aObjects, List<T> bObjects) {
        return 0;
    }

    public <T> int computeNumEdges(List<T> aObjects, List<T> bObjects) {
        return 0;
    }

    public void removeEdges(String A, String B) {

    }

    public void addEdge(String A, String B, int value) {

    }

    public void addShortestEdgeToCluster(String A, String B) {

    }


    public static class Edge {

        private final String A;
        private final String Aid;
        private final String B;
        private final String Bid;

        private final int id;
        private final int value;

        public Edge(String A, String Aid, String B, String Bid, int value) {
            this.A = A;
            this.B = B;
            this.Aid = Aid;
            this.Bid = Bid;
            this.id = A.hashCode() ^ B.hashCode();
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public int getValue() {
            return value;
        }

        public boolean edgeMatches(String A, String B) {
            return this.id == (A.hashCode() ^ B.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return id == edge.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "A='" + A + '\'' +
                    ", B='" + B + '\'' +
                    ", value=" + value +
                    '}';
        }
    }

    public static class CountyCompute implements Computable<TigerCountyObject, TigerCountyObject, Integer> {

        CountyCompute() {
//            Not used
        }

        @Override
        public Integer compute(TigerCountyObject nodeA, TigerCountyObject nodeB) {
            return FastMath.abs(nodeA.getPop_estimate() - nodeB.getPop_estimate());
        }
    }

    public static class CountyFilter implements Filterable<TigerCountyObject> {

        private final Map<String, Integer> counties;

        CountyFilter(Map<String, Integer> counties) {
            this.counties = counties;
        }

        @Override
        public boolean filter(TigerCountyObject nodeA) {
            return this.counties.containsKey(nodeA.getCounty());
        }
    }
}

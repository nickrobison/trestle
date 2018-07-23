package com.nickrobison.trestle.regionalization;

import com.nickrobison.trestle.datasets.TigerCountyObject;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.math3.util.FastMath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Created by nickrobison on 7/21/18.
 */
public class RegionalizationTests {

    private static TrestleReasoner reasoner;
    //    Just the counties we care about
    private static Map<String, Integer> counties;
    //        Set to August 1st, 2013
    public static final OffsetDateTime VALID_AT = LocalDate.of(2013, 8, 1).atStartOfDay().atOffset(ZoneOffset.UTC);

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
                .withoutCaching()
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
    public void buildGraph() throws TrestleClassException, MissingOntologyEntity {

        Set<Edge> computedEdges = new HashSet<>();

        for (Map.Entry<String, Integer> entry : counties.entrySet()) {
            computeEdges(entry.getValue().toString(), computedEdges);
        }

        //        Put it all into a queue and sort by value
        Queue<Edge> sortedEdges = new PriorityQueue<>(Comparator.comparing(edge -> edge.value));
        List<Edge> clusterEdges = new ArrayList<>();
        sortedEdges.addAll(computedEdges);

        while (!sortedEdges.isEmpty()) {
            final Edge shortest = sortedEdges.poll();
            if (shortest == null) {
                continue;
            }

//            Are the two objects in the same cluster?
            final Map<String, List<String>> relatedCollections = reasoner.getRelatedCollections(shortest.Aid, null, 0.01)
                    .orElseThrow(() -> new RuntimeException("Should have something"));

//            If the map is empty, then they're in two separate clusters.
            if (separateClusters(relatedCollections, shortest.Aid, shortest.Bid)) {

            }

//
//            reasoner.co
        }


    }

    private boolean separateClusters(Map<String, List<String>> collections, String A, String B) {
        if (collections.isEmpty()) {
            return true;
        }

        return false;
    }

    public void computeEdges(String county, Set<Edge> computedEdges) throws TrestleClassException, MissingOntologyEntity {

        final TigerCountyObject self = reasoner.readTrestleObject(TigerCountyObject.class, county, VALID_AT, null);
        final List<TigerCountyObject> spatiallyAdjacentObjects = reasoner.getRelatedObjects(TigerCountyObject.class, county,
                ObjectRelation.SPATIAL_MEETS,
                VALID_AT, null);

        final int self_pop = self.getPop_estimate();

        for (final TigerCountyObject adjacentObject : spatiallyAdjacentObjects) {
            final int length = FastMath.abs(self_pop - adjacentObject.getPop_estimate());
            computedEdges.add(new Edge(self.getCounty(), self.getGeoid(), adjacentObject.getCounty(), adjacentObject.getGeoid(), length));
        }
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
}

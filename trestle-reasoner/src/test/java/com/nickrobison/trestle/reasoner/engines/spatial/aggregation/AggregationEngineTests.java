package com.nickrobison.trestle.reasoner.engines.spatial.aggregation;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.SharedTestUtils;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.List;

import static com.nickrobison.trestle.SharedTestUtils.readFromShapeFiles;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Created by nickrobison on 7/25/18.
 */
@Tag("integration")
@Disabled // Disabled until we can have more than one spatial test running at the same time.
public class AggregationEngineTests extends AbstractReasonerTest {

    @Test
    public void testAdjacencyGraph() throws IOException {
        //        Start with King County state plane
        SharedTestUtils.ITestClassConstructor<TestClasses.KCProjectionTestClass, SimpleFeature> kcConstructor = (feature -> new TestClasses.KCProjectionTestClass(
                Long.parseLong(feature.getAttribute("OBJECTID").toString()),
                feature.getAttribute("NAMELSAD10").toString(),
                (Geometry) feature.getDefaultGeometry()));

        final List<TestClasses.KCProjectionTestClass> kingCountyShapes = readFromShapeFiles("king_county/kc_tract_10.shp", kcConstructor);
        kingCountyShapes
                .parallelStream()
                .forEach(county -> {
                    try {
                        this.reasoner.writeTrestleObject(county);
                        this.reasoner.calculateSpatialAndTemporalRelationships(TestClasses.KCProjectionTestClass.class, Long.toString(county.getObjectid()), null);
                    } catch (TrestleClassException | MissingOntologyEntity e) {
                        e.printStackTrace();
                    }
                });
        final KCCalculator comp = new KCCalculator();

//        Compute the adjacency graph
        final AggregationEngine.AdjacencyGraph<TestClasses.KCProjectionTestClass, Double> graph = this.reasoner.getAggregationEngine().buildSpatialGraph(TestClasses.KCProjectionTestClass.class,
                Long.toString(kingCountyShapes.get(0).getObjectid()), comp,
                (a) -> true, null, null);

        assertFalse(graph.getEdges().isEmpty(), "Should have edges");
    }

    @Override
    protected String getTestName() {
        return "aggregation_tests";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.KCProjectionTestClass.class);
    }

    public static class KCCalculator implements Computable<TestClasses.KCProjectionTestClass, TestClasses.KCProjectionTestClass, Double> {

        public KCCalculator() {
//            Not used
        }

        @Override
        public Double compute(TestClasses.KCProjectionTestClass nodeA, TestClasses.KCProjectionTestClass nodeB) {
            return nodeA.getGeom().getArea() - nodeB.getGeom().getArea();
        }
    }
}

package com.nickrobison.trestle.reasoner.engines.spatial;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.SharedTestUtils;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.vividsolutions.jts.geom.Geometry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.nickrobison.trestle.SharedTestUtils.readFromShapeFiles;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nickrobison on 6/25/18.
 */
@Tag("integration")
@Disabled
public class SpatialProjectionTest extends AbstractReasonerTest {

    @Test
    public void testSpatialProjection() throws IOException {
//        Load both of the test datasets
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
                    } catch (TrestleClassException | MissingOntologyEntity e) {
                        e.printStackTrace();
                    }
                });

//        Now the US census data
        SharedTestUtils.ITestClassConstructor<TestClasses.CensusProjectionTestClass, SimpleFeature> censusConstrutor = (feature -> new TestClasses.CensusProjectionTestClass(
                Long.parseLong(feature.getAttribute("GEOID10").toString()),
                feature.getAttribute("NAMELSAD10").toString(),
                (Geometry) feature.getDefaultGeometry()));

        readFromShapeFiles("tiger_kc/tl_2010_53033_tract10.shp", censusConstrutor)
                .parallelStream()
                .forEach(census -> {
                    try {
                        this.reasoner.writeTrestleObject(census);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        this.reasoner.getUnderlyingOntology().runInference();

        //        Try to intersect with a WGS 84 point
        final String polygonWKT = "POLYGON((-122.374781 47.690612, -122.325515 47.690612, -122.325515 47.668884, -122.374781 47.668884, -122.374781 47.690612))";
        final List<TestClasses.KCProjectionTestClass> kcObjects = this.reasoner.spatialIntersect(TestClasses.KCProjectionTestClass.class
                , polygonWKT, 0).orElseThrow(() -> new IllegalStateException("Should have objects"));
        List<SharedTestUtils.ICensusTract> intersectedObjects = new ArrayList<>(kcObjects);
        assertEquals(14, intersectedObjects.size(), "Should have intersected with 2 objects");

//        Try to add the others
        intersectedObjects.addAll(this.reasoner.spatialIntersect(TestClasses.CensusProjectionTestClass.class, polygonWKT, 0).orElseThrow(() -> new IllegalStateException("Should have objects")));

        assertEquals(28, intersectedObjects.size(), "Should have intersected with objects from both datasets");

//        Check to ensure they're equal
        final Map<String, List<SharedTestUtils.ICensusTract>> grouped = intersectedObjects
                .stream()
                .collect(groupingBy(SharedTestUtils.ICensusTract::getName));

        grouped.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    final List<SharedTestUtils.ICensusTract> value = entry.getValue();
                    final SpatialComparisonReport spatialComparisonReport = this.reasoner.compareTrestleObjects(value.get(0), value.get(1), 0.9);
                    assertAll(() -> assertTrue(spatialComparisonReport.getEquality().isPresent(), "Should have equality"),
                            () -> assertTrue(spatialComparisonReport.getEquality().get() > 0.99, "Should be almost exactly equal"));
                });


//        The 2 objects should be equal
//        final SpatialComparisonReport spatialComparisonReport = this.reasoner.compareTrestleObjects(intersectedClasses.get().get(0),
//                intersectedClasses.get().get(1),
//                SpatialReference.create(4326),
//                .9);
//        assertAll(() -> assertTrue(spatialComparisonReport.getEquality().isPresent(), "Should have equality"),
//                () -> assertTrue(spatialComparisonReport.getEquality().get() > 0.99, "Should be almost exactly equal"));
    }


    @Override
    protected String getTestName() {
        return "spatial_projection";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.KCProjectionTestClass.class,
                TestClasses.CensusProjectionTestClass.class);
    }
}

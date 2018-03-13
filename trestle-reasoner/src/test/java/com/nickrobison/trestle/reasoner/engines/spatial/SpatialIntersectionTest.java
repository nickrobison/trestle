package com.nickrobison.trestle.reasoner.engines.spatial;

import com.esri.core.geometry.SpatialReference;
import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.nickrobison.trestle.SharedUtils.*;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class SpatialIntersectionTest extends AbstractReasonerTest {


    @Test
    @Disabled
    public void spatialIntersectionTest() throws IOException {
        final List<TestClasses.GAULTestClass> gaulObjects = readGAULObjects();
        gaulObjects.parallelStream().forEach(gaul -> {
            try {
                reasoner.writeTrestleObject(gaul);
            } catch (TrestleClassException e) {
                throw new RuntimeException(String.format("Problem writing object %s", gaul.adm0_name), e);
            } catch (MissingOntologyEntity missingOntologyEntity) {
                throw new RuntimeException(String.format("Missing individual %s", missingOntologyEntity.getIndividual()), missingOntologyEntity);
            }
        });

//        Find Maputo
        final TestClasses.GAULTestClass maputo = gaulObjects.stream()
                .filter(gaul -> gaul.adm0_code == 41374)
                .findAny().orElseThrow(RuntimeException::new);

//        Do a standard spatial intersection with it

        final Optional<List<TrestleIndividual>> trestleIndividuals = this.reasoner.getSpatialEngine().spatialIntersectIndividuals(TestClasses.GAULTestClass.class,
                maputo.wkt, 0.0, null, null);
        assertAll(() -> assertTrue(trestleIndividuals.isPresent(), "Should have individuals"),
                () -> assertEquals(10, trestleIndividuals.get().size(), "Should have all Distrito and Aeropuerto individuals"));

        //        Do a TS intersection with it

        final Optional<List<TrestleIndividual>> individuals2015 = this.reasoner.getSpatialEngine().spatialIntersectIndividuals(TestClasses.GAULTestClass.class,
                maputo.wkt, 0.0, LocalDate.of(2015, 01, 01), null);

        assertAll(() -> assertTrue(individuals2015.isPresent(), "Should have an optional"),
                () -> assertEquals(1, individuals2015.get().size(), "Should only have itself"));


//        Find Manhica 2
        final TestClasses.GAULTestClass manhica = gaulObjects
                .stream()
                .filter(gaul -> gaul.adm0_code == 41375)
                .findFirst().orElseThrow(RuntimeException::new);

//        Add a buffer and try a spatial intersection
        final Optional<List<TrestleIndividual>> manhicaSpatial = this.reasoner.getSpatialEngine().spatialIntersectIndividuals(TestClasses.GAULTestClass.class
                , manhica.wkt, 0.1, null, null);

        assertAll(() -> assertTrue(manhicaSpatial.isPresent(), "Should have optional"),
                () -> assertEquals(6, manhicaSpatial.get().size(), "Should have multiple objects"));


//        Now with the buffer, do a TS Intersection
        final Optional<List<TrestleIndividual>> manhica2015 = this.reasoner.getSpatialEngine().spatialIntersectIndividuals(TestClasses.GAULTestClass.class,
                manhica.wkt, 0.1, LocalDate.of(2015, 01, 01), null);
        assertAll(() -> assertTrue(manhica2015.isPresent(), "Should have optional"),
                () -> assertEquals(2, manhica2015.get().size(), "Should not have old Manhica"));

    }

    @Test
    public void testSpatialProjection() throws IOException {
//        Load both of the test datasets
//        Start with King County state plane
        final List<TestClasses.KCProjectionTestClass> kingCountyShapes = readKCProjectionClass("king_county/kc.shp", "OBJECTID");
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
        readCensusProjectionClass("tiger_kc/tiger_kc.shp", "GEOID10")
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
        List<TestClasses.ICensusTract> intersectedObjects = new ArrayList<>(kcObjects);
        assertEquals(14, intersectedObjects.size(), "Should have intersected with 2 objects");

//        Try to add the others
        intersectedObjects.addAll(this.reasoner.spatialIntersect(TestClasses.CensusProjectionTestClass.class, polygonWKT, 0).orElseThrow(() -> new IllegalStateException("Should have objects")));

        assertEquals(28, intersectedObjects.size(), "Should have intersected with objects from both datasets");

//        Check to ensure they're equal
        final Map<String, List<TestClasses.ICensusTract>> grouped = intersectedObjects
                .stream()
                .collect(groupingBy(TestClasses.ICensusTract::getName));

        grouped.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    final List<TestClasses.ICensusTract> value = entry.getValue();
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
        return "sptest1";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.GAULTestClass.class,
                TestClasses.KCProjectionTestClass.class,
                TestClasses.CensusProjectionTestClass.class);
    }
}

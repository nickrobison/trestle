package com.nickrobison.trestle.reasoner.engines.spatial;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.SharedUtils;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class SpatialIntersectionTest extends AbstractReasonerTest {


    @Test
//    @Disabled
    public void spatialIntersectionTest() throws IOException {
        final List<TestClasses.GAULTestClass> gaulObjects = SharedUtils.readGAULObjects();
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


    @Override
    protected String getTestName() {
        return "spatial_intersection_test";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.GAULTestClass.class);
    }
}

package com.nickrobison.trestle.reasoner.engines.spatial;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.nickrobison.trestle.SharedTestUtils.readGAULObjects;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class SpatialIntersectionTest extends AbstractReasonerTest {


    @Test
    public void spatialIntersectionTest() throws IOException {
        final List<TestClasses.GAULTestClass> gaulObjects = readGAULObjects();
        gaulObjects.parallelStream().forEach(gaul -> {
            try {
                reasoner.writeTrestleObject(gaul).blockingAwait();
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
        final List<TrestleIndividual> trestleIndividuals = this.reasoner.spatialIntersectIndividuals(TestClasses.GAULTestClass.class,
                maputo.wkt, 0.0, null, null).toList().blockingGet();
        assertEquals(10, trestleIndividuals.size(), "Should have all Distrito and Aeropuerto individuals");
//
//        //        Do a TS intersection with it
//
//        final List<TrestleIndividual> individuals2015 = this.reasoner.spatialIntersectIndividuals(TestClasses.GAULTestClass.class,
//                maputo.wkt, 0.0, LocalDate.of(2015, 1, 1), null).toList().blockingGet();
//        assertEquals(1, individuals2015.size(), "Should only have itself");
//
//
////        Find Manhica 2
//        final TestClasses.GAULTestClass manhica = gaulObjects
//                .stream()
//                .filter(gaul -> gaul.adm0_code == 41375)
//                .findFirst().orElseThrow(RuntimeException::new);
//
////        Add a buffer and try a spatial intersection
//        final List<TrestleIndividual> manhicaSpatial = this.reasoner.spatialIntersectIndividuals(TestClasses.GAULTestClass.class
//                , manhica.wkt, 0.1, null, null).toList().blockingGet();
//        assertEquals(6, manhicaSpatial.size(), "Should have multiple objects");
//
//
////        Now with the buffer, do a TS Intersection
//        final List<TrestleIndividual> manhica2015 = this.reasoner.spatialIntersectIndividuals(TestClasses.GAULTestClass.class,
//                manhica.wkt, 0.1, LocalDate.of(2015, 1, 1), null).toList().blockingGet();
//        assertEquals(2, manhica2015.size(), "Should not have old Manhica");
    }


    @Override
    protected String getTestName() {
        return "spatial_intersection";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.GAULTestClass.class,
                TestClasses.KCProjectionTestClass.class,
                TestClasses.CensusProjectionTestClass.class);
    }
}

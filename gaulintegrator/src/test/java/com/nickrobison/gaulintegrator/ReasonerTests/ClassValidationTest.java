package com.nickrobison.gaulintegrator.ReasonerTests;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.nickrobison.gaulintegrator.GAULObject;
import com.nickrobison.gaulintegrator.common.ObjectID;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by nrobison on 8/2/16.
 */
public class ClassValidationTest {

    static TrestleReasoner reasoner;
    static ObjectID testID;
    static GAULObject testObject;

    @BeforeAll
    public static void setup() {
        reasoner = new TrestleReasoner.TrestleBuilder()
                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
//                .withDBConnection("jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial", "spatialUser", "spatial1")
                .withName("gaul_class_test")
                .initialize()
                .build();

//        Polygon testPolygon = new Polygon();
        Polygon testPolygon = (Polygon) GeometryEngine.geometryFromWkt("POLYGON((39.322559357 -13.2994823459999,39.322559357 -12.5851001739999,40.2268218990001 -12.5851001739999,40.2268218990001 -13.2994823459999,39.322559357 -13.2994823459999))", 0, Geometry.Type.Polygon);
//        Envelope env = new Envelope(1000, 2000, 1010, 2010);
//        testPolygon.addEnvelope(env, false);
        testID = new ObjectID();
        testObject = new GAULObject(testID,
                4321,
                "Test Object",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2000, 1, 1),
                testPolygon);
    }

    @Test
    public void testRegistration() {
        try {
            reasoner.registerClass(GAULObject.class);
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("Should not throw exception");
        }
    }

    @Test
    public void testObjectLoading() {
        try {
            reasoner.writeObjectAsFact(testObject);
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("Should not throw exception");
        }

//        Write out the ontology
//        reasoner.writeOntology(new File("/Users/nrobison/Desktop/gaul.owl").toURI(), false);

//        Try to read it back out
        GAULObject gaulObject = null;
        try {
            gaulObject = reasoner.readAsObject(GAULObject.class, testObject.getObjectName());
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("should not throw");
        } catch (MissingOntologyEntity missingOntologyEntity) {
            missingOntologyEntity.printStackTrace();
            fail("Should not throw");
        }

        assertEquals(testObject, gaulObject, "Both objects should be equal");
    }

    @AfterAll
    public static void shutdown() {
        reasoner.shutdown(true);
    }
}

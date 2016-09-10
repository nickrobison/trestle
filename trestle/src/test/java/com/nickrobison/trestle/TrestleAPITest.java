package com.nickrobison.trestle;

import afu.edu.emory.mathcs.backport.java.util.Arrays;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.parser.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 7/27/16.
 */
@SuppressWarnings({"Duplicates", "initialization"})
public class TrestleAPITest {

    private TrestleReasoner reasoner;
    private OWLDataFactory df;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");

    @BeforeEach
    public void setup() {
        reasoner = new TrestleBuilder()
//                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
//                .withDBConnection(
//                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
//                        "spatialUser",
//                        "spatial1")
                .withName("api_test")
                .withInputClasses(TestClasses.GAULTestClass.class,
                        TestClasses.GAULComplexClassTest.class,
                        TestClasses.JTSGeometryTest.class,
                        TestClasses.ESRIPolygonTest.class,
                        TestClasses.GeotoolsPolygonTest.class)
                .withoutCaching()
                .initialize()
                .build();

        df = OWLManager.getOWLDataFactory();
    }

    @Test
    public void gaulLoader() throws IOException, TrestleClassException, MissingOntologyEntity, OWLOntologyStorageException {
//        Parse the CSV
        List<TestClasses.GAULTestClass> gaulObjects = new ArrayList<>();

        final InputStream is = OracleOntologyGAULoader.class.getClassLoader().getResourceAsStream("objects.csv");

        final BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;

        while ((line = br.readLine()) != null) {


            final String[] splitLine = line.split(";");
            final int code;
            try {
                code = Integer.parseInt(splitLine[0]);
            } catch (NumberFormatException e) {
                continue;
            }


            LocalDate date = LocalDate.parse(splitLine[2].replace("\"", ""), formatter);
//            final Instant instant = Instant.from(date);
//            final LocalDateTime startTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

//            Need to add a second to get it to format correctly.
            gaulObjects.add(new TestClasses.GAULTestClass(code, splitLine[1].replace("\"", ""), date.atStartOfDay().plusSeconds(1), splitLine[4].replace("\"", "")));
        }

//        Write the objects
        gaulObjects.parallelStream().forEach(gaul -> {
            try {
                reasoner.writeObjectAsConcept(gaul);
            } catch (TrestleClassException e) {
                throw new RuntimeException(String.format("Problem storing object %s", gaul.adm0_name), e);
            }
        });

//        Validate Results
        final Set<OWLNamedIndividual> gaulInstances = reasoner.getInstances(TestClasses.GAULTestClass.class);
        assertEquals(191, gaulInstances.size(), "Wrong number of GAUL records from instances method");

//        reasoner.getUnderlyingOntology().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/gaul.owl")), false);

//        Try to read one out.
//        final GAULTestClass ancuabe = reasoner.readAsObject(GAULTestClass.class, IRI.create("trestle:", "Ancuabe"));
        final TestClasses.GAULTestClass ancuabe = reasoner.readAsObject(TestClasses.GAULTestClass.class, "Ancuabe");
        assertEquals(ancuabe.adm0_name, "Ancuabe", "Wrong name");
    }

    @Test
    public void testClasses() throws TrestleClassException, MissingOntologyEntity, ParseException, TransformException {

//        Complex objects
        final TestClasses.GAULComplexClassTest gaulComplexClassTest = new TestClasses.GAULComplexClassTest();
        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
        final TestClasses.JTSGeometryTest jtsGeometryTest = new TestClasses.JTSGeometryTest(4326, jtsGeom, LocalDate.now());
        final Polygon geometry = (Polygon) GeometryEngine.geometryFromWkt("POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))", 0, com.esri.core.geometry.Geometry.Type.Polygon);
        final TestClasses.ESRIPolygonTest esriPolygonTest = new TestClasses.ESRIPolygonTest(4792, geometry, LocalDate.now());

        List<Object> classObjects = new ArrayList<>();
        classObjects.add(gaulComplexClassTest);
        classObjects.add(jtsGeometryTest);
        classObjects.add(esriPolygonTest);

        classObjects.parallelStream().forEach(object -> {
            final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(object);
            try {
                reasoner.writeObjectAsFact(object);
            } catch (TrestleClassException e) {
                e.printStackTrace();
            }
            final Object returnedObject = reasoner.readAsObject(object.getClass(), owlNamedIndividual.getIRI(), false);
            if (returnedObject instanceof TestClasses.GAULComplexClassTest) {
                assertEquals(gaulComplexClassTest, returnedObject, "Should have the same object");
            } else if (returnedObject instanceof TestClasses.JTSGeometryTest) {
                assertEquals(jtsGeometryTest, returnedObject, "Should have the same object");
            } else {
                assertEquals(esriPolygonTest, returnedObject, "Should be equal");
            }
        });

//        Geotools
//
//        JTS.toGeometry()
//        final Geometry geotoolsGeom = JTS.toGeographic(new WKTReader().read("POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))"), DefaultGeographicCRS.WGS84);
//        JTS.toGeometry()
//        final TestClasses.GeotoolsPolygonTest geotoolsPolygonTest = new TestClasses.GeotoolsPolygonTest(UUID.randomUUID(), (org.opengis.geometry.coordinate.Polygon) geotoolsGeom, LocalDate.now());
//        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(geotoolsPolygonTest);
//        reasoner.writeObjectAsFact(geotoolsPolygonTest);
//        final TestClasses.GeotoolsPolygonTest geotoolsPolygonTest1 = reasoner.readAsObject(geotoolsPolygonTest.getClass(), owlNamedIndividual.getIRI(), false);
//        assertEquals(geotoolsPolygonTest, geotoolsPolygonTest1, "Should be equal");


    }


    @AfterEach
    public void close() throws OWLOntologyStorageException {
//        reasoner.getUnderlyingOntology().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/gaul.owl")), true);
        reasoner.shutdown(true);
    }
}

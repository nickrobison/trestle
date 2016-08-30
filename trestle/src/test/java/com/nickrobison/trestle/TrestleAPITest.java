package com.nickrobison.trestle;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.parser.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
//                .withDBConnection(
//                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
//                        "spatialUser",
//                        "spatial1")
                .withName("api-test")
                .withInputClasses(TestClasses.GAULTestClass.class, GAULComplexClassTest.class, TestClasses.JTSGeometryTest.class)
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
        gaulObjects.forEach(gaul -> {
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
    public void testClasses() throws TrestleClassException, MissingOntologyEntity, ParseException {

//        Complex objects
//        final GAULComplexClassTest gaulComplexClassTest = new GAULComplexClassTest();
//        OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(gaulComplexClassTest);
//        reasoner.writeObjectAsFact(gaulComplexClassTest);
//        final GAULComplexClassTest ReturnedGaulComplexClassTest = reasoner.readAsObject(gaulComplexClassTest.getClass(), owlNamedIndividual.getIRI(), false);
//        assertEquals(gaulComplexClassTest, ReturnedGaulComplexClassTest, "Should have the same object");

//        Spatial
//        JTS
        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
        final TestClasses.JTSGeometryTest jtsGeometryTest = new TestClasses.JTSGeometryTest(4326, jtsGeom, LocalDate.now());
        OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(jtsGeometryTest);
        reasoner.writeObjectAsFact(jtsGeometryTest);
        final TestClasses.JTSGeometryTest returnedJTS = reasoner.readAsObject(jtsGeometryTest.getClass(), owlNamedIndividual.getIRI(), false);
        assertEquals(jtsGeometryTest, returnedJTS, "Should have the same object");

//        ESRI

    }


    @AfterEach
    public void close() throws OWLOntologyStorageException {
//        reasoner.getUnderlyingOntology().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/gaul.owl")), true);
        reasoner.shutdown(true);
    }
}

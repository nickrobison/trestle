package com.nickrobison.trestle;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.parser.ClassParser;
import com.nickrobison.trestle.parser.OracleOntologyGAULoader;
import com.nickrobison.trestle.parser.TrestleParser;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 7/27/16.
 */
@SuppressWarnings({"Duplicates", "initialization"})
public class TrestleAPITest {

    public static final String OVERRIDE_PREFIX = "http://nickrobison.com/test-owl#";
    private TrestleReasoner reasoner;
    private OWLDataFactory df;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private String datasetClassID;
    private TrestleParser tp;

    @BeforeEach
    public void setup() {
        final Config config = ConfigFactory.parseResources("test.configuration.conf");
        reasoner = new TrestleBuilder()
                .withDBConnection(config.getString("trestle.ontology.connectionString"),
                        config.getString("trestle.ontology.username"),
                        config.getString("trestle.ontology.password"))
                .withName("api_test")
                .withOntology(IRI.create(config.getString("trestle.ontology.location")))
                .withPrefix(OVERRIDE_PREFIX)
                .withInputClasses(TestClasses.GAULTestClass.class,
                        TestClasses.GAULComplexClassTest.class,
                        TestClasses.JTSGeometryTest.class,
                        TestClasses.ESRIPolygonTest.class,
                        TestClasses.GeotoolsPolygonTest.class,
                        TestClasses.OffsetDateTimeTest.class)
                .withoutCaching()
                .initialize()
                .build();

        df = OWLManager.getOWLDataFactory();
        tp = new TrestleParser(df, OVERRIDE_PREFIX);
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
            gaulObjects.add(new TestClasses.GAULTestClass(code, splitLine[1].replace("\"", ""), date.atStartOfDay(), splitLine[4].replace("\"", "")));
        }

//        Write the objects
        gaulObjects.parallelStream().forEach(gaul -> {
            try {
                reasoner.WriteAsTSObject(gaul);
            } catch (TrestleClassException e) {
                throw new RuntimeException(String.format("Problem storing object %s", gaul.adm0_name), e);
            } catch (MissingOntologyEntity missingOntologyEntity) {
                throw new RuntimeException(String.format("Missing individual %s", missingOntologyEntity.getIndividual()), missingOntologyEntity);
            }
        });

        reasoner.getUnderlyingOntology().runInference();

//        Validate Results
        final Set<OWLNamedIndividual> gaulInstances = reasoner.getInstances(TestClasses.GAULTestClass.class);
        assertEquals(191, gaulInstances.size(), "Wrong number of GAUL records from instances method");

//        Try to read one out.
//        final GAULTestClass ancuabe = reasoner.readAsObject(GAULTestClass.class, IRI.create("trestle:", "Ancuabe"));
        final TestClasses.GAULTestClass ancuabe = reasoner.readAsObject(TestClasses.GAULTestClass.class, "Ancuabe");
        assertEquals(ancuabe.adm0_name, "Ancuabe", "Wrong name");
//        Check the temporal to make sure they got parsed correctly
        assertEquals(LocalDate.of(1990, 1, 1).atStartOfDay(), ancuabe.time, "Times should match");

//        Try to read out the datasets
        final Set<String> availableDatasets = reasoner.getAvailableDatasets();
        assertTrue(availableDatasets.size() > 0, "Should have dataset");

        datasetClassID = availableDatasets.stream()
                .filter(ds -> ds.equals("GAUL_Test"))
                .findAny()
                .get();
        @NonNull final Object ancuabe1 = reasoner.readAsObject(datasetClassID, "Ancuabe");
        assertEquals(ancuabe, ancuabe1, "Objects should be equal");
        final Object ancuabe2 = reasoner.readAsObject(reasoner.getDatasetClass(datasetClassID), "Ancuabe");
        assertEquals(ancuabe, ancuabe2, "Should be equal");

//        Check the spatial intersection
        Optional<List<@NonNull Object>> intersectedObjects = reasoner.spatialIntersectObject(ancuabe1, 100.0);
        assertTrue(intersectedObjects.isPresent(), "Should have objects");
        assertTrue(intersectedObjects.get().size() > 0, "Should have more than 1 object");
//
//        final Class<?> datasetClass = reasoner.getDatasetClass(datasetClassID);
//        intersectedObjects = reasoner.spatialIntersect(datasetClass, ((TestClasses.GAULTestClass) ancuabe1).wkt, 100.0);
//        assertTrue(intersectedObjects.isPresent());
//        assertTrue(intersectedObjects.get().size() > 0, "Should have more than 0 objects");
    }

    @Test
    public void testClasses() throws TrestleClassException, MissingOntologyEntity, ParseException, TransformException {

//        Spatial/Complex objects
        final TestClasses.GAULComplexClassTest gaulComplexClassTest = new TestClasses.GAULComplexClassTest();
        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
        final TestClasses.JTSGeometryTest jtsGeometryTest = new TestClasses.JTSGeometryTest(4326, jtsGeom, LocalDate.now());
        final Polygon geometry = (Polygon) GeometryEngine.geometryFromWkt("POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))", 0, com.esri.core.geometry.Geometry.Type.Polygon);
        final TestClasses.ESRIPolygonTest esriPolygonTest = new TestClasses.ESRIPolygonTest(4792, geometry, LocalDate.now());
        final TestClasses.OffsetDateTimeTest offsetDateTimeTest = new TestClasses.OffsetDateTimeTest(5515, OffsetDateTime.now(), OffsetDateTime.now().plusYears(5));

        List<Object> classObjects = new ArrayList<>();
        classObjects.add(gaulComplexClassTest);
        classObjects.add(jtsGeometryTest);
        classObjects.add(esriPolygonTest);
        classObjects.add(offsetDateTimeTest);

        classObjects.parallelStream().forEach(object -> {
            try {
                reasoner.WriteAsTSObject(object);
            } catch (TrestleClassException e) {
                e.printStackTrace();
            } catch (MissingOntologyEntity missingOntologyEntity) {
                missingOntologyEntity.printStackTrace();
            }
        });

        reasoner.getUnderlyingOntology().runInference();
        classObjects.stream().forEach(object -> {
            final OWLNamedIndividual owlNamedIndividual = tp.classParser.GetIndividual(object);
            final Object returnedObject = reasoner.readAsObject(object.getClass(), owlNamedIndividual.getIRI(), false);
            if (returnedObject instanceof TestClasses.GAULComplexClassTest) {
                assertEquals(gaulComplexClassTest, returnedObject, "Should have the same object");
            } else if (returnedObject instanceof TestClasses.JTSGeometryTest) {
                assertEquals(jtsGeometryTest, returnedObject, "Should have the same object");
            } else if (returnedObject instanceof TestClasses.OffsetDateTimeTest) {
                assertEquals(offsetDateTimeTest, returnedObject, "Should have the same object");
            } else {
                assertEquals(esriPolygonTest, returnedObject, "Should be equal");
            }
        });

//        Search for some matching individuals
        final IRI gaul_jts_test = IRI.create(OVERRIDE_PREFIX, "GAUL_JTS_Test");
        List<String> individuals = reasoner.searchForIndividual("43", gaul_jts_test.toString(), null);
        assertEquals(1, individuals.size(), "Should only have 1 individual in the JTS class");
//        FIXME(nrobison): For some reason, the inferencer isn't updating correctly. So the query works, but it's not grabbing the correct values
//        individuals = reasoner.searchForIndividuals("2");
//        assertEquals(4, individuals.size(), "Should have 4 individuals, overall");

//        Test attribute generation
        final TrestleIndividual trestleIndividual = reasoner.GetIndividualFacts(individuals.get(0));
        assertEquals(2, trestleIndividual.getFacts().size(), "Wrong number of attributes");

//        Now try to remove it
        reasoner.removeIndividual(classObjects.toArray(new Object[classObjects.size()]));

//        reasoner.writeOntology(new File("/Users/nrobison/Desktop/trestle_test.owl").toURI(), false);

//        Geotools
//
//        JTS.toGeometry()
//        final Geometry geotoolsGeom = JTS.toGeographic(new WKTReader().read("POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))"), DefaultGeographicCRS.WGS84);
//        JTS.toGeometry()
//        final TestClasses.GeotoolsPolygonTest geotoolsPolygonTest = new TestClasses.GeotoolsPolygonTest(UUID.randomUUID(), (org.opengis.geometry.coordinate.Polygon) geotoolsGeom, LocalDate.now());
//        final OWLNamedIndividual owlNamedIndividual = classParser.GetIndividual(geotoolsPolygonTest);
//        reasoner.WriteAsTSObject(geotoolsPolygonTest);
//        final TestClasses.GeotoolsPolygonTest geotoolsPolygonTest1 = reasoner.readAsObject(geotoolsPolygonTest.getClass(), owlNamedIndividual.getIRI(), false);
//        assertEquals(geotoolsPolygonTest, geotoolsPolygonTest1, "Should be equal");


    }


    @AfterEach
    public void close() throws OWLOntologyStorageException {
        reasoner.shutdown(true);
    }
}

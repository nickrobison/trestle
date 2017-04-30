package com.nickrobison.trestle;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.NoValidStateException;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.parser.TrestleParser;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 7/27/16.
 */
@SuppressWarnings({"Duplicates", "initialization"})
@Tag("integration")
public class TrestleAPITest {

    private static final Logger logger = LoggerFactory.getLogger(TrestleAPITest.class);
    public static final String OVERRIDE_PREFIX = "http://nickrobison.com/test-owl#";
    private TrestleReasonerImpl reasoner;
    private OWLDataFactory df;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private String datasetClassID;
    private TrestleParser tp;

    @BeforeEach
    public void setup() {
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf"));
        reasoner = (TrestleReasonerImpl) new TrestleBuilder()
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
                        TestClasses.OffsetDateTimeTest.class,
                        TestClasses.MultiLangTest.class,
                        TestClasses.FactVersionTest.class)
                .withoutCaching()
//                .withoutMetrics()
                .initialize()
                .build();

        df = OWLManager.getOWLDataFactory();
        tp = new TrestleParser(df, OVERRIDE_PREFIX, false, null);
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
        final TestClasses.MultiLangTest multiLangTest = new TestClasses.MultiLangTest();

        List<Object> classObjects = new ArrayList<>();
        classObjects.add(gaulComplexClassTest);
        classObjects.add(jtsGeometryTest);
        classObjects.add(esriPolygonTest);
        classObjects.add(offsetDateTimeTest);
        classObjects.add(multiLangTest);

        classObjects.parallelStream().forEach(object -> {
            try {
                reasoner.writeTrestleObject(object);
            } catch (TrestleClassException | MissingOntologyEntity e) {
                e.printStackTrace();
            }
        });
//        Try to write some relations between two objects
        reasoner.writeObjectRelationship(classObjects.get(1), classObjects.get(0), ObjectRelation.MEETS);
        reasoner.writeObjectRelationship(classObjects.get(1), classObjects.get(3), ObjectRelation.DURING);

        reasoner.getUnderlyingOntology().runInference();
        classObjects.parallelStream().forEach(object -> {
            final OWLNamedIndividual owlNamedIndividual = tp.classParser.getIndividual(object);
            final Object returnedObject = reasoner.readTrestleObject(object.getClass(), owlNamedIndividual.getIRI(), false);
            if (returnedObject instanceof TestClasses.GAULComplexClassTest) {
                assertEquals(gaulComplexClassTest, returnedObject, "Should have the same object");
            } else if (returnedObject instanceof TestClasses.JTSGeometryTest) {
                assertEquals(jtsGeometryTest, returnedObject, "Should have the same object");
            } else if (returnedObject instanceof TestClasses.OffsetDateTimeTest) {
                assertEquals(offsetDateTimeTest, returnedObject, "Should have the same object");
            } else if (returnedObject instanceof TestClasses.MultiLangTest) {
                assertEquals(multiLangTest, returnedObject, "Should have the same object");
            } else {
                assertEquals(esriPolygonTest, returnedObject, "Should be equal");
            }
        });

//        Search for some matching individuals
        final IRI gaul_jts_test = IRI.create(OVERRIDE_PREFIX, "GAUL_JTS_Test");
        List<String> individuals = reasoner.searchForIndividual("43", gaul_jts_test.toString(), null);
        assertEquals(1, individuals.size(), "Should only have 1 individual in the JTS class");
//        individuals = reasoner.searchForIndividuals("2");
//        assertEquals(4, individuals.size(), "Should have 4 individuals, overall");

//        Test attribute generation
        final Instant iStart = Instant.now();
        final TrestleIndividual trestleIndividual = reasoner.getTrestleIndividual(individuals.get(0));
        final Instant iEnd = Instant.now();
        logger.info("Creating individual took {} ms", Duration.between(iStart, iEnd).toMillis());
        assertAll(() -> assertEquals(2, trestleIndividual.getFacts().size(), "Wrong number of attributes"),
                () -> assertEquals(2, trestleIndividual.getRelations().size(), "Wrong number of relations"));


//        Now try to remove it
//        reasoner.removeIndividual(classObjects.toArray(new Object[classObjects.size()]));

//        reasoner.writeOntology(new File("/Users/nrobison/Desktop/trestle_test.owl").toURI(), false);

//        Geotools
//
//        JTS.toGeometry()
//        final Geometry geotoolsGeom = JTS.toGeographic(new WKTReader().read("POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))"), DefaultGeographicCRS.WGS84);
//        JTS.toGeometry()
//        final TestClasses.GeotoolsPolygonTest geotoolsPolygonTest = new TestClasses.GeotoolsPolygonTest(UUID.randomUUID(), (org.opengis.geometry.coordinate.Polygon) geotoolsGeom, LocalDate.now());
//        final OWLNamedIndividual owlNamedIndividual = classParser.getIndividual(geotoolsPolygonTest);
//        reasoner.writeTrestleObject(geotoolsPolygonTest);
//        final TestClasses.GeotoolsPolygonTest geotoolsPolygonTest1 = reasoner.readTrestleObject(geotoolsPolygonTest.getClass(), owlNamedIndividual.getIRI(), false);
//        assertEquals(geotoolsPolygonTest, geotoolsPolygonTest1, "Should be equal");

        reasoner.getMetricsEngine().exportData(new File("./target/api-test-metrics.csv"));
    }

    @Test
    public void testCache() throws TrestleClassException, MissingOntologyEntity {
        final TestClasses.OffsetDateTimeTest offsetDateTimeTest = new TestClasses.OffsetDateTimeTest(5515, OffsetDateTime.now(), OffsetDateTime.now().plusYears(5));
        reasoner.writeTrestleObject(offsetDateTimeTest);
        reasoner.getUnderlyingOntology().runInference();
        final Instant firstStart = Instant.now();
        final TestClasses.OffsetDateTimeTest first = reasoner.readTrestleObject(TestClasses.OffsetDateTimeTest.class, offsetDateTimeTest.adm0_code.toString(), LocalDate.of(2017, 7, 1), null);
        final Instant firstEnd = Instant.now();
        final Instant secondStart = Instant.now();
        final TestClasses.OffsetDateTimeTest second = reasoner.readTrestleObject(TestClasses.OffsetDateTimeTest.class, offsetDateTimeTest.adm0_code.toString(), LocalDate.of(2018, 3, 1), null);
        final Instant secondEnd = Instant.now();
        assertEquals(first, second, "Objects should be equal");
        final Duration firstDuration = Duration.between(firstStart, firstEnd);
        final Duration secondDuration = Duration.between(secondStart, secondEnd);
        logger.info("Took {} ms to read first object, {} ms to read it again", firstDuration.toMillis(), secondDuration.toMillis());
        assertTrue(firstDuration.compareTo(secondDuration) > 0, "Cache should have lower latency");
    }

    @Test
    public void testFactValidity() throws TrestleClassException, MissingOntologyEntity {
        final TestClasses.FactVersionTest v1 = new TestClasses.FactVersionTest("test-object",
                LocalDate.of(1989, 3, 26),
                "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))",
                "test value one");
        final TestClasses.FactVersionTest v2 = new TestClasses.FactVersionTest("test-object",
                LocalDate.of(1990, 5, 14),
                "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))",
                "test value two");
        final TestClasses.FactVersionTest v3 = new TestClasses.FactVersionTest("test-object",
                LocalDate.of(2016, 3, 11),
                "POINT(0.71255092695307 -25.572028714467507)",
                "test value two");

//        Write each, then validate
        reasoner.writeTrestleObject(v1);
        reasoner.getUnderlyingOntology().runInference();
        final TestClasses.FactVersionTest v1Return = reasoner.readTrestleObject(v1.getClass(), tp.classParser.getIndividual(v1).getIRI(), false);
        assertEquals(v1, v1Return, "Should be equal to V1");
        reasoner.writeTrestleObject(v2);
        reasoner.getUnderlyingOntology().runInference();
        final TestClasses.FactVersionTest v2Return = reasoner.readTrestleObject(v2.getClass(), tp.classParser.getIndividual(v1).getIRI(), false);
        assertEquals(v2, v2Return, "Should be equal to V2");
        reasoner.writeTrestleObject(v3);
        reasoner.getUnderlyingOntology().runInference();
        final TestClasses.FactVersionTest v3Return = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false);
        assertEquals(v3, v3Return, "Should be equal to V3");
//        Try for specific points in time
        final TestClasses.FactVersionTest v1ReturnHistorical = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, LocalDate.of(1990, 3, 26), null);
        assertEquals(v1, v1ReturnHistorical, "Historical query should be equal to V1");
        final TestClasses.FactVersionTest v2ReturnHistorical = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, LocalDate.of(1999, 3, 26), null);
        assertEquals(v2, v2ReturnHistorical, "Historical query should be equal to V2");
        final TestClasses.FactVersionTest v3ReturnHistorical = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, LocalDate.of(2016, 3, 26), null);
        assertEquals(v3, v3ReturnHistorical, "Historical query should be equal to V3");
        assertThrows(NoValidStateException.class, () -> reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), true, LocalDate.of(1980, 3, 26), null));

//        Check to make sure we have all the facts
        final TrestleIndividual trestleIndividual = reasoner.getTrestleIndividual("test-object");
        assertEquals(5, trestleIndividual.getFacts().size(), "Should have 5 facts over the lifetime of the object");

//        Try to manually add a new value
        reasoner.addFactToTrestleObject(v3.getClass(), "test-object", "testValue", "test value three", LocalDate.of(2007, 3, 26), null, null);
        reasoner.addFactToTrestleObject(v3.getClass(), "test-object", "wkt", "POINT(1.71255092695307 -30.572028714467507)", LocalDate.of(2017, 1, 1), null);

        reasoner.getUnderlyingOntology().runInference();
//        Try to get some fact values
        final Optional<List<Object>> values = reasoner.getFactValues(v3.getClass(), "test-object", "testValue", null, null, null);
        assertAll(() -> assertTrue(values.isPresent(), "Should have fact values"),
                () -> assertEquals(3, values.get().size(), "Should have 3 fact values"));

        final Optional<List<Object>> wktValues = reasoner.getFactValues(v3.getClass(), "test-object", "wkt", LocalDate.of(1988, 3, 26), LocalDate.of(1995, 3, 26), null);
        assertAll(() -> assertTrue(wktValues.isPresent(), "Should have wkt values"),
                () -> assertEquals(1, wktValues.get().size(), "Should only have 1 wkt value"));

//        Test database temporals
        reasoner.getMetricsEngine().exportData(new File("./target/api-test-fact-validity-metrics.csv"));
    }

    @Test
    public void gaulLoader() throws IOException, TrestleClassException, MissingOntologyEntity, OWLOntologyStorageException {
//        Parse the CSV
        List<TestClasses.GAULTestClass> gaulObjects = new ArrayList<>();

        final InputStream is = TrestleAPITest.class.getClassLoader().getResourceAsStream("objects.csv");

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
                reasoner.writeTrestleObject(gaul);
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
//        final GAULTestClass ancuabe = reasoner.readTrestleObject(GAULTestClass.class, IRI.create("trestle:", "Ancuabe"));
        final TestClasses.GAULTestClass ancuabe = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, IRI.create(OVERRIDE_PREFIX, "Ancuabe"), true, OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
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
        @NonNull final Object ancuabe1 = reasoner.readTrestleObject(datasetClassID, "Ancuabe", OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
        assertEquals(ancuabe, ancuabe1, "Objects should be equal");
        final Object ancuabe2 = reasoner.readTrestleObject(reasoner.getDatasetClass(datasetClassID), "Ancuabe", OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
        assertEquals(ancuabe, ancuabe2, "Should be equal");

//        Check the spatial intersection
        Optional<List<@NonNull Object>> intersectedObjects = reasoner.spatialIntersectObject(ancuabe1, 100.0, OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC));
        assertTrue(intersectedObjects.isPresent(), "Should have objects");
        assertTrue(intersectedObjects.get().size() > 0, "Should have more than 1 object");
//
//        final Class<?> datasetClass = reasoner.getDatasetClass(datasetClassID);
//        intersectedObjects = reasoner.spatialIntersect(datasetClass, ((TestClasses.GAULTestClass) ancuabe1).wkt, 100.0);
//        assertTrue(intersectedObjects.isPresent());
//        assertTrue(intersectedObjects.get().size() > 0, "Should have more than 0 objects");
        reasoner.getMetricsEngine().exportData(new File("./target/api-test-gaul-loader-metrics.csv"));
    }

    @AfterEach
    public void close() throws OWLOntologyStorageException {
        reasoner.shutdown(true);
    }

}

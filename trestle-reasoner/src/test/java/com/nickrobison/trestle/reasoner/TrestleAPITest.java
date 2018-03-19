package com.nickrobison.trestle.reasoner;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.SharedTestUtils;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.TrestleRelation;
import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 7/27/16.
 */
@SuppressWarnings({"Duplicates", "initialization", "ConstantConditions"})
@Tag("integration")
public class TrestleAPITest extends AbstractReasonerTest {

    @Test
    public void testClasses() throws ParseException {

//        Spatial/Complex objects
        final TestClasses.GAULComplexClassTest gaulComplexClassTest = new TestClasses.GAULComplexClassTest();
        final Geometry jtsGeom = new WKTReader().read("POINT(4.0 6.0)");
        jtsGeom.setSRID(4269);
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
        reasoner.writeObjectRelationship(classObjects.get(1), classObjects.get(0), ObjectRelation.SPATIAL_MEETS);
        reasoner.writeObjectRelationship(classObjects.get(1), classObjects.get(3), ObjectRelation.DURING);

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
        final TrestleIndividual trestleIndividual = reasoner.getTrestleIndividual(individuals.get(0));
        assertAll(() -> assertEquals(2, trestleIndividual.getFacts().size(), "Wrong number of attributes"),
                () -> assertEquals(1, trestleIndividual.getRelations().size(), "Wrong number of relations"));


//        Now try to remove it
//        reasoner.removeIndividual(classObjects.toArray(new Object[classObjects.size()]));

//        reasoner.writeOntology(new File("/Users/nrobison/Desktop/trestle_test.owl").toURI(), false);

        reasoner.getMetricsEngine().exportData(new File("./target/api-test-metrics.csv"));
    }

    @Test
    public void testClassRegistration() throws TrestleClassException, MissingOntologyEntity {
        final TestClasses.GAULComplexClassTest gaulComplexClassTest = new TestClasses.GAULComplexClassTest();
//        De register the class
        this.reasoner.deregisterClass(TestClasses.GAULComplexClassTest.class);
//        Try to write the indvidual
        assertThrows(TrestleClassException.class, () -> this.reasoner.writeTrestleObject(gaulComplexClassTest));
//        Register the class again
        this.reasoner.registerClass(TestClasses.GAULComplexClassTest.class);
//        Try again
        this.reasoner.writeTrestleObject(gaulComplexClassTest);
    }

    @Test
    public void eventTest() throws TrestleClassException, MissingOntologyEntity {
//        Split event
//        Create test events
        final OffsetDateTime earlyStart = LocalDate.of(1990, 1, 1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        final OffsetDateTime middle = earlyStart.plusYears(5);
        final OffsetDateTime end = middle.plusYears(5);
        final TestClasses.OffsetDateTimeTest split_start = new TestClasses.OffsetDateTimeTest(100, earlyStart, middle);
        final TestClasses.OffsetDateTimeTest split1 = new TestClasses.OffsetDateTimeTest(101, middle, end);
        final TestClasses.OffsetDateTimeTest split2 = new TestClasses.OffsetDateTimeTest(102, middle, end);
        final TestClasses.OffsetDateTimeTest split3 = new TestClasses.OffsetDateTimeTest(103, middle, end);
        final TestClasses.OffsetDateTimeTest split4 = new TestClasses.OffsetDateTimeTest(104, middle, end);
        final TestClasses.OffsetDateTimeTest split5 = new TestClasses.OffsetDateTimeTest(105, middle, end);
        final ImmutableList<TestClasses.OffsetDateTimeTest> splitSet = ImmutableList.of(split1, split2, split3, split4, split5);
//        Try for invalid event types
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.CREATED, split_start, splitSet, 0.8)),
                () -> assertThrows(IllegalArgumentException.class, () -> this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.BECAME, split_start, splitSet, 0.8)),
                () -> assertThrows(IllegalArgumentException.class, () -> this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.DESTROYED, split_start, splitSet, 0.8)));
        this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.SPLIT, split_start, splitSet, 0.8);
//        Check that the subject has the split event
        final Optional<Set<TrestleEvent>> individualEvents = this.reasoner.getIndividualEvents(split_start.getClass(), split_start.adm0_code.toString());
        assertAll(() -> assertTrue(individualEvents.isPresent()),
                () -> assertEquals(3, individualEvents.get().size()),
                () -> assertEquals(middle, individualEvents.get().stream().filter(event -> event.getType() == TrestleEventType.SPLIT).findFirst().get().getAtTemporal(), "SPLIT event should equal end temporal"));

//        Merge event
        final TestClasses.OffsetDateTimeTest merge_subject = new TestClasses.OffsetDateTimeTest(200, middle, end);
        final TestClasses.OffsetDateTimeTest merge1 = new TestClasses.OffsetDateTimeTest(201, earlyStart, middle);
        final TestClasses.OffsetDateTimeTest merge2 = new TestClasses.OffsetDateTimeTest(202, earlyStart, middle);
        final TestClasses.OffsetDateTimeTest merge3 = new TestClasses.OffsetDateTimeTest(203, earlyStart, middle);
        final TestClasses.OffsetDateTimeTest merge4 = new TestClasses.OffsetDateTimeTest(204, earlyStart, middle);
        final TestClasses.OffsetDateTimeTest merge5 = new TestClasses.OffsetDateTimeTest(205, earlyStart, middle);
        final ImmutableList<TestClasses.OffsetDateTimeTest> mergeSet = ImmutableList.of(merge1, merge2, merge3, merge4, merge5);
        this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.MERGED, merge_subject, mergeSet, 0.8);
        final Optional<Set<TrestleEvent>> mergeEvents = this.reasoner.getIndividualEvents(merge_subject.getClass(), merge_subject.adm0_code.toString());
        assertAll(() -> assertTrue(mergeEvents.isPresent()),
                () -> assertEquals(3, individualEvents.get().size()),
                () -> assertEquals(merge_subject.startTemporal, mergeEvents.get().stream().filter(event -> event.getType() == TrestleEventType.MERGED).findFirst().get().getAtTemporal(), "MERGED temporal should equal created date"));

//        Ensure that events are handled correctly (along with relations).
//        Split first
        final TrestleIndividual splitStartIndividual = this.reasoner.getTrestleIndividual(split_start.adm0_code.toString());
        assertAll(() -> assertEquals(3, splitStartIndividual.getEvents().size(), "Should have 3 events"),
                () -> assertEquals(5, splitStartIndividual.getRelations().stream().filter(relation -> relation.getType().equals("SPLIT_INTO")).collect(Collectors.toList()).size(), "Should have 5 split_into events"));
        final Optional<TrestleRelation> split_from = this.reasoner.getTrestleIndividual(split1.adm0_code.toString()).getRelations().stream().filter(relation -> relation.getType().equals("SPLIT_FROM")).findFirst();
        assertAll(() -> assertTrue(split_from.isPresent()),
                () -> assertEquals("http://nickrobison.com/test-owl/100", split_from.get().getObject(), "Should point to starting split"));

//        Now merged
        final TrestleIndividual mergeSubjectIndividual = this.reasoner.getTrestleIndividual(merge_subject.adm0_code.toString());
        assertAll(() -> assertEquals(3, mergeSubjectIndividual.getEvents().size(), "Should have 3 events"),
                () -> assertEquals(5, mergeSubjectIndividual.getRelations().stream().filter(relation -> relation.getType().equals("MERGED_FROM")).collect(Collectors.toList()).size(), "Should have 5 merged objects"));
        final Optional<TrestleRelation> merged_into = this.reasoner.getTrestleIndividual(merge5.adm0_code.toString()).getRelations().stream().filter(relation -> relation.getType().equals("MERGED_INTO")).findFirst();
        assertAll(() -> assertTrue(merged_into.isPresent()),
                () -> assertEquals("http://nickrobison.com/test-owl/200", merged_into.get().getObject(), "Should point to merged subject"));
    }

    @Test
    public void gaulLoader() throws IOException, TrestleClassException, MissingOntologyEntity {


//        Write the objects
        SharedTestUtils.readGAULObjects().parallelStream().forEach(gaul -> {
            try {
                reasoner.writeTrestleObject(gaul);
            } catch (TrestleClassException e) {
                throw new RuntimeException(String.format("Problem storing object %s", gaul.adm0_name), e);
            } catch (MissingOntologyEntity missingOntologyEntity) {
                throw new RuntimeException(String.format("Missing individual %s", missingOntologyEntity.getIndividual()), missingOntologyEntity);
            }
        });

//        Validate Results
        final Set<OWLNamedIndividual> gaulInstances = reasoner.getInstances(TestClasses.GAULTestClass.class);
        assertEquals(200, gaulInstances.size(), "Wrong number of GAUL records from instances method");

//        Try to read one out.
//        final GAULTestClass ancuabe = reasoner.readTrestleObject(GAULTestClass.class, IRI.create("trestle:", "Ancuabe"));
        final TestClasses.GAULTestClass ancuabe = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, IRI.create(OVERRIDE_PREFIX, "Ancuabe"), true, OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
        assertEquals(ancuabe.adm0_name, "Ancuabe", "Wrong name");
//        Check the temporal to make sure they got parsed correctly
        assertEquals(LocalDate.of(1990, 1, 1).atStartOfDay(), ancuabe.time, "Times should match");

//        Try to read out the datasets
        final Set<String> availableDatasets = reasoner.getAvailableDatasets();
        assertTrue(availableDatasets.size() > 0, "Should have dataset");

        String datasetClassID = availableDatasets.stream()
                .filter(ds -> ds.equals("GAUL_Test"))
                .findAny()
                .get();
        @NonNull final Object ancuabe1 = reasoner.readTrestleObject(datasetClassID, "Ancuabe", OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
        assertEquals(ancuabe, ancuabe1, "Objects should be equal");
        final Object ancuabe2 = reasoner.readTrestleObject(reasoner.getDatasetClass(datasetClassID), "Ancuabe", OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
        assertEquals(ancuabe, ancuabe2, "Should be equal");

//        Check the spatial intersection
        Optional<List<@NonNull Object>> intersectedObjects = reasoner.spatialIntersectObject(ancuabe1, 100.0, OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null);
        assertTrue(intersectedObjects.isPresent(), "Should have objects");
        assertTrue(intersectedObjects.get().size() > 0, "Should have more than 1 object");

//        Big intersection
        final String mozWKT = "POLYGON((30.21 -10.33, 41.05 -10.33, 41.05 -26.92, 30.21 -26.92, 30.21 -10.33))";
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> reasoner.spatialIntersect(TestClasses.GAULTestClass.class, mozWKT, 100.0, OffsetDateTime.of(LocalDate.of(1990, 3, 26).atStartOfDay(), ZoneOffset.UTC), null), "Should complete in less than 60 seconds");
//        final Optional<List<TestClasses.@NonNull GAULTestClass>> mozClasses = reasoner.spatialIntersect(TestClasses.GAULTestClass.class, mozWKT, 100.0);
//        assertAll(() -> assertTrue(intersectedObjects))
//
//        final Class<?> datasetClass = reasoner.getDatasetClass(datasetClassID);
//        intersectedObjects = reasoner.spatialIntersect(datasetClass, ((TestClasses.GAULTestClass) ancuabe1).wkt, 100.0);
//        assertTrue(intersectedObjects.isPresent());
//        assertTrue(intersectedObjects.get().size() > 0, "Should have more than 0 objects");
        reasoner.getMetricsEngine().exportData(new File("./target/api-test-gaul-loader-metrics.csv"));
    }

    @Override
    protected String getTestName() {
        return "api_test";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.GAULTestClass.class,
                TestClasses.GAULComplexClassTest.class,
                TestClasses.JTSGeometryTest.class,
                TestClasses.ESRIPolygonTest.class,
                TestClasses.GeotoolsPolygonTest.class,
                TestClasses.OffsetDateTimeTest.class,
                TestClasses.MultiLangTest.class,
                TestClasses.FactVersionTest.class);
    }
}

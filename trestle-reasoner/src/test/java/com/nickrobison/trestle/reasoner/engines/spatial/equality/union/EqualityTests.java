package com.nickrobison.trestle.reasoner.engines.spatial.equality.union;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Polygon;
import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.SharedTestUtils;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.EqualityEngine;
import com.nickrobison.trestle.types.events.TrestleEventType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"ConstantConditions", "initialization.fields.uninitialized"})
@Tag("integration")
public class EqualityTests extends AbstractReasonerTest {

    public static final int INPUT_SR = 4296;
    private EqualityTestClass obj1;
    private EqualityTestClass obj2;
    private EqualityTestClass obj3;
    private EqualityTestClass obj4;
    private EqualityTestClass obj5;
    private EqualityTestClass obj6;
    private EqualityTestClass obj7;
    private EqualityTestClass obj8;
    private EqualityTestClass obj9;
    private EqualityEngine unionWalker;
    private OWLNamedIndividual obj1Individual;
    private OWLNamedIndividual obj5Individual;
    private OWLNamedIndividual obj7Individual;
    private OWLNamedIndividual obj2Individual;
    private OWLNamedIndividual obj9Individual;
    private OWLNamedIndividual obj6Individual;
    private OWLNamedIndividual obj3Individual;
    private OWLNamedIndividual obj4Individual;
    private OWLNamedIndividual obj8Individual;

    @BeforeEach
    public void loadData() {
//        Load the data
        obj1 = new EqualityTestClass("obj1", LocalDate.of(2012, 1, 1), LocalDate.of(2013, 1, 1));
        obj2 = new EqualityTestClass("obj2", LocalDate.of(2013, 1, 1), LocalDate.of(2014, 1, 1));
        obj3 = new EqualityTestClass("obj3", LocalDate.of(2013, 1, 1), LocalDate.of(2017, 1, 1));
        obj4 = new EqualityTestClass("obj4", LocalDate.of(2013, 1, 1), LocalDate.of(2018, 1, 1));
        obj5 = new EqualityTestClass("obj5", LocalDate.of(2013, 1, 1), LocalDate.of(2016, 1, 1));
        obj6 = new EqualityTestClass("obj6", LocalDate.of(2013, 1, 1), LocalDate.of(2016, 1, 1));
        obj7 = new EqualityTestClass("obj7", LocalDate.of(2014, 1, 1), LocalDate.of(2018, 1, 1));
        obj8 = new EqualityTestClass("obj8", LocalDate.of(2014, 1, 1), LocalDate.of(2018, 1, 1));
        obj9 = new EqualityTestClass("obj9", LocalDate.of(2016, 1, 1), LocalDate.of(2018, 1, 1));
//        Write each of the Spatial Unions
//        Disable merging, just skip existing objects
        this.reasoner.getMergeEngine().changeMergeOnLoad(false);
        this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.SPLIT, obj1, Arrays.asList(obj6, obj2, obj3, obj4, obj5), 0.9);
        this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.MERGED, obj9, Arrays.asList(obj5, obj6), 0.9);
        this.reasoner.addTrestleObjectSplitMerge(TrestleEventType.SPLIT, obj2, Arrays.asList(obj7, obj8), 0.9);

        unionWalker = this.reasoner.getEqualityEngine();
        obj1Individual = this.tp.classParser.getIndividual(obj1);
        obj2Individual = this.tp.classParser.getIndividual(obj2);
        obj3Individual = this.tp.classParser.getIndividual(obj3);
        obj4Individual = this.tp.classParser.getIndividual(obj4);
        obj5Individual = this.tp.classParser.getIndividual(obj5);
        obj6Individual = this.tp.classParser.getIndividual(obj6);
        obj7Individual = this.tp.classParser.getIndividual(obj7);
        obj8Individual = this.tp.classParser.getIndividual(obj8);
        obj9Individual = this.tp.classParser.getIndividual(obj9);
    }

    @Test
    public void testEqualityWalk() {


//        What is obj1 equal to in 2013?
        final List<OWLNamedIndividual> equivalentObjects = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj1Individual, LocalDate.of(2013, 3, 11));
        assertAll(() -> assertEquals(5, equivalentObjects.size(), "Should only have 5 equivalent objects in 2013"),
                () -> assertTrue(equivalentObjects.contains(obj2Individual), "Should have Obj2"),
                () -> assertTrue(!equivalentObjects.contains(obj8Individual), "Should not have Obj8"));

        final List<OWLNamedIndividual> obj2EqObjects = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj2Individual, LocalDate.of(2015, 3, 11));
        assertAll(() -> assertEquals(2, obj2EqObjects.size(), "Should have 2 equivalent objects"),
                () -> assertTrue(obj2EqObjects.contains(obj7Individual), "Should be partially equivalent to Obj7"));

//        What about 2014?
        final List<OWLNamedIndividual> eqObjects2 = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj1Individual, LocalDate.of(2014, 3, 11));
        assertAll(() -> assertEquals(6, eqObjects2.size(), "Should have 6 equivalent objects in 2014"),
                () -> assertTrue(eqObjects2.contains(obj8Individual), "Should have obj8"),
                () -> assertTrue(!eqObjects2.contains(obj2Individual), "Should not have Obj2"));

//        2015
        final List<OWLNamedIndividual> eqObjects3 = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj1Individual, LocalDate.of(2015, 3, 11));
        assertAll(() -> assertEquals(6, eqObjects3.size(), "Should have 6 equivalent objects in 2015"),
                () -> assertTrue(eqObjects3.contains(obj8Individual), "Should have obj8"),
                () -> assertTrue(!eqObjects3.contains(obj2Individual), "Should not have Obj2"));

//        2016
        final List<OWLNamedIndividual> eqObjects4 = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj1Individual, LocalDate.of(2016, 3, 11));
        assertAll(() -> assertEquals(5, eqObjects4.size(), "Should have 5 equivalent objects in 2016"),
                () -> assertTrue(eqObjects4.contains(obj9Individual), "Should have Obj9"),
                () -> assertTrue(!eqObjects4.contains(obj6Individual), "Should not have Obj6"));

        final List<OWLNamedIndividual> eqObjects56 = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj5Individual, obj6Individual), LocalDate.of(2016, 3, 11));
        assertAll(() -> assertEquals(1, eqObjects56.size(), "Should only have 1 equivalent object"),
                () -> assertTrue(eqObjects56.contains(obj9Individual), "Should be equal to Obj9"));

        final List<OWLNamedIndividual> eqObjects56Empty = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj5Individual), LocalDate.of(2016, 3, 11));
        assertAll(() -> assertEquals(0, eqObjects56Empty.size(), "Should have no equivalent objects"));

//        2017
        final List<OWLNamedIndividual> eqObjects5 = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj1Individual, LocalDate.of(2017, 3, 11));
        assertTrue(eqObjects5.isEmpty(), "Should have no equivalent objects in 2017");

//        Try to go backwards in time

//        Simple, walk backwards
        final List<OWLNamedIndividual> obj3EqObjects = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj3Individual, LocalDate.of(2013, 3, 11));
        assertAll(() -> assertEquals(1, obj3EqObjects.size(), "Should only have 1 equivalent object"),
                () -> assertTrue(obj3EqObjects.contains(obj3Individual), "Should be equal to self"));


        final List<OWLNamedIndividual> obj1EqObjects = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj2Individual, obj3Individual, obj4Individual, obj5Individual, obj6Individual), LocalDate.of(2012, 3, 11));
        assertAll(() -> assertEquals(1, obj1EqObjects.size(), "Should have 1 equivalent object"),
                () -> assertTrue(obj1EqObjects.contains(obj1Individual), "Should be equivalent to Obj1"));

//         With some missing members
        final List<OWLNamedIndividual> obj1EqObjectsEmpty = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj2Individual, obj3Individual, obj5Individual, obj6Individual), LocalDate.of(2012, 3, 11));
        assertTrue(obj1EqObjectsEmpty.isEmpty(), "Should not be equivalent to anything");

//        2016 -> 2014
        final List<OWLNamedIndividual> obj9EqObjects = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj9Individual, LocalDate.of(2014, 3, 11));
        assertAll(() -> assertEquals(2, obj9EqObjects.size(), "Should have 2 equivalent objects"),
                () -> assertTrue(obj9EqObjects.contains(obj6Individual), "Should have obj6"));

//        2016 -> 2013
        final List<OWLNamedIndividual> eqObjects2013 = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj7Individual, obj8Individual, obj3Individual, obj4Individual, obj9Individual), LocalDate.of(2013, 3, 11));
        assertAll(() -> assertEquals(5, eqObjects2013.size(), "Should have equivalent objects in 2013"),
                () -> assertTrue(eqObjects2013.contains(obj2Individual), "Should have obj2 as an equivalent object"),
                () -> assertTrue(!eqObjects2013.contains(obj7Individual), "Obj7 should not be a possible option"));

//        2016 -> 2012
        final List<OWLNamedIndividual> eqObjects2012 = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj7Individual, obj8Individual, obj3Individual, obj4Individual, obj9Individual), LocalDate.of(2012, 3, 11));
        assertAll(() -> assertEquals(1, eqObjects2012.size(), "Should have equivalent objects in 2012"),
                () -> assertTrue(eqObjects2012.contains(obj1Individual), "Should have obj1 as an equivalent object"));

//        2014 -> 2013
        final List<OWLNamedIndividual> obj78EqObjects = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj7Individual, obj8Individual), LocalDate.of(2013, 3, 11));
        assertAll(() -> assertEquals(1, obj78EqObjects.size(), "Should have 2 equivalent objects"),
                () -> assertTrue(obj78EqObjects.contains(obj2Individual), "Should be equivalent to obj2"));

        final List<OWLNamedIndividual> obj78EqObjectsEmpty = unionWalker.getEquivalentIndividuals(EqualityTestClass.class, obj7Individual, LocalDate.of(2013, 3, 11));
        assertTrue(obj78EqObjectsEmpty.isEmpty(), "Should not be equivalent to anything");

//        Check for objects that have invalid temporal directions
        assertThrows(IllegalStateException.class, () -> unionWalker.getEquivalentIndividuals(EqualityTestClass.class, Arrays.asList(obj2Individual, obj9Individual), LocalDate.of(2014, 3, 11)));
    }

    @Test
    public void testEqualityObjectCreation() {
        //        What is obj1 equal to in 2013?
        final Optional<List<EqualityTestClass>> obj1EqObjects = this.reasoner.getEquivalentObjects(EqualityTestClass.class, obj1Individual.getIRI(), LocalDate.of(2013, 3, 11));
        assertAll(() -> assertTrue(obj1EqObjects.isPresent(), "Should have equivalent objects"),
                () -> assertEquals(5, obj1EqObjects.get().size(), "Should have 5 objects"),
                () -> assertEquals(obj5, obj1EqObjects.get().stream().filter(obj -> tp.classParser.getIndividual(obj).equals(obj5Individual)).findFirst().get(), "Obj 5 should match"));

//        Try with multiple objects
        final Optional<List<EqualityTestClass>> obj56Eqobj = this.reasoner.getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj5Individual.getIRI(), obj6Individual.getIRI()), LocalDate.of(2016, 3, 11));
        assertAll(() -> assertTrue(obj56Eqobj.isPresent(), "Should have results"),
                () -> assertEquals(1, obj56Eqobj.get().size(), "Should have 1 result"),
                () -> assertEquals(obj9, obj56Eqobj.get().stream().findFirst().get(), "Should be equal to Obj9"));

//        Try for something that doesn't exist
        final Optional<List<EqualityTestClass>> obj1Empty = this.reasoner.getEquivalentObjects(EqualityTestClass.class, obj7Individual.getIRI(), LocalDate.of(2013, 3, 11));
        assertAll(() -> assertTrue(obj1Empty.isPresent(), "Should have results"),
                () -> assertTrue(obj1Empty.get().isEmpty(), "Should have empty result set"));

//        Try with an error
        final Optional<List<EqualityTestClass>> obj29Error = this.reasoner.getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj2Individual.getIRI(), obj9Individual.getIRI()), LocalDate.of(2014, 3, 11));
        assertTrue(!obj29Error.isPresent(), "Should have empty optional due to error");

    }

    @Test
    @SuppressWarnings({"dereference.of.nullable", "argument.type.incompatible"})
    public void unionTest() throws IOException {
        final TestClasses.ESRIPolygonTest originalObject;
        List<TestClasses.ESRIPolygonTest> splitObjects = new ArrayList<>();
//        Read in the individuals
        final InputStream originalStream = EqualityTestClass.class.getClassLoader().getResourceAsStream("98103.csv");
        final BufferedReader originalReader = new BufferedReader(new InputStreamReader(originalStream, StandardCharsets.UTF_8));
        try {
            final String[] firstLine = originalReader.readLine().split(";");
            originalObject = new TestClasses.ESRIPolygonTest(Integer.parseInt(firstLine[0]), (Polygon) GeometryEngine.geometryFromWkt(firstLine[1], 0, Geometry.Type.Polygon), LocalDate.of(2001, 1, 1));
            splitObjects.add(originalObject);
        } finally {
            originalReader.close();
            originalStream.close();
        }

//        Read in the dissolved ones
        final SharedTestUtils.ITestClassConstructor<TestClasses.ESRIPolygonTest, String> esriConstructor = (line -> {
            final String[] splitLine = line.split(";");
            return new TestClasses.ESRIPolygonTest(Integer.parseInt(splitLine[0]), (Polygon) GeometryEngine.geometryFromWkt(splitLine[1], 0, Geometry.Type.Polygon), LocalDate.of(2000, 1, 1));
        });

        splitObjects.addAll(SharedTestUtils.readFromCSV("98103_split.csv", esriConstructor));
        assertEquals(6, splitObjects.size(), "Should have 6 split objects");

//        Calculate equality
        final Optional<UnionEqualityResult<TestClasses.ESRIPolygonTest>> equalityResult = this.reasoner.getEqualityEngine().calculateSpatialUnion(splitObjects, INPUT_SR, 0.9);
        assertAll(() -> assertTrue(equalityResult.isPresent(), "Should have equality result"),
                () -> assertEquals(TrestleEventType.MERGED, equalityResult.get().getType(), "Should have detected a split"),
//                We need to do this, because we can't compare on doubles, plus rounding
                () -> assertTrue(equalityResult.get().getStrength() > 0.99, "Should be perfectly equal"));

//        Try for a union
        final TestClasses.ESRIPolygonTest northSeattle;
        List<TestClasses.ESRIPolygonTest> nsUnionObjects = new ArrayList<>();
        final InputStream northSeattleIS = EqualityTestClass.class.getClassLoader().getResourceAsStream("northseattle.csv");
        final BufferedReader northSeattleReader = new BufferedReader(new InputStreamReader(northSeattleIS, StandardCharsets.UTF_8));
        try {
            final String[] firstLine = northSeattleReader.readLine().split(";");
            northSeattle = new TestClasses.ESRIPolygonTest(Integer.parseInt(firstLine[0]), (Polygon) GeometryEngine.geometryFromWkt(firstLine[1], 0, Geometry.Type.Polygon), LocalDate.of(2002, 1, 1));
            nsUnionObjects.add(northSeattle);
        } finally {
            northSeattleReader.close();
            northSeattleIS.close();
        }

        final SharedTestUtils.ITestClassConstructor<TestClasses.ESRIPolygonTest, String> esriSplitConstructor = (line -> {
            final String[] splitLine = line.split(";");
            return new TestClasses.ESRIPolygonTest(Integer.parseInt(splitLine[0]), (Polygon) GeometryEngine.geometryFromWkt(splitLine[1], 0, Geometry.Type.Polygon), LocalDate.of(2001, 1, 1));
        });

        nsUnionObjects.addAll(SharedTestUtils.readFromCSV("northseattle_split.csv", esriSplitConstructor));

        assertEquals(6, nsUnionObjects.size(), "Should have 6 objects");

        final Optional<UnionEqualityResult<TestClasses.ESRIPolygonTest>> unionResult = this.reasoner.getEqualityEngine().calculateSpatialUnion(nsUnionObjects, INPUT_SR, 0.9);
        assertAll(() -> assertTrue(unionResult.isPresent(), "Should have equality"),
                () -> assertEquals(northSeattle, unionResult.get().getUnionObject(), "Should be a union of North Seattle"),
                () -> assertEquals(TrestleEventType.MERGED, unionResult.get().getType(), "Should be split"),
                () -> assertTrue(unionResult.get().getStrength() > 0.99, "Should be really equal"));

//        Try for a split

        final SharedTestUtils.ITestClassConstructor<TestClasses.ESRIPolygonTest, String> fakeSplitConstructor = (line -> {
            final String[] split = line.split(";");
            return new TestClasses.ESRIPolygonTest(Integer.parseInt(split[0]), (Polygon) GeometryEngine.geometryFromWkt(split[1], 0, Geometry.Type.Polygon), LocalDate.of(2003, 1, 1));
        });
        List<TestClasses.ESRIPolygonTest> fakeSplitObjects = SharedTestUtils.readFromCSV("northseattle_fakesplit.csv", fakeSplitConstructor);

        fakeSplitObjects.add(northSeattle);

        final Optional<UnionEqualityResult<TestClasses.ESRIPolygonTest>> nsSplitResult = this.reasoner.getEqualityEngine().calculateSpatialUnion(fakeSplitObjects, INPUT_SR, 0.9);
        assertAll(() -> assertTrue(nsSplitResult.isPresent(), "Should have equality"),
                () -> assertEquals(northSeattle, nsSplitResult.get().getUnionObject(), "Should be a split of North Seattle"),
                () -> assertEquals(TrestleEventType.SPLIT, nsSplitResult.get().getType(), "Should be split"),
                () -> assertTrue(nsSplitResult.get().getStrength() > 0.99, "Should be pretty much equal"));

//        Should not have equality if we drop some of the elements
        final List<TestClasses.ESRIPolygonTest> missingSplitObjects = fakeSplitObjects
                .stream()
                .filter(obj -> obj.getAdm0_code() != 26)
                .collect(Collectors.toList());
        final Optional<UnionEqualityResult<TestClasses.ESRIPolygonTest>> missingSplitResult = this.reasoner.getEqualityEngine().calculateSpatialUnion(missingSplitObjects, INPUT_SR, 0.9);
        assertTrue(!missingSplitResult.isPresent(), "Should not have equality");

//        Try to write the objects
        this.reasoner.addTrestleObjectSplitMerge(equalityResult.get().getType(), equalityResult.get().getUnionObject(), new ArrayList<>(equalityResult.get().getUnionOf()), equalityResult.get().getStrength());
        this.reasoner.addTrestleObjectSplitMerge(nsSplitResult.get().getType(), nsSplitResult.get().getUnionObject(), new ArrayList<>(nsSplitResult.get().getUnionOf()), nsSplitResult.get().getStrength());
        this.reasoner.addTrestleObjectSplitMerge(unionResult.get().getType(), unionResult.get().getUnionObject(), new ArrayList<>(unionResult.get().getUnionOf()), unionResult.get().getStrength());

        final Optional<List<TestClasses.ESRIPolygonTest>> eqObj2000 = this.reasoner.getEquivalentObjects(TestClasses.ESRIPolygonTest.class, IRI.create(OVERRIDE_PREFIX, "98103"), LocalDate.of(2000, 1, 1));
        assertAll(() -> assertTrue(eqObj2000.isPresent(), "Should have objects"),
                () -> assertEquals(5, eqObj2000.get().size(), "Should have 5 objects"));

    }

    @Override
    protected String getTestName() {
        return "equality_tests";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(EqualityTestClass.class, TestClasses.ESRIPolygonTest.class);
    }

    @DatasetClass(name = "equality-test")
    public static class EqualityTestClass implements Serializable {
        private static final long serialVersionUID = 42L;

        private final String id;
        private final LocalDate startDate;
        private final LocalDate endDate;

        public EqualityTestClass(String id, LocalDate startDate, LocalDate endDate) {
            this.id = id;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @IndividualIdentifier
        public String getId() {
            return id;
        }

        @StartTemporal
        public LocalDate getStartDate() {
            return startDate;
        }

        @EndTemporal
        public LocalDate getEndDate() {
            return endDate;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EqualityTestClass that = (EqualityTestClass) o;

            if (!id.equals(that.id)) return false;
            if (!startDate.equals(that.startDate)) return false;
            return endDate.equals(that.endDate);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + startDate.hashCode();
            result = 31 * result + endDate.hashCode();
            return result;
        }
    }
}

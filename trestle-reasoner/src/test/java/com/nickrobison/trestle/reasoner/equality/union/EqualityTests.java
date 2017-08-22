package com.nickrobison.trestle.reasoner.equality.union;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.equality.EqualityEngineImpl;
import com.nickrobison.trestle.types.events.TrestleEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class EqualityTests extends AbstractReasonerTest {


    private EqualityTestClass obj1;
    private EqualityTestClass obj2;
    private EqualityTestClass obj3;
    private EqualityTestClass obj4;
    private EqualityTestClass obj5;
    private EqualityTestClass obj6;
    private EqualityTestClass obj7;
    private EqualityTestClass obj8;
    private EqualityTestClass obj9;

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
    }

    @Test
    public void testEqualityWalk() {
        final OWLNamedIndividual obj1Individual = this.tp.classParser.getIndividual(obj1);
        final OWLNamedIndividual obj2Individual = this.tp.classParser.getIndividual(obj2);
        final OWLNamedIndividual obj3Individual = this.tp.classParser.getIndividual(obj3);
        final OWLNamedIndividual obj4Individual = this.tp.classParser.getIndividual(obj4);
        final OWLNamedIndividual obj5Individual = this.tp.classParser.getIndividual(obj5);
        final OWLNamedIndividual obj6Individual = this.tp.classParser.getIndividual(obj6);
        final OWLNamedIndividual obj7Individual = this.tp.classParser.getIndividual(obj7);
        final OWLNamedIndividual obj8Individual = this.tp.classParser.getIndividual(obj8);
        final OWLNamedIndividual obj9Individual = this.tp.classParser.getIndividual(obj9);


//        What is obj1 equal to in 2013?
        final List<OWLNamedIndividual> equivalentObjects = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj1Individual, LocalDate.of(2013, 3, 11));
        assertAll(() -> assertEquals(5, equivalentObjects.size(), "Should only have 5 equivalent objects in 2013"),
                () -> assertTrue(equivalentObjects.contains(obj2Individual), "Should have Obj2"),
                () -> assertTrue(!equivalentObjects.contains(obj8Individual), "Should not have Obj8"));

        final List<OWLNamedIndividual> obj2EqObjects = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj2Individual, LocalDate.of(2014, 3, 11));
        assertAll(() -> assertEquals(2, obj2EqObjects.size(), "Should have 2 equivalent objects"),
                () -> assertTrue(obj2EqObjects.contains(obj7Individual), "Should be partially equivalent to Obj7"));

//        What about 2014?
        final List<OWLNamedIndividual> eqObjects2 = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj1Individual, LocalDate.of(2014, 3, 11));
        assertAll(() -> assertEquals(6, eqObjects2.size(), "Should have 6 equivalent objects in 2014"),
                () -> assertTrue(eqObjects2.contains(obj8Individual), "Should have obj8"),
                () -> assertTrue(!eqObjects2.contains(obj2Individual), "Should not have Obj2"));

//        2015
        final List<OWLNamedIndividual> eqObjects3 = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj1Individual, LocalDate.of(2015, 3, 11));
        assertAll(() -> assertEquals(6, eqObjects3.size(), "Should have 6 equivalent objects in 2015"),
                () -> assertTrue(eqObjects3.contains(obj8Individual), "Should have obj8"),
                () -> assertTrue(!eqObjects3.contains(obj2Individual), "Should not have Obj2"));

//        2016
        final List<OWLNamedIndividual> eqObjects4 = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj1Individual, LocalDate.of(2016, 3, 11));
        assertAll(() -> assertEquals(5, eqObjects4.size(), "Should have 5 equivalent objects in 2016"),
                () -> assertTrue(eqObjects4.contains(obj9Individual), "Should have Obj9"),
                () -> assertTrue(!eqObjects4.contains(obj6Individual), "Should not have Obj6"));

        final List<OWLNamedIndividual> eqObjects56 = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj5Individual, obj6Individual), LocalDate.of(2016, 3, 11));
        assertAll(() -> assertEquals(1, eqObjects56.size(), "Should only have 1 equivalent object"),
                () -> assertTrue(eqObjects56.contains(obj9Individual), "Should be equal to Obj9"));

//        2017
        final List<OWLNamedIndividual> eqObjects5 = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj1Individual, LocalDate.of(2017, 3, 11));
        assertTrue(eqObjects5.isEmpty(), "Should have no equivalent objects in 2017");

//        Try to go backwards in time

//        Simple, walk backwards
        final List<OWLNamedIndividual> obj3EqObjects = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj3Individual, LocalDate.of(2013, 3, 11));
        assertAll(() -> assertEquals(1, obj3EqObjects.size(), "Should only have 1 equivalent object"),
                () -> assertTrue(obj3EqObjects.contains(obj3Individual), "Should be equal to self"));


         final List<OWLNamedIndividual> obj1EqObjects = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj2Individual, obj3Individual, obj4Individual, obj5Individual, obj6Individual), LocalDate.of(2012, 3, 11));
         assertAll(() -> assertEquals(1, obj1EqObjects.size(), "Should have 1 equivalent object"),
                 () -> assertTrue(obj1EqObjects.contains(obj1Individual), "Should be equivalent to Obj1"));

//         With some missing members
        final List<OWLNamedIndividual> obj1EqObjectsEmpty = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj2Individual, obj3Individual, obj5Individual, obj6Individual), LocalDate.of(2012, 3, 11));
        assertTrue(obj1EqObjectsEmpty.isEmpty(), "Should not be equivalent to anything");

//        2016 -> 2014
        final List<OWLNamedIndividual> obj9EqObjects = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, obj9Individual, LocalDate.of(2014, 3, 11));
        assertAll(() -> assertEquals(2, obj9EqObjects.size(), "Should have 2 equivalent objects"),
                () -> assertTrue(obj9EqObjects.contains(obj6Individual), "Should have obj6"));

//        2016 -> 2013
        final List<OWLNamedIndividual> eqObjects2013 = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj7Individual, obj8Individual, obj3Individual, obj4Individual, obj9Individual), LocalDate.of(2014, 3, 11));
        assertAll(() -> assertEquals(5, eqObjects2013.size(), "Should have equivalent objects in 2013"),
                () -> assertTrue(eqObjects2013.contains(obj2Individual), "Should have obj2 as an equivalent object"),
                () -> assertTrue(!eqObjects2013.contains(obj7Individual), "Obj7 should not be a possible option"));

//        2016 -> 2012
        final List<OWLNamedIndividual> eqObjects2012 = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj7Individual, obj8Individual, obj3Individual, obj4Individual, obj9Individual), LocalDate.of(2014, 3, 11));
        assertAll(() -> assertEquals(1, eqObjects2012.size(), "Should have equivalent objects in 2012"),
                () -> assertTrue(eqObjects2013.contains(obj1Individual), "Should have obj1 as an equivalent object"));

//        2014 -> 2013
        final List<OWLNamedIndividual> obj78EqObjects = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj7Individual, obj8Individual), LocalDate.of(2013, 3, 11));
        assertAll(() -> assertEquals(1, obj78EqObjects.size(), "Should have 2 equivalent objects"),
                () -> assertTrue(obj78EqObjects.contains(obj2Individual), "Should be equivalent to obj2"));

        final List<OWLNamedIndividual> obj78EqObjectsEmpty = ((EqualityEngineImpl) this.reasoner.getEqualityEngine()).getEquivalentObjects(EqualityTestClass.class, Arrays.asList(obj7Individual, obj8Individual), LocalDate.of(2013, 3, 11));
        assertTrue(obj78EqObjectsEmpty.isEmpty(), "Should not be equivalent to anything");

    }


    @Override
    protected String getTestName() {
        return "equality_tests";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(EqualityTestClass.class);
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
    }
}

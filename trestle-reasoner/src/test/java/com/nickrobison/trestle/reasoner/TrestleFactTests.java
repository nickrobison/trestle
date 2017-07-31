package com.nickrobison.trestle.reasoner;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.Spatial;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.NoValidStateException;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.merge.ExistenceStrategy;
import com.nickrobison.trestle.reasoner.merge.MergeStrategy;
import com.nickrobison.trestle.reasoner.merge.TrestleMergeConflict;
import com.nickrobison.trestle.reasoner.merge.TrestleMergeException;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TrestleFactTests extends AbstractReasonerTest {
    @Override
    protected String getTestName() {
        return "fact_tests";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestClasses.FactVersionTest.class, TestClasses.GAULTestClass.class, BoundedFact.class);
    }

    @Test
    public void testFactValidityMerge() throws TrestleClassException, MissingOntologyEntity {
//        Disable existence merge
        this.reasoner.getMergeEngine().changeDefaultExistenceStrategy(ExistenceStrategy.Ignore);
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
        final TestClasses.FactVersionTest v1Return = reasoner.readTrestleObject(v1.getClass(), tp.classParser.getIndividual(v1).getIRI(), false);
        assertEquals(v1, v1Return, "Should be equal to V1");
        reasoner.writeTrestleObject(v2);
        final TestClasses.FactVersionTest v2Return = reasoner.readTrestleObject(v2.getClass(), tp.classParser.getIndividual(v1).getIRI(), false);
        assertEquals(v2, v2Return, "Should be equal to V2");
        reasoner.writeTrestleObject(v3);
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
        assertEquals(7, trestleIndividual.getFacts().size(), "Should have 5 facts over the lifetime of the object");

//        Try to manually add a new value
        reasoner.addFactToTrestleObject(v3.getClass(), "test-object", "testValue", "test value three", LocalDate.of(2007, 3, 26), null, null);
        reasoner.addFactToTrestleObject(v3.getClass(), "test-object", "wkt", "POINT(1.71255092695307 -30.572028714467507)", LocalDate.of(2017, 1, 1), null);

//        Try to get some fact values
        final Optional<List<Object>> values = reasoner.getFactValues(v3.getClass(), "test-object", "testValue", null, null, null);
        assertAll(() -> assertTrue(values.isPresent(), "Should have fact values"),
                () -> assertEquals(5, values.get().size(), "Should have 5 fact values"));

        final Optional<List<Object>> wktValues = reasoner.getFactValues(v3.getClass(), "test-object", "wkt", LocalDate.of(1988, 3, 26), LocalDate.of(1995, 3, 26), null);
        assertAll(() -> assertTrue(wktValues.isPresent(), "Should have wkt values"),
                () -> assertEquals(2, wktValues.get().size(), "Should only have 2 wkt values"));

//        Test merging with overlapping (non-continuing facts)
        final TestClasses.GAULTestClass overlappingFactTest = new TestClasses.GAULTestClass(4115, "test-fact-object", LocalDate.of(1989, 3, 26).atStartOfDay(), "POINT(0.71255092695307 -25.572028714467507)");
        reasoner.writeTrestleObject(overlappingFactTest);
//        Should throw an exception with both methods
        final TestClasses.GAULTestClass updatedFactClass = new TestClasses.GAULTestClass(9944, "test-fact-object", LocalDate.of(1989, 5, 14).atStartOfDay(), "POINT(0.71255092695307 -25.572028714467507)");
        assertThrows(TrestleMergeConflict.class, () -> reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "adm0_code", 9944, LocalDate.of(1989, 5, 14).atStartOfDay(), null));
        assertThrows(TrestleMergeConflict.class, () -> reasoner.writeTrestleObject(updatedFactClass));
//        Read out the same object
        final TestClasses.@NonNull GAULTestClass originalObject = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", LocalDate.of(1989, 5, 14), null);
        assertEquals(overlappingFactTest, originalObject, "Should match the original object");

//        Change method and try again
        this.reasoner.getMergeEngine().changeDefaultMergeStrategy(MergeStrategy.ExistingFacts);
        reasoner.writeTrestleObject(updatedFactClass);
        final TestClasses.@NonNull GAULTestClass updatedObject = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", LocalDate.of(1989, 5, 15), null);
        assertEquals(updatedFactClass, updatedObject);
        reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "wkt", "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))", LocalDate.of(1989, 5, 14), null, null);
        final TestClasses.GAULTestClass newWKT = new TestClasses.GAULTestClass(9944, "test-fact-object", LocalDate.of(1989, 03, 26).atStartOfDay(), "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))");
        final TestClasses.@NonNull GAULTestClass updatedWKT = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", LocalDate.of(1989, 5, 15), null);
        assertEquals(newWKT, updatedWKT);

//        Try for no merge.
        this.reasoner.getMergeEngine().changeDefaultMergeStrategy(MergeStrategy.NoMerge);
        assertThrows(TrestleMergeConflict.class, () -> reasoner.writeTrestleObject(updatedFactClass));
        assertThrows(TrestleMergeConflict.class, () -> reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "wkt", "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))", LocalDate.of(1989, 5, 14), null, null));
//        Try to add facts in the future.
        this.reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "adm0_code", 1234, LocalDate.of(1990, 5, 14).atStartOfDay(), null, null);
        final Optional<List<Object>> factValues = reasoner.getFactValues(TestClasses.GAULTestClass.class, "test-fact-object", "adm0_code", null, null, null);
        assertEquals(4, factValues.get().size(), "Should have 4 values for ADM0_Code");

////        Test database temporals
//        reasoner.getMetricsEngine().exportData(new File("./target/api-test-fact-validity-metrics.csv"));
    }

    @Test
    public void testObjectExistenceMerge() throws TrestleClassException, MissingOntologyEntity {
        final BoundedFact boundedFact = new BoundedFact("bounded-object",
                LocalDate.of(1989, 3, 26),
                LocalDate.of(1994, 3, 26),
                "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))",
                "test value one");

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

        final TestClasses.FactVersionTest beforeExists = new TestClasses.FactVersionTest("test-object",
                LocalDate.of(1980, 3, 26),
                "POINT(0.71255092695307 -25.572028714467507)",
                "test value before exists");

        //        During tests
        reasoner.writeTrestleObject(v1);
        assertThrows(TrestleMergeException.class, () -> reasoner.writeTrestleObject(beforeExists));

//        Extend existence
        this.reasoner.getMergeEngine().changeDefaultExistenceStrategy(ExistenceStrategy.Extend);
//        Try to write the before object, again
        this.reasoner.writeTrestleObject(beforeExists);
        final TestClasses.FactVersionTest extendedFact = this.reasoner.readTrestleObject(TestClasses.FactVersionTest.class, "test-object", LocalDate.of(1984, 5, 14), null);
        assertEquals(LocalDate.of(1980, 3, 26), extendedFact.getValidFrom(), "Should have extended existsFrom");

//        Try to write forward fact
        this.reasoner.writeTrestleObject(boundedFact);
//        Try to add continuing fact to non-continuing object
        assertThrows(TrestleMergeException.class, () -> this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "testValue", "new value, after exists", LocalDate.of(1989, 3, 26).plusYears(6), null, null));
//        Need to add all the facts, because otherwise we'll get an invalid state exception
        this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "testValue", "new value, after exists", LocalDate.of(1989, 3, 26).plusYears(6), LocalDate.of(1989, 3, 26).plusYears(10), null);
        this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "id", "bounded-object", LocalDate.of(1989, 3, 26).plusYears(6), LocalDate.of(1989, 3, 26).plusYears(10), null);
        this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "wkt", "POINT(0.71255092695307 -25.572028714467507)", LocalDate.of(1989, 3, 26).plusYears(6), LocalDate.of(1989, 3, 26).plusYears(10), null);
        @NonNull final BoundedFact extendedBoundedFact = this.reasoner.readTrestleObject(BoundedFact.class, "bounded-object", LocalDate.of(1989, 3, 26).plusYears(7), null);
        assertEquals(LocalDate.of(1989, 3, 26).plusYears(10).plusDays(1), extendedBoundedFact.getValidTo(), "Should have extended ending temporal");


//        Ignore


    }

    @DatasetClass(name = "BoundedVersionTest")
    public static class BoundedFact implements Serializable {
        private static final long serialVersionUID = 42L;

        @IndividualIdentifier
        public final String id;
        private final LocalDate validFrom;
        private final LocalDate validTo;
        private final String wkt;
        public final String testValue;


        public BoundedFact(String id, LocalDate validFrom, LocalDate validTo, String wkt, String testValue) {
            this.id = id;
            this.validFrom = validFrom;
            this.validTo = validTo;
            this.wkt = wkt;
            this.testValue = testValue;
        }

        @Spatial
        public String getWkt() {
            return this.wkt;
        }

        @StartTemporal
        public LocalDate getValidFrom() {
            return this.validFrom;
        }

        @EndTemporal
        public LocalDate getValidTo() {
            return this.validTo;
        }
    }

}

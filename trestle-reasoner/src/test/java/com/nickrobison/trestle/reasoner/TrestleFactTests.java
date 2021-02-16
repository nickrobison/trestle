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
import com.nickrobison.trestle.reasoner.engines.merge.ExistenceStrategy;
import com.nickrobison.trestle.reasoner.engines.merge.MergeStrategy;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeConflict;
import com.nickrobison.trestle.reasoner.engines.merge.TrestleMergeException;
import com.nickrobison.trestle.types.TrestleIndividual;
import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.events.TrestleEventType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
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
    @Disabled
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
        reasoner.writeTrestleObject(v1).blockingAwait();
//        Check that events exist
        List<TrestleEvent> events = reasoner.getIndividualEvents(v1.getClass(), v1.id).toList().blockingGet();
        assertAll(() -> assertEquals(1, events.size(), "Should only have created event"),
                () -> assertEquals(v1.getValidFrom(), events.get(0).getAtTemporal(), "CREATED event should equal valid from"));
        final TestClasses.FactVersionTest v1Return = reasoner.readTrestleObject(v1.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, null).blockingGet();
        assertEquals(v1, v1Return, "Should be equal to V1");
        reasoner.writeTrestleObject(v2).blockingAwait();
        final List<TrestleEvent> events2 = reasoner.getIndividualEvents(v2.getClass(), v2.id).toList().blockingGet();
        assertAll(() -> assertEquals(1, events2.size(), "Should only have created event"),
                () -> assertEquals(v1.getValidFrom(), events2.get(0).getAtTemporal(), "CREATED event should equal V1 Valid from"));

        final TestClasses.FactVersionTest v2Return = reasoner.readTrestleObject(v2.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, null).blockingGet();
        assertEquals(v2, v2Return, "Should be equal to V2");
        reasoner.writeTrestleObject(v3).blockingAwait();
        final TestClasses.FactVersionTest v3Return = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, null).blockingGet();
        assertEquals(v3, v3Return, "Should be equal to V3");
//        Try for specific points in time
        final TestClasses.FactVersionTest v1ReturnHistorical = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, LocalDate.of(1990, 3, 26), null, null).blockingGet();
        assertEquals(v1, v1ReturnHistorical, "Historical query should be equal to V1");
        final TestClasses.FactVersionTest v2ReturnHistorical = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, LocalDate.of(1999, 3, 26), null, null).blockingGet();
        assertEquals(v2, v2ReturnHistorical, "Historical query should be equal to V2");
        final TestClasses.FactVersionTest v3ReturnHistorical = reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), false, LocalDate.of(2016, 3, 26), null, null).blockingGet();
        assertEquals(v3, v3ReturnHistorical, "Historical query should be equal to V3");
        assertThrows(NoValidStateException.class, () -> reasoner.readTrestleObject(v3.getClass(), tp.classParser.getIndividual(v1).getIRI(), true, LocalDate.of(1980, 3, 26), null, null));

//        Check to make sure we have all the facts
        final TrestleIndividual trestleIndividual = reasoner.getTrestleIndividual("test-object").blockingGet();
        assertEquals(7, trestleIndividual.getFacts().size(), "Should have 5 facts over the lifetime of the object");

//        Try to manually add a new value
        reasoner.addFactToTrestleObject(v3.getClass(), "test-object", "testValue", "test value three", LocalDate.of(2007, 3, 26), null, null).blockingAwait();
        reasoner.addFactToTrestleObject(v3.getClass(), "test-object", "wkt", "POINT(1.71255092695307 -30.572028714467507)", LocalDate.of(2017, 1, 1), null).blockingAwait();

//        Try for invalid fact name
        assertThrows(IllegalArgumentException.class, () -> reasoner.getFactValues(v3.getClass(), "test-object", "missing-fact", null, null, null));

//        Try to get some fact values
        final List<Object> values = reasoner.getFactValues(v3.getClass(), "test-object", "testValue", null, null, null).toList().blockingGet();
        assertEquals(5, values.size(), "Should have 5 fact values");

        final List<Object> wktValues = reasoner.getFactValues(v3.getClass(), "test-object", "wkt", LocalDate.of(1988, 3, 26), LocalDate.of(1995, 3, 26), null).toList().blockingGet();
        assertEquals(2, wktValues.size(), "Should only have 2 wkt values");

//        Test merging with overlapping (non-continuing facts)
        final TestClasses.GAULTestClass overlappingFactTest = new TestClasses.GAULTestClass(4115, "test-fact-object", LocalDate.of(1989, 3, 26).atStartOfDay(), LocalDate.of(1990, 3, 26).atStartOfDay(), "POINT(0.71255092695307 -25.572028714467507)");
        reasoner.writeTrestleObject(overlappingFactTest);
//        Should throw an exception with both methods
        final TestClasses.GAULTestClass updatedFactClass = new TestClasses.GAULTestClass(9944, "test-fact-object", LocalDate.of(1989, 5, 14).atStartOfDay(), LocalDate.of(1990, 5, 14).atStartOfDay(), "POINT(0.71255092695307 -25.572028714467507)");
        assertThrows(TrestleMergeConflict.class, () -> reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "adm0_code", 9944, LocalDate.of(1989, 5, 14).atStartOfDay(), null).blockingAwait());
        assertThrows(TrestleMergeConflict.class, () -> reasoner.writeTrestleObject(updatedFactClass).blockingAwait());
//        Read out the same object
        final TestClasses.GAULTestClass originalObject = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", LocalDate.of(1989, 5, 14), null).blockingGet();
        assertEquals(overlappingFactTest, originalObject, "Should match the original object");

//        Change method and try again
        this.reasoner.getMergeEngine().changeDefaultMergeStrategy(MergeStrategy.ExistingFacts);
        reasoner.writeTrestleObject(updatedFactClass).blockingAwait();
        final TestClasses.GAULTestClass updatedObject = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", LocalDate.of(1989, 5, 15), null).blockingGet();
        assertEquals(updatedFactClass, updatedObject);
        reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "wkt", "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))", LocalDate.of(1989, 5, 14), null, null).blockingAwait();
        final TestClasses.GAULTestClass newWKT = new TestClasses.GAULTestClass(9944, "test-fact-object", LocalDate.of(1989, 03, 26).atStartOfDay(), LocalDate.of(1990, 03, 26).atStartOfDay(), "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))");
        final TestClasses.GAULTestClass updatedWKT = reasoner.readTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", LocalDate.of(1989, 5, 15), null).blockingGet();
        assertEquals(newWKT, updatedWKT);

//        Try for no merge.
        this.reasoner.getMergeEngine().changeDefaultMergeStrategy(MergeStrategy.NoMerge);
        assertThrows(TrestleMergeConflict.class, () -> reasoner.writeTrestleObject(updatedFactClass).blockingAwait());
        assertThrows(TrestleMergeConflict.class, () -> reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "wkt", "POLYGON ((30.71255092695307 -25.572028714467507, 30.71255092695307 -24.57695170392701, 34.23641567304696 -24.57695170392701, 34.23641567304696 -25.572028714467507, 30.71255092695307 -25.572028714467507))", LocalDate.of(1989, 5, 14), null, null).blockingAwait());
//        Try to add facts in the future.
        this.reasoner.addFactToTrestleObject(TestClasses.GAULTestClass.class, "test-fact-object", "adm0_code", 1234, LocalDate.of(1990, 5, 14).atStartOfDay(), null, null).blockingAwait();
        final List<Object> factValues = reasoner.getFactValues(TestClasses.GAULTestClass.class, "test-fact-object", "adm0_code", null, null, null).toList().blockingGet();
        assertEquals(4, factValues.size(), "Should have 4 values for ADM0_Code");

////        Test database temporals
//        reasoner.getMetricsEngine().exportData(new File("./target/api-test-fact-validity-metrics.csv"));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
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
        reasoner.writeTrestleObject(v1).blockingAwait();
        assertThrows(TrestleMergeException.class, () -> reasoner.writeTrestleObject(beforeExists).blockingAwait());

//        Extend existence
        this.reasoner.getMergeEngine().changeDefaultExistenceStrategy(ExistenceStrategy.Extend);
//        Try to write the before object, again
        this.reasoner.writeTrestleObject(beforeExists).blockingAwait();
        final TestClasses.FactVersionTest extendedFact = this.reasoner.readTrestleObject(TestClasses.FactVersionTest.class, "test-object", LocalDate.of(1984, 5, 14), null).blockingGet();
        assertEquals(LocalDate.of(1980, 3, 26), extendedFact.getValidFrom(), "Should have extended existsFrom");
//        Check that the CREATED event was extended correctly
        final List<TrestleEvent> beforeEvents = this.reasoner.getIndividualEvents(TestClasses.FactVersionTest.class, "test-object").toList().blockingGet();
        assertAll(() -> assertEquals(1, beforeEvents.size(), "Should only have CREATED event"),
                () -> assertEquals(beforeExists.getValidFrom(), beforeEvents.stream().findFirst().get().getAtTemporal(), "CREATED event should equal new existsFrom"));

//        Try to write forward fact
        this.reasoner.writeTrestleObject(boundedFact).blockingAwait();
        final List<TrestleEvent> boundedEvents = this.reasoner.getIndividualEvents(boundedFact.getClass(), boundedFact.id).toList().blockingGet();
        assertAll(() -> assertEquals(2, boundedEvents.size(), "Should have CREATED and DESTROYED events"),
                () -> assertEquals(boundedFact.validFrom, boundedEvents.stream().filter(event -> event.getType() == TrestleEventType.CREATED).findFirst().get().getAtTemporal(), "CREATED should equal existsFrom"),
                () -> assertEquals(boundedFact.validTo, boundedEvents.stream().filter(event -> event.getType() == TrestleEventType.DESTROYED).findFirst().get().getAtTemporal(), "DESTROYED should equal existsTo"));

//        Try to add continuing fact to non-continuing object
        assertThrows(TrestleMergeException.class, () -> this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "testValue", "new value, after exists", LocalDate.of(1989, 3, 26).plusYears(6), null, null).blockingAwait());
//        Need to add all the facts, because otherwise we'll get an invalid state exception
        this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "testValue", "new value, after exists", LocalDate.of(1989, 3, 26).plusYears(6), LocalDate.of(1989, 3, 26).plusYears(10), null).blockingAwait();
        this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "id", "bounded-object", LocalDate.of(1989, 3, 26).plusYears(6), LocalDate.of(1989, 3, 26).plusYears(10), null).blockingAwait();
        this.reasoner.addFactToTrestleObject(BoundedFact.class, "bounded-object", "wkt", "POINT(0.71255092695307 -25.572028714467507)", LocalDate.of(1989, 3, 26).plusYears(6), LocalDate.of(1989, 3, 26).plusYears(10), null).blockingAwait();
        @NonNull final BoundedFact extendedBoundedFact = this.reasoner.readTrestleObject(BoundedFact.class, "bounded-object", LocalDate.of(1989, 3, 26).plusYears(7), null).blockingGet();
        final  List<TrestleEvent> boundedExtendedEvents = this.reasoner.getIndividualEvents(BoundedFact.class, "bounded-object").toList().blockingGet();
        assertEquals(LocalDate.of(1989, 3, 26).plusYears(10).plusDays(1), extendedBoundedFact.getValidTo(), "Should have extended ending temporal");
        assertAll(() -> assertEquals(2, boundedExtendedEvents.size(), "Should have CREATED and DESTROYED events"),
                () -> assertEquals(boundedFact.validFrom, boundedExtendedEvents.stream().filter(event -> event.getType() == TrestleEventType.CREATED).findFirst().get().getAtTemporal(), "CREATED should equal existsFrom"),
                () -> assertEquals(LocalDate.of(1989, 3, 26).plusYears(10).plusDays(1), boundedExtendedEvents.stream().filter(event -> event.getType() == TrestleEventType.DESTROYED).findFirst().get().getAtTemporal(), "DESTROYED should equal existsTo"));
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

package com.nickrobison.trestle.reasoner.engines.temporal;

import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.reasoner.parser.TrestleParserProvider;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalEngineTest {

    private TrestleParser parser;
    private TemporalEngine engine;


    @BeforeEach
    public void setup() {
//        Setup the mock values
        parser = new TrestleParserProvider("http://test-prefix.com/").get();
        engine = new TemporalEngine(parser);
    }


    @Test
    public void intervalToIntervalTests() {

//        Before/After
        final IntervalTestObject beforeObjectA = new IntervalTestObject("object-1", LocalDate.of(1989, 3, 26), LocalDate.of(1989, 4, 15));
        final IntervalTestObject beforeObjectB = new IntervalTestObject("object-2", LocalDate.of(1990, 5, 15), LocalDate.of(1990, 6, 7));

        final TemporalComparisonReport aBeforeBCompare = engine.compareObjects(beforeObjectA, beforeObjectB);
        assertAll(() -> assertEquals(1, aBeforeBCompare.getRelations().size(), "Should only have 1 relation"),
                () -> assertTrue(aBeforeBCompare.getRelations().contains(ObjectRelation.BEFORE), "Should have before relation"));
        final TemporalComparisonReport bAfterACompare = engine.compareObjects(beforeObjectB, beforeObjectA);
        assertAll(() -> assertEquals(1, bAfterACompare.getRelations().size(), "Should only have 1 relation"),
                () -> assertTrue(bAfterACompare.getRelations().contains(ObjectRelation.AFTER), "Should have after relation"));

//        During
        final IntervalTestObject duringA = new IntervalTestObject("during-a", LocalDate.of(1990, 5, 14), LocalDate.of(2016, 1, 1));
        final IntervalTestObject duringB = new IntervalTestObject("during-b", LocalDate.of(1989, 3, 26), LocalDate.of(2016, 3, 26));
        final TemporalComparisonReport duringAReport = engine.compareObjects(duringA, duringB);
        assertAll(() -> assertEquals(1, duringAReport.getRelations().size(), "Should only have 1 relation"),
                () -> assertTrue(duringAReport.getRelations().contains(ObjectRelation.DURING), "Should have during relation"));
        final TemporalComparisonReport duringBReport = engine.compareObjects(duringB, duringA);
        assertEquals(0, duringBReport.getRelations().size(), "Should not have any relations");

//        Starts
        final IntervalTestObject aStarts = new IntervalTestObject("starts-a", LocalDate.of(2017, 3, 11), LocalDate.of(2017, 3, 26));
        final IntervalTestObject bStarts = new IntervalTestObject("starts-b", LocalDate.of(2017, 3, 11), LocalDate.of(2017, 5, 14));
        final TemporalComparisonReport startsComparison = engine.compareObjects(aStarts, bStarts);
        assertAll(() -> assertEquals(1, startsComparison.getRelations().size(), "should only have 1 relation"),
                () -> assertTrue(startsComparison.getRelations().contains(ObjectRelation.STARTS), "Should have starts Relation"));

        final TemporalComparisonReport noStartsCompare = engine.compareObjects(bStarts, aStarts);
        assertEquals(0, noStartsCompare.getRelations().size(), "Should not have relations");

//        Finishes
        final IntervalTestObject finishesA = new IntervalTestObject("finishes-a", LocalDate.of(2017, 3, 11), LocalDate.of(2017, 5, 14));
        final IntervalTestObject finishesB = new IntervalTestObject("finishes-b", LocalDate.of(1990, 5, 14), LocalDate.of(2017, 5, 14));
        final TemporalComparisonReport finishesCompare = engine.compareObjects(finishesA, finishesB);
        assertAll(() -> assertEquals(1, finishesCompare.getRelations().size(), "Should have 1 relation"),
                () -> assertTrue(finishesCompare.getRelations().contains(ObjectRelation.FINISHES), "Should have Finishes"));
        final TemporalComparisonReport noFinishesCompare = engine.compareObjects(finishesB, finishesA);
        assertEquals(0, noFinishesCompare.getRelations().size(), "Should not have relations");

//        Meets
        final IntervalTestObject meetsA = new IntervalTestObject("meets-a", LocalDate.of(1989, 3, 11), LocalDate.of(2017, 3, 11));
        final IntervalTestObject meetsB = new IntervalTestObject("meets-b", LocalDate.of(2017, 3, 11), LocalDate.of(2017, 5, 14));
        final TemporalComparisonReport meetsCompare = engine.compareObjects(meetsA, meetsB);
        assertAll(() -> assertEquals(1, meetsCompare.getRelations().size(), "Should only have 1 relation"),
                () -> assertTrue(meetsCompare.getRelations().contains(ObjectRelation.TEMPORAL_MEETS), "Should have MEETS"));
        final TemporalComparisonReport noMeetsCompare = engine.compareObjects(meetsB, meetsA);
        assertAll(() -> assertEquals(1, noMeetsCompare.getRelations().size(), "Should have 1 relation"),
                () -> assertTrue(noMeetsCompare.getRelations().contains(ObjectRelation.AFTER), "Should have after relation"),
                () -> assertFalse(noMeetsCompare.getRelations().contains(ObjectRelation.TEMPORAL_MEETS), "Should not have meets"));

//        Test continuing
        final ContinuingIntervalObject continuingA = new ContinuingIntervalObject("continuing-a", LocalDate.of(2017, 3, 11).atStartOfDay());
        final TemporalComparisonReport noContinuing = engine.compareObjects(continuingA, meetsB);
        assertEquals(0, noContinuing.getRelations().size(), "Should not have relations");
        final TemporalComparisonReport startsContinuingReport = engine.compareObjects(meetsB, continuingA);
        assertAll(() -> assertEquals(1, startsContinuingReport.getRelations().size(), "Should only have 1 relation"),
                () -> assertTrue(startsContinuingReport.getRelations().contains(ObjectRelation.STARTS), "MeetsB should start ContinuingA"));
    }

    @Test
    public void intervalToPointTests() {

    }


    @DatasetClass(name = "test-interval")
    public static class IntervalTestObject implements Serializable {

        private final String id;
        private final LocalDate fromTemporal;
        private final LocalDate toTemporal;

        public IntervalTestObject(String id, LocalDate fromTemporal, LocalDate toTemporal) {
            this.id = id;
            this.fromTemporal = fromTemporal;
            this.toTemporal = toTemporal;
        }

        @IndividualIdentifier
        public String getId() {
            return id;
        }

        @StartTemporal
        public LocalDate getFromTemporal() {
            return fromTemporal;
        }

        @EndTemporal
        public LocalDate getToTemporal() {
            return toTemporal;
        }
    }

    @DatasetClass(name = "test-point")
    public static class PointTestObject implements Serializable {

        private final String id;
        private final LocalDate atTemporal;

        public PointTestObject(String id, LocalDate atTemporal) {
            this.id = id;
            this.atTemporal = atTemporal;
        }

        @IndividualIdentifier
        public String getId() {
            return id;
        }

        @DefaultTemporal(type = TemporalType.POINT, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDate getAtTemporal() {
            return atTemporal;
        }
    }

    @DatasetClass(name = "test-continuing")
    public static class ContinuingIntervalObject implements Serializable {
        private final String id;
        private final LocalDateTime fromTemporal;

        public ContinuingIntervalObject(String id, LocalDateTime fromTemporal) {
            this.id = id;
            this.fromTemporal = fromTemporal;
        }

        @IndividualIdentifier
        public String getId() {
            return id;
        }

        @StartTemporal
        public LocalDateTime getFromTemporal() {
            return fromTemporal;
        }
    }
}

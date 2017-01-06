package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.exceptions.InvalidClassException;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.types.TemporalType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.nickrobison.trestle.exceptions.InvalidClassException.State.EXCESS;
import static com.nickrobison.trestle.exceptions.InvalidClassException.State.INVALID;
import static com.nickrobison.trestle.exceptions.InvalidClassException.State.MISSING;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 7/26/16.
 */
@SuppressWarnings({"initialization"})
@Tag("unit")
public class ClassRegisterTest {

    private static EmptyTest eTest;
    private static FullTest fTest;
    private static ExtraMembers xTest;
    private static SpatialMembers sTest;
    private static PassingTemporalTest pTest;
    private static FailingTemporalTest ftTest;
    private static LanguageTest lTest;
    private static FailingLanguageTest flTest;
    private static Class<? extends SpatialMembers> sClass;
    private static Class<? extends FullTest> fClass;
    private static Class<? extends EmptyTest> eClass;
    private static Class<? extends ExtraMembers> xClass;
    private static Class<? extends PassingTemporalTest> pClass;
    private static Class<? extends FailingTemporalTest> ftClass;
    private static Class<? extends LanguageTest> lClass;
    private static Class<? extends FailingLanguageTest> flClass;
    private static final Logger logger = LoggerFactory.getLogger(ClassRegisterTest.class);

    @BeforeAll
    public static void setup() {
        fTest = new FullTest();
        eTest = new EmptyTest();
        xTest = new ExtraMembers();
        sTest = new SpatialMembers();
        pTest = new PassingTemporalTest(LocalDate.now(), LocalDate.now());
        ftTest = new FailingTemporalTest(LocalDateTime.now(), LocalDateTime.now());
        lTest = new LanguageTest();
        flTest = new FailingLanguageTest();


        fClass = fTest.getClass();
        eClass = eTest.getClass();
        xClass = xTest.getClass();
        sClass = sTest.getClass();
        pClass = pTest.getClass();
        ftClass = ftTest.getClass();
        lClass = lTest.getClass();
        flClass = flTest.getClass();
    }

    @Test
    public void testIdentifier() {
        try {
            ClassRegister.checkIndividualIdentifier(fClass);
        } catch (InvalidClassException e) {
            logger.error("Invalid exception thrown", e);
            fail("Should not throw exception");
        }

        InvalidClassException invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkIndividualIdentifier(eClass));
        assertEquals(MISSING, invalidClassException.getProblemState(), "Wrong problem state");

        invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkIndividualIdentifier(xClass));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Wrong problem state");
    }

    @Test
    public void testName() {

        try {
            ClassRegister.checkForClassName(fClass);
        } catch (InvalidClassException e) {
            fail("Should not throw exception");
        }

        final InvalidClassException invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForClassName(eClass));
        assertEquals(MISSING, invalidClassException.getProblemState(), "Wrong problem state");
    }

    @Test
    public void testConstructor() {

        try {
            ClassRegister.checkForConstructor(fClass);
        } catch (TrestleClassException e) {
            fail("Should not throw exception");
        }

        final InvalidClassException invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForConstructor(eClass));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Wrong problem state");

        try {
            ClassRegister.checkForConstructor(xClass);
        } catch (TrestleClassException e) {
            fail("Should not throw exception");
        }

    }

    @Test
    public void testSpatial() {
        try {
            ClassRegister.checkForSpatial(fClass);
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("Should not throw exception");
        }

        InvalidClassException invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForSpatial(xClass));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Should have EXCESS problem state");

        invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForSpatial(sClass));
        assertEquals(MISSING, invalidClassException.getProblemState(), "Should be missing the spatial argument in the constructor");
    }

    @Test
    public void testTemporal() {
        try {
            ClassRegister.checkForTemporals(pClass);
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("Should not throw exception");
        }

        final InvalidClassException invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForTemporals(ftClass));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Should have excess problem state");

        try {
            ClassRegister.checkForTemporals(TimeZoneParsingTest.class);
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("Should not throw exception");
        }
    }

    @Test
    public void testLanguage() {
        try {
            ClassRegister.checkForLanguage(lClass);
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("Should not throw exception");
        }

        final InvalidClassException invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForLanguage(flClass));
        assertEquals(INVALID, invalidClassException.getProblemState());
    }

    private static class PassingTemporalTest {

        private LocalDate startDate;
        private LocalDate endDate;

        PassingTemporalTest(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @StartTemporalProperty
        public LocalDate getStartDate() {
            return this.startDate;
        }

        public LocalDate getEndDate() {
            return this.endDate;
        }
    }

    private static class FailingTemporalTest {

        @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDateTime startDate;
        @StartTemporalProperty
        public LocalDateTime endDate;

        FailingTemporalTest(LocalDateTime startDate, LocalDateTime endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private static class TimeZoneParsingTest {
        @DefaultTemporalProperty(timeZone = "America/Los_Angeles", type = TemporalType.POINT, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDate defaultDate;

        TimeZoneParsingTest(LocalDate defaultDate) {
            this.defaultDate = defaultDate;
        }
    }


    @SuppressWarnings("UnusedParameters")
    private static class EmptyTest {
        public String thing = "nope";

        EmptyTest() {
        }

        EmptyTest(String test1) {
            this.thing = test1;
        }

        EmptyTest(String test1, String test2) {
            this.thing = test2;
        }
    }

    @OWLClassName(className = "ready")
    private static class FullTest {
        @IndividualIdentifier
        public String thing;
        @Spatial(name = "wktString")
        public String wkt;

        FullTest() {
            this.thing = "testString";
            this.wkt = "hellowkt";
        }

        @TrestleCreator
        FullTest(String thing, String wktString) {
            this.thing = thing;
            this.wkt = wktString;
        }
    }

    private static class ExtraMembers {

        private final String id1;
        private final String id2;

        ExtraMembers() {
            this.id1 = "hello";
            this.id2 = "hello2";
        }

        ExtraMembers(String id1, String id2) {
            this.id1 = id1;
            this.id2 = id2;
        }

        @IndividualIdentifier
        @Spatial
        public String getID1() {
            return this.id1;
        }

        @IndividualIdentifier
        @Spatial
        public String getID2() {
            return this.id2;
        }


    }

    private static class SpatialMembers {
        private final String wkt;

        SpatialMembers() {
            this.wkt = "testWKT";
        }

        SpatialMembers(String wkt) {
            this.wkt = wkt;
        }

        @Spatial(name = "wrongWKT")
        public String getWKT() {
            return this.wkt;
        }
    }

    private static class LanguageTest {
        @DataProperty(name = "testString")
        @Language(language = "fr")
        public final String testString;
        private final String testString2;

        LanguageTest() {
            this.testString = "test string";
            this.testString2 = "test string";
        }

        @DataProperty(name = "testString")
        @Language(language = "en")
        public String getTestString2() {
            return this.testString2;
        }
    }

    private static class FailingLanguageTest {
        @DataProperty(name = "testString")
        @Language(language = "fr")
        public final String testString;
        private final String testString2;

        FailingLanguageTest() {
            this.testString = "test string";
            this.testString2 = "test string";
        }

        @DataProperty(name = "testString")
        @Language(language = "en-Nick")
        public String getTestString2() {
            return this.testString2;
        }
    }
}

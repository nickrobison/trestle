package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.InvalidClassException;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.parser.clojure.ClojureProvider;
import com.nickrobison.trestle.types.TemporalType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.nickrobison.trestle.reasoner.exceptions.InvalidClassException.State.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 7/26/16.
 */
@SuppressWarnings({"initialization", "WeakerAccess"})
public class ClassRegisterTest {

    private static EmptyTest eTest;
    private static FullTest fTest;
    private static ExtraID xTest;
    private static SpatialMembers sTest;
    private static PassingTemporalTest pTest;
    private static FailingTemporalTest ftTest;
    private static LanguageTest lTest;
    private static FailingLanguageTest flTest;
    private static Class<? extends SpatialMembers> sClass;
    private static Class<? extends FullTest> fClass;
    private static Class<? extends EmptyTest> eClass;
    private static Class<? extends ExtraID> xClass;
    private static Class<? extends PassingTemporalTest> pClass;
    private static Class<? extends FailingTemporalTest> ftClass;
    private static Class<? extends LanguageTest> lClass;
    private static IClassRegister cr;
    private static IClassParser cp;
    private static ITypeConverter typeConverter;

    @BeforeAll
    public static void setup() {
        fTest = new FullTest();
        eTest = new EmptyTest();
        xTest = new ExtraID();
        sTest = new SpatialMembers();
        pTest = new PassingTemporalTest(LocalDate.now(), LocalDate.now());
        ftTest = new FailingTemporalTest(LocalDateTime.now(), LocalDateTime.now());
        lTest = new LanguageTest();


        fClass = fTest.getClass();
        eClass = eTest.getClass();
        xClass = xTest.getClass();
        sClass = sTest.getClass();
        pClass = pTest.getClass();
        ftClass = ftTest.getClass();
        lClass = lTest.getClass();
        typeConverter = new TypeConverter();
        final Object o = ClojureProvider.buildClojureParser("http://nickrobison.com/test/", true, "en-US", 4326, typeConverter);
        cr = (IClassRegister) o;
        cp = (IClassParser) o;
    }

    @Test
    public void testAccess() {
//        Private class
//        final InvalidClassException invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(PrivateClassTest.class));
//        assertAll(() -> assertEquals(INVALID, invalidClassException.getProblemState(), "Should have problem with class"),
//                () -> assertEquals("Class", invalidClassException.getMember(), "Should have a problem with the class"));

//        Public class, private constructor
        final InvalidClassException invalidConstructor = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(PrivateConstructorTest.class), PrivateConstructorTest.class));
        assertAll(() -> assertEquals(INVALID, invalidConstructor.getProblemState(), "Should have problem with constructor"),
                () -> assertEquals("Constructor", invalidConstructor.getMember(), "Should have a problem with the constructor"));
    }

    @Test
    public void testIdentifier() {

        InvalidClassException invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(eClass), eClass));
        assertEquals(MISSING, invalidClassException.getProblemState(), "Wrong problem state");

        invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(xClass), xClass));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Wrong problem state");
    }

    @Test
    public void testName() {

        final InvalidClassException invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(eClass), eClass));
        assertEquals(MISSING, invalidClassException.getProblemState(), "Wrong problem state");
    }

    @Test
    public void testConstructor() {


//        Check for too many constructors
        final InvalidClassException invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(ExcessConstructorTest.class), ExcessConstructorTest.class));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Wrong problem state");

    }

    @Test
    public void testSpatial() {

        InvalidClassException invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(ExtraSpatial.class), ExtraSpatial.class));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Should have EXCESS problem state");

        invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(sClass), sClass));
        assertEquals(MISSING, invalidClassException.getProblemState(), "Should be missing the spatial argument in the constructor");

//        Test for invalid Projection
        invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(FailingSpatialTest.class), FailingSpatialTest.class));
        assertEquals(INVALID, invalidClassException.getProblemState(), "Should have an invalid projection");
    }

    @Test
    public void testTemporal() {

        final InvalidClassException invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(ftClass), ftClass));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Should have excess problem state");

//        Check for multiple point temporals
        final InvalidClassException multiPointException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(MultiplePointTemporal.class), MultiplePointTemporal.class));
        assertEquals(EXCESS, multiPointException.getProblemState(), "Should have excess point temporals");

//        No temporals
        final InvalidClassException noTemporalException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(NoTemporal.class), NoTemporal.class));
        assertEquals(MISSING, noTemporalException.getProblemState());

        try {
            cr.registerClass(cp.getObjectClass(TimeZoneParsingTest.class), TimeZoneParsingTest.class);
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail("Should not throw exception");
        }
    }

    @Test
    public void testLanguage() {

        final InvalidClassException invalidClassException = assertThrows(InvalidClassException.class, () -> cr.registerClass(cp.getObjectClass(FailingTemporalTest.class), FailingLanguageTest.class));
        assertEquals(INVALID, invalidClassException.getProblemState());
    }

    private static class PassingTemporalTest {

        private LocalDate startDate;
        private LocalDate endDate;

        PassingTemporalTest(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @StartTemporal
        public LocalDate getStartDate() {
            return this.startDate;
        }

        public LocalDate getEndDate() {
            return this.endDate;
        }
    }


    private static class PrivateClassTest {

        PrivateClassTest() {
//            Not used
        }
    }

    @DatasetClass(name = "test")
    public static class PrivateConstructorTest {

        private final String test;

        private PrivateConstructorTest(String test) {
            this.test = test;
        }

    }

    @DatasetClass(name = "test")
    public static class FailingTemporalTest {

        @DefaultTemporal(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDateTime startDate;
        @StartTemporal
        public LocalDateTime endDate;
        @IndividualIdentifier
        public String id;

        public FailingTemporalTest(LocalDateTime startDate, LocalDateTime endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    @DatasetClass(name = "test")
    public static class TimeZoneParsingTest {
        @DefaultTemporal(timeZone = "America/Los_Angeles", type = TemporalType.POINT, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDate defaultDate;
        @IndividualIdentifier
        public String id;

        public TimeZoneParsingTest(LocalDate defaultDate) {
            this.defaultDate = defaultDate;
        }
    }

    @DatasetClass(name = "test")
    public static class MultiplePointTemporal {
        @DefaultTemporal(timeZone = "America/Los_Angeles", type = TemporalType.POINT, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDate defaultDate;
        @DefaultTemporal(timeZone = "America/Los_Angeles", type = TemporalType.POINT, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDateTime excessDate;
        @IndividualIdentifier
        public String id;

        public MultiplePointTemporal(LocalDate defaultDate) {
            this.defaultDate = defaultDate;
        }
    }

    @DatasetClass(name = "test")
    public static class NoTemporal {
        @IndividualIdentifier
        public String id;

        public NoTemporal(String id) {
            this.id = id;
        }
    }


    @SuppressWarnings("UnusedParameters")
    @DatasetClass(name = "test")
    public static class EmptyTest {
        public String thing = "nope";

        EmptyTest() {
//            Not used
        }

        @TrestleCreator
        public EmptyTest(String test1) {
            this.thing = test1;
        }

        EmptyTest(String test1, String test2) {
            this.thing = test2;
        }
    }

    @DatasetClass(name = "test")
    public static class ExcessConstructorTest {
        public String thing = "nope";

        ExcessConstructorTest() {
//            Not used
        }

        public ExcessConstructorTest(String test1) {
            this.thing = test1;
        }

        public ExcessConstructorTest(String test1, String test2) {
            this.thing = test2;
        }
    }


    @DatasetClass(name = "ready")
    public static class FullTest {
        @IndividualIdentifier
        public String thing;
        @Spatial(name = "wktString")
        public String wkt;

        FullTest() {
            this.thing = "testString";
            this.wkt = "hellowkt";
        }

        @TrestleCreator
        public FullTest(String thing, String wktString) {
            this.thing = thing;
            this.wkt = wktString;
        }
    }

    @DatasetClass(name = "test")
    public static class ExtraID {

        private final String id1;
        private final String id2;

        ExtraID() {
            this.id1 = "hello";
            this.id2 = "hello2";
        }

        public ExtraID(String id1, String id2) {
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

    @DatasetClass(name = "test")
    public static class ExtraSpatial {

        private final String id1;
        private final String id2;
        @IndividualIdentifier
        public final String id = "testID";

        ExtraSpatial() {
            this.id1 = "hello";
            this.id2 = "hello2";
        }

        public ExtraSpatial(String id1, String id2) {
            this.id1 = id1;
            this.id2 = id2;
        }

        @Spatial
        public String getID1() {
            return this.id1;
        }

        @Spatial
        public String getID2() {
            return this.id2;
        }
    }

    @DatasetClass(name = "test")
    public static class SpatialMembers {
        private final String wkt;

        SpatialMembers() {
            this.wkt = "testWKT";
        }

        public SpatialMembers(String wkt) {
            this.wkt = wkt;
        }

        @Spatial(name = "wrongWKT")
        public String getWKT() {
            return this.wkt;
        }
    }

    private static class LanguageTest {
        @Fact(name = "testString")
        @Language(language = "fr")
        public final String testString;
        private final String testString2;

        LanguageTest() {
            this.testString = "test string";
            this.testString2 = "test string";
        }

        @Fact(name = "testString")
        @Language(language = "en")
        public String getTestString2() {
            return this.testString2;
        }
    }

    @DatasetClass(name = "test")
    public static class FailingLanguageTest {
        @Fact(name = "testString")
        @Language(language = "fr")
        public final String testString;
        private final String testString2;
        @IndividualIdentifier
        public String id;
        @StartTemporal
        public LocalDate startTemporal;

        public FailingLanguageTest(String testString, String testString2, LocalDate startTemporal) {
            this.testString = testString;
            this.testString2 = testString2;
            this.startTemporal = startTemporal;
        }

        @Fact(name = "testString")
        @Language(language = "en-Nick")
        public String getTestString2() {
            return this.testString2;
        }
    }

    @DatasetClass(name = "test")
    public static class FailingSpatialTest {
        @IndividualIdentifier
        public String id;
        @Spatial(projection = -1)
        public String wkt;
        @StartTemporal
        public LocalDate startTemporal;

        public FailingSpatialTest(String id, String wkt, LocalDate startTemporal) {
            this.id = id;
            this.wkt = wkt;
            this.startTemporal = startTemporal;
        }
    }
}

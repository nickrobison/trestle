package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.exceptions.InvalidClassException;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nickrobison.trestle.exceptions.InvalidClassException.State.EXCESS;
import static com.nickrobison.trestle.exceptions.InvalidClassException.State.MISSING;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 7/26/16.
 */
@SuppressWarnings({"initialization"})
public class ClassRegisterTest {

    private static EmptyTest eTest;
    private static FullTest fTest;
    private static ExtraMembers xTest;
    private static SpatialMembers sTest;
    private static Class<? extends FullTest> fClass;
    private static Class<? extends EmptyTest> eClass;
    private static Class<? extends ExtraMembers> xClass;
    private static Class<? extends SpatialMembers> sClass;
    private static final Logger logger = LoggerFactory.getLogger(ClassRegisterTest.class);

    @BeforeAll
    public static void setup() {
        fTest = new FullTest();
        eTest = new EmptyTest();
        xTest = new ExtraMembers();
        sTest = new SpatialMembers();

        fClass = fTest.getClass();
        eClass = eTest.getClass();
        xClass = xTest.getClass();
        sClass = sTest.getClass();
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
            fail("Should not throw exception");
        }

        InvalidClassException invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForSpatial(xClass));
        assertEquals(EXCESS, invalidClassException.getProblemState(), "Should have EXCESS problem state");

        invalidClassException = expectThrows(InvalidClassException.class, () -> ClassRegister.checkForSpatial(sClass));
        assertEquals(MISSING, invalidClassException.getProblemState(), "Should be missing the spatial argument in the constructor");
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
}

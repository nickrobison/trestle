package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.OWLClassName;
import com.nickrobison.trestle.exceptions.InvalidClassException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nickrobison.trestle.exceptions.TrestleClassException.State.EXCESS;
import static com.nickrobison.trestle.exceptions.TrestleClassException.State.MISSING;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 7/26/16.
 */
@SuppressWarnings({"initialization"})
public class ClassRegisterTest {

    private static EmptyTest eTest;
    private static FullTest fTest;
    private static ExtraMembers xTest;
    private static Class<? extends FullTest> fClass;
    private static Class<? extends EmptyTest> eClass;
    private static Class<? extends ExtraMembers> xClass;
    private static final Logger logger = LoggerFactory.getLogger(ClassRegisterTest.class);

    @BeforeAll
    public static void setup() {
        fTest = new FullTest();
        eTest = new EmptyTest();
        xTest = new ExtraMembers();

        fClass = fTest.getClass();
        eClass = eTest.getClass();
        xClass = xTest.getClass();
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


    private static class EmptyTest {
        public String thing = "nope";

        EmptyTest() {
        }
    }

    @OWLClassName(className = "ready")
    private static class FullTest {
        @IndividualIdentifier
        public String thing = "hello";

        FullTest() {
        }
    }

    private static class ExtraMembers {

        ExtraMembers() {
        }

        @IndividualIdentifier
        public String getID1() {
            return "hello";
        }

        @IndividualIdentifier
        public String getID2() {
            return "hello2";
        }

    }
}

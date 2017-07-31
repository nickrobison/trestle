package com.nickrobison.trestle.types.temporal;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.TrestleCreator;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by detwiler on 5/5/17.
 */
@Tag("integration")
public class ConstructorTemporalDependencyTest extends AbstractReasonerTest {

    @Override
    protected String getTestName() {
        return "constructor_test";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestObject.class);
    }

    @Test
    public void testNonTemporalConstructor() {


        LocalDate startDate = LocalDate.of(2017, 1, 1);
        String id = "TEST0001";
        TestObject inObj = new TestObject(startDate, id);

        try {
            reasoner.writeTrestleObject(inObj);
            TestObject outObject = reasoner.readTrestleObject(TestObject.class, id, startDate, null);
            if (!outObject.equals(inObj))
                fail("Input and output objects are not equivalent");
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (MissingOntologyEntity e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @DatasetClass(name = "objectconstructor-test")
    public static class TestObject {
        @IndividualIdentifier
        public String id;

        @StartTemporal
        public LocalDate startTime;

        public TestObject(LocalDate startTime, String id) {
            this.startTime = startTime;
            this.id = id;
        }

        @TrestleCreator
        public TestObject(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestObject other = (TestObject) o;

            if (!id.equals(other.id)) return false;
            return true;

        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + startTime.hashCode();
            return result;
        }
    }
}

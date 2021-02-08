package com.nickrobison.trestle.types.temporal;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.AbstractReasonerTest;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.TrestleCreator;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by detwiler on 5/5/17.
 */
@Tag("integration")
// Suppress this because we're actually dealing with nulls, and I don't really care that much
@SuppressWarnings({"assignment.type.incompatible", "argument.type.incompatible", "initialization.fields.uninitialized"})
public class NullEndTemporalTest extends AbstractReasonerTest {

    @Override
    protected String getTestName() {
        return "null_enddate_test";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(TestObject.class);
    }

    @Test
    public void nullEndDateTest() {


        LocalDate startDate = LocalDate.of(2016, 1, 1);
        String id = "TEST0001";
        TestObject inObj1 = new TestObject(startDate, null, id, 1);
        TestObject inObj2 = new TestObject(startDate.plusYears(1), null, id, 2);

        try {
            reasoner.writeTrestleObject(inObj1);
            reasoner.writeTrestleObject(inObj2);
            LocalDate retrieveDate1 = startDate.plusMonths(1);
            TestObject outObject1 = reasoner.readTrestleObject(TestObject.class, id, retrieveDate1, null).blockingGet();
            // output object should equal inObj1
            if (!outObject1.equals(inObj1))
                fail("Output does not equal first input object");

            // output object should equal inObj2
            LocalDate retrieveDate2 = startDate.plusMonths(13);
            TestObject outObject2 = reasoner.readTrestleObject(TestObject.class, id, retrieveDate2, null).blockingGet();
            if (!outObject2.equals(inObj2))
                fail("Output does not equal second input object");
        } catch (TrestleClassException e) {
            e.printStackTrace();
            fail(e);
        } catch (MissingOntologyEntity e) {
            e.printStackTrace();
            fail(e);
        }
    }

    @DatasetClass(name = "objectconstructor-test")
    public static class TestObject {
        @IndividualIdentifier
        public String id;

        public Integer data;

        @StartTemporal
        public LocalDate startTime;

        @EndTemporal
        public LocalDate endTime;

        public TestObject(LocalDate startTime, LocalDate endTime, String id, int data) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.id = id;
            this.data = data;
        }

        @TrestleCreator
        public TestObject(String id, Integer data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestObject other = (TestObject) o;

            if (!id.equals(other.id)) return false;
            return data.equals(other.data);

        }

        @Override
        public int hashCode() {
            int idcode = id.hashCode();
            int datacode = data.hashCode();
            int thiscode = 31 * idcode + 71 * datacode + startTime.hashCode();
            return thiscode;
        }
    }
}

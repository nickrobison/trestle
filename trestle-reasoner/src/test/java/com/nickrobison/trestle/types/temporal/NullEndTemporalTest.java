package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.annotations.DatasetClass;
import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by detwiler on 5/5/17.
 */
@Tag("integration")
public class NullEndTemporalTest {
    final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf"));
    private String connectStr = config.getString("trestle.ontology.connectionString");
    private String username = config.getString("trestle.ontology.username");
    private String password = config.getString("trestle.ontology.password");
    private String reponame = "null_enddate_test";
    private String ontLocation = config.getString("trestle.ontology.location");
    private TrestleReasoner reasoner;

    @BeforeEach
    public void setup() {
        reasoner = new TrestleBuilder()
                .withDBConnection(connectStr, username, password)
                .withName(reponame)
                .withOntology(IRI.create(ontLocation))
                .withPrefix("http://nickrobison.com/demonstration/test#")
                .withInputClasses(TestObject.class)
                .withoutCaching()
                .withoutMetrics()
                .initialize()
                .build();
    }

    @AfterEach
    public void shutdown() {
        reasoner.shutdown(true);
    }

    @Test
    public void nullEndDateTest() {


        LocalDate startDate = LocalDate.of(2016,1,1);
        String id = "TEST0001";
        TestObject inObj1 = new TestObject(startDate,null, id, 1);
        TestObject inObj2 = new TestObject(startDate.plusYears(1),null, id, 2);

        try {
            reasoner.writeTrestleObject(inObj1);
            reasoner.writeTrestleObject(inObj2);
            LocalDate retrieveDate1 = startDate.plusMonths(1);
            TestObject outObject1 = reasoner.readTrestleObject(TestObject.class, id, retrieveDate1, null);
            // output object should equal inObj1
            if(!outObject1.equals(inObj1))
                fail("Output does not equal first input object");

            // output object should equal inObj2
            LocalDate retrieveDate2 = startDate.plusMonths(13);
            TestObject outObject2 = reasoner.readTrestleObject(TestObject.class, id, retrieveDate2, null);
            if(!outObject2.equals(inObj2))
                fail("Output does not equal second input object");
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
        public boolean equals(Object o) {
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

package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.annotations.DatasetClass;
import com.nickrobison.trestle.annotations.IndividualIdentifier;
import com.nickrobison.trestle.annotations.TrestleCreator;
import com.nickrobison.trestle.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by detwiler on 5/5/17.
 */
public class ConstructorTemporalDependencyTest {
    private Config config = ConfigFactory.load("application.conf");
    private String connectStr = config.getString("trestle.graphdb.connection_string");
    private String username = config.getString("trestle.graphdb.username");
    private String password = config.getString("trestle.graphdb.password");
    private String reponame = "constructor_test";
    private String ontLocation = config.getString("trestle.ontology.location");

    @Test
    public void TestNonTemporalConstructor() {
        TrestleReasoner reasoner = new TrestleBuilder()
                .withDBConnection(connectStr, username, password)
                .withName(reponame)
                .withOntology(IRI.create(ontLocation))
                .withPrefix("http://nickrobison.com/demonstration/test#")
                .withInputClasses(TestObject.class)
                .withoutCaching()
                .initialize()
                .build();

        LocalDate startDate = LocalDate.of(2017,1,1);
        String id = "TEST0001";
        TestObject inObj = new TestObject(startDate,id);

        try {
            reasoner.writeTrestleObject(inObj);
            TestObject outObject = reasoner.readTrestleObject(TestObject.class, id, startDate, null);
            if(!outObject.equals(inObj))
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

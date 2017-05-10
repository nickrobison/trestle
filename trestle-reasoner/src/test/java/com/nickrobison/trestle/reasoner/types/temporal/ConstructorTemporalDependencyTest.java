package com.nickrobison.trestle.reasoner.types.temporal;

import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.TrestleCreator;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
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
public class ConstructorTemporalDependencyTest {
    final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf"));
    private String connectStr = config.getString("trestle.ontology.connectionString");
    private String username = config.getString("trestle.ontology.username");
    private String password = config.getString("trestle.ontology.password");
    private String reponame = "constructor_test";
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
    public void testNonTemporalConstructor() {


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

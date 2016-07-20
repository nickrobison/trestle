package com.nickrobison.trestle.common;

import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization"})
public class TrestleParserTest {

    private GAULTestClass gaulTestClass;
    private MoreGAULTests moreGAULTests;
    private OWLDataFactory df;
    private TemporalObject temporal;
    private TemporalObject temporalPoint;
    private GAULMethodTest testMethod;

    @BeforeEach
    public void Setup() {
        gaulTestClass = new GAULTestClass(1234, "gaulTestClass");
        moreGAULTests = new MoreGAULTests();
        testMethod = new GAULMethodTest();
        df = OWLManager.getOWLDataFactory();
        LocalDateTime dt = LocalDateTime.of(1989, 3, 26, 0, 0);
        temporal = TemporalObjectBuilder.valid().from(dt).to(dt.plusYears(1)).withRelations();
        temporalPoint = TemporalObjectBuilder.exists().at(dt).withRelations();
    }

    @Test
    public void TestGAULParser() {

//        Test the class
        final OWLClass owlClass = ClassParser.GetObjectClass(gaulTestClass);
        final OWLClass gaul_test1 = df.getOWLClass(IRI.create("trestle:", "GAUL_Test"));
        assertEquals(gaul_test1, owlClass, "Wrong OWL Class");
//        Test the named individual
        OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(gaulTestClass);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "test_me"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Test the data property parser
//        Code
        final OWLDataProperty adm0_code = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));
        final OWLLiteral adm0_code_literal = df.getOWLLiteral("1234", OWL2Datatype.XSD_INTEGER);
        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(gaulTestClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(3, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");
        final OWLDataPropertyAssertionAxiom parsed_code = owlDataPropertyAssertionAxioms.get().get(0);
        assertEquals(gaul_test, parsed_code.getSubject(), "Wrong named individual");
        assertEquals(adm0_code, parsed_code.getProperty(), "Data property IRIs don't match");
        assertEquals(adm0_code_literal, parsed_code.getObject(), "Data property values are wrong");

//        Name
        final OWLDataProperty adm0_name = df.getOWLDataProperty(IRI.create("trestle:", "adm0_name"));
        final OWLLiteral adm0_name_literal = df.getOWLLiteral("gaulTestClass");
        final OWLDataPropertyAssertionAxiom parsed_name = owlDataPropertyAssertionAxioms.get().get(1);
        assertEquals(gaul_test, parsed_name.getSubject(), "Wrong named individual");
        assertEquals(adm0_name, parsed_name.getProperty(), "Data property IRIs don't match");
        assertEquals(adm0_name_literal, parsed_name.getObject(), "Data property values are wrong");

//        Test the new gaul test
        //        Test the named individual
        owlNamedIndividual = ClassParser.GetIndividual(moreGAULTests);
        gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "test region"));
        assertEquals(owlNamedIndividual, gaul_test, "Wrong named individual");

        owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(moreGAULTests);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(4, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");

//        Test the temporal
        Optional<List<TemporalObject>> temporalObjects = ClassParser.GetTemporalObjects(moreGAULTests);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(4, temporalObjects.get().size(), "Wrong number of temporal objects");
//        Check for the same type and scope for interval
        assertEquals(temporal.getType(), temporalObjects.get().get(0).getType(), "Wrong temporal type");
        assertEquals(temporal.getScope(), temporalObjects.get().get(0).getScope(), "Wrong temporal scope");
        assertEquals(1, temporalObjects.get().get(0).getTemporalRelations().size(), "Wrong # of temporal relations");
        assertEquals(gaul_test, temporalObjects.get().get(0).getTemporalRelations().stream().findFirst().get(), "Wrong temporal relation");
//        Check for the correct values on the built temporal objects
        //        Interval
        assertTrue(temporalObjects.get().get(2).isInterval(), "Should have build an interval object");
        assertEquals(LocalDateTime.of(1989, 3, 26, 0, 0), temporalObjects.get().get(2).asInterval().getFromTime());
        assertEquals(LocalDateTime.of(1989, 3, 26, 0, 0).plusYears(5), temporalObjects.get().get(2).asInterval().getToTime().get());
//        Point
        assertTrue(temporalObjects.get().get(3).isPoint(), "Should have built a point object");
        assertEquals(LocalDateTime.of(1989, 3, 26, 0, 0), temporalObjects.get().get(3).asPoint().getPointTime(), "Wrong point time");

//        Check geo point
        assertEquals(temporalPoint.getType(), temporalObjects.get().get(1).getType(), "Wrong temporal type");
        assertEquals(temporalPoint.getScope(), temporalObjects.get().get(1).getScope(), "Wrong temporal scope");
        assertEquals(1, temporalObjects.get().get(1).getTemporalRelations().size(), "Wrong # of temporal relations");
        assertEquals(gaul_test, temporalObjects.get().get(1).getTemporalRelations().stream().findFirst().get(), "Wrong temporal relation");

//        Check methods
//        Individual
        owlNamedIndividual = ClassParser.GetIndividual(testMethod);
        gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "string_from_method"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Data properties

//        Temporal
        temporalObjects = ClassParser.GetTemporalObjects(testMethod);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(1, temporalObjects.get().size(), "Wrong number of objects");
        assertEquals(LocalDateTime.of(1989, 3, 26, 0, 0), temporalObjects.get().stream().findFirst().get().asInterval().getFromTime(), "Temporal is incorrect");

    }

    @OWLClassName(className = "GAUL_Test")
    protected class MoreGAULTests {

        public int adm0_code;
        @IndividualIdentifier
        public String adm0_name;
        @Spatial
        public String test_name;
        @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        @Ignore
        public LocalDateTime testtime;
        private String privateField;
        @DefaultTemporalProperty(type = TemporalType.POINT, scope= TemporalScope.EXISTS, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDateTime testpoint;
        @StartTemporalProperty(type = TemporalType.INTERVAL)
        public LocalDateTime teststart;
        @EndTemporalProperty()
        public LocalDateTime testend;
        @StartTemporalProperty(type = TemporalType.POINT)
        public LocalDateTime testat;
        public MoreGAULTests() {
            this.adm0_code = 4326;
            this.test_name = "new_test";
            this.adm0_name = "test region";
            this.testtime = LocalDateTime.of(1998, 3, 26, 0, 0);
            this.privateField = "don't read me";
            this.testpoint = LocalDateTime.of(1989, 3, 26, 0, 0);
            this.teststart = LocalDateTime.of(1989, 3, 26, 0, 0);
            this.testend = LocalDateTime.of(1989, 3, 26, 0, 0).plusYears(5);
            this.testat = LocalDateTime.of(1989, 3, 26, 0, 0);
        }
    }

    @OWLClassName(className = "GAUL_Test")
    protected class GAULMethodTest {

        public int adm0_code;
        public String adm0_name;
        @Spatial
        public String test_name;

        @Ignore
        public LocalDateTime testtime;
        private String privateField;
//        @DefaultTemporalProperty(type = TemporalType.POINT, scope= TemporalScope.EXISTS, duration = 0, unit = ChronoUnit.YEARS)
        private LocalDateTime testpoint;

        public GAULMethodTest() {
            this.adm0_code = 4326;
            this.test_name = "new_test";
            this.adm0_name = "test region";
            this.testtime = LocalDateTime.of(1998, 3, 26, 0, 0);
            this.privateField = "don't read me";
            this.testpoint = LocalDateTime.of(1989, 3, 26, 0, 0);
        }

        @IndividualIdentifier
        public String getName() {
            return "string_from_method";
        }

        public int getAdm0_code() {
            return this.adm0_code;
        }

        public String getadm0_name() {
            return this.adm0_name;
        }

        @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDateTime getTime() {
            return this.testpoint;
        }
    }




}

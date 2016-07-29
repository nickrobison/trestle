package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.temporal.DefaultTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
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
import java.util.ArrayList;
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
    private ExpandedGAULTests expandedGAULClass;
    private OWLDataFactory df;
    private TemporalObject temporal;
    private TemporalObject temporalPoint;
    private GAULMethodTest testMethod;

    @BeforeEach
    public void Setup() {
        gaulTestClass = new GAULTestClass(1234, "gaulTestClass");
        expandedGAULClass = new ExpandedGAULTests();
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
        owlNamedIndividual = ClassParser.GetIndividual(expandedGAULClass);
        gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "test_region"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

        owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(expandedGAULClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(3, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");

//        Test the temporal
        Optional<List<TemporalObject>> temporalObjects = TemporalParser.GetTemporalObjects(expandedGAULClass);
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
        owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(testMethod);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have method properties");
        assertEquals(5, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of data properties");
        assertEquals(OWL2Datatype.XSD_INTEGER, owlDataPropertyAssertionAxioms.get().get(5).getObject().getDatatype().getBuiltInDatatype(), "Should have integer datatype");
        assertEquals(testMethod.getAdm0_code(), owlDataPropertyAssertionAxioms.get().get(5).getObject().parseInteger(), "Invalid ADM0_Code");
        assertEquals(testMethod.test_name, owlDataPropertyAssertionAxioms.get().get(2).getObject().getLiteral(), "Invalid Spatial");

//        Temporal
        temporalObjects = TemporalParser.GetTemporalObjects(testMethod);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(2, temporalObjects.get().size(), "Wrong number of objects");
        assertEquals(LocalDateTime.of(1989, 3, 26, 0, 0), temporalObjects.get().get(0).asInterval().getFromTime(), "Wrong interval start");
        assertEquals(LocalDateTime.of(1989, 3, 26, 0, 0).plusYears(5), temporalObjects.get().get(0).asInterval().getToTime().get(), "Wrong interval end");
        assertEquals(LocalDateTime.of(1998, 3, 26, 0, 0), temporalObjects.get().get(1).asInterval().getFromTime(), "Temporal is incorrect");


    }

    @Test
    public void testConstructor() {
        List<OWLDataPropertyAssertionAxiom> testProperties = new ArrayList<>();
        List<TemporalObject> testTemporals = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = df.getOWLNamedIndividual(IRI.create("trestle:", "string_from_method"));
//        Build the data objects
        final OWLDataPropertyAssertionAxiom admcode = df.getOWLDataPropertyAssertionAxiom(
                df.getOWLDataProperty(IRI.create("trestle", "adm0_code")),
                owlNamedIndividual,
                df.getOWLLiteral(4326));
        testProperties.add(admcode);

        final OWLDataPropertyAssertionAxiom adm0Name = df.getOWLDataPropertyAssertionAxiom(
                df.getOWLDataProperty(IRI.create("trestle:", "getAdm0_name")),
                owlNamedIndividual,
                df.getOWLLiteral("test region"));
        testProperties.add(adm0Name);

        final OWLDataPropertyAssertionAxiom testName = df.getOWLDataPropertyAssertionAxiom(
                df.getOWLDataProperty(IRI.create("trestle:", "test_name")),
                owlNamedIndividual,
                df.getOWLLiteral("new_test"));
        testProperties.add(testName);

        final IntervalTemporal defaultTemporal = TemporalObjectBuilder
                .valid()
                .from(LocalDateTime.of(1998, 3, 26, 0, 0))
                .to(LocalDateTime.of(1998, 3, 26, 0, 0).plusYears(1))
                .isDefault()
                .withRelations(owlNamedIndividual);
        testTemporals.add(defaultTemporal);


        final IntervalTemporal intervalTemporal = TemporalObjectBuilder
                .valid()
                .from(LocalDateTime.of(1989, 3, 26, 0, 0))
                .to(LocalDateTime.of(1989, 3, 26, 0, 0).plusYears(5))
                .withRelations(owlNamedIndividual);
        testTemporals.add(intervalTemporal);

//        Build the inputs and test the constructor
        List<Class<?>> inputClasses = new ArrayList<>();
        List<Object> inputObjects = new ArrayList<>();
        final Optional<List<OWLDataProperty>> propertyMembers = ClassBuilder.getPropertyMembers(GAULMethodTest.class);
//        if (propertyMembers.isPresent()) {
//            propertyMembers.get().forEach(property -> {
////                inputClasses.add(property.get)
//            });
//        }

//        Properties
        testProperties.forEach(property -> {
            final Class<?> javaClass = ClassBuilder.lookupJavaClassFromOWLDatatype(property.getObject().getDatatype().getBuiltInDatatype());
            inputClasses.add(javaClass);
            final Object literalValue = ClassBuilder.extractOWLLiteral(javaClass, property.getObject());
//            final Object literalValue = javaClass.cast(property.getObject().getLiteral());
            inputObjects.add(literalValue);
        });

//        Temporals
        testTemporals.forEach(temporal -> {
            if (temporal.isPoint()) {
                inputClasses.add(LocalDateTime.class);
                inputObjects.add(temporal.asPoint().getPointTime());
            } else {
//                Add the from time
                inputClasses.add(LocalDateTime.class);
                inputObjects.add(temporal.asInterval().getFromTime());
                if (!temporal.asInterval().isDefault()) {
                    final Optional<LocalDateTime> toTime = temporal.asInterval().getToTime();
                    if (toTime.isPresent()) {
                        inputClasses.add(LocalDateTime.class);
                        inputObjects.add(toTime.get());
                    }
                }
            }
        });

        final GAULMethodTest expectedClass = new GAULMethodTest();
        final GAULMethodTest gaulMethodTest = ClassBuilder.ConstructObject(GAULMethodTest.class, inputClasses, inputObjects);
        assertEquals(expectedClass, gaulMethodTest, "Should match");


    }

    @OWLClassName(className = "GAUL_Test")
    protected class ExpandedGAULTests {

        public int adm0_code;
        @IndividualIdentifier
        public String adm0_name;
        @Spatial
        public String test_name;
        @DefaultTemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        @Ignore
        public LocalDateTime testtime;
        private String privateField;
        @DefaultTemporalProperty(type = TemporalType.POINT, scope = TemporalScope.EXISTS, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDateTime testpoint;
        @StartTemporalProperty(type = TemporalType.INTERVAL)
        public LocalDateTime teststart;
        @EndTemporalProperty()
        public LocalDateTime testend;
        @StartTemporalProperty(type = TemporalType.POINT)
        public LocalDateTime testat;

        public ExpandedGAULTests() {
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


}

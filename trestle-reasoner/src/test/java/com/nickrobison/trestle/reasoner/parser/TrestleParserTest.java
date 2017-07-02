package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.annotations.DatasetClass;
import com.nickrobison.trestle.reasoner.annotations.Ignore;
import com.nickrobison.trestle.reasoner.annotations.IndividualIdentifier;
import com.nickrobison.trestle.reasoner.annotations.Spatial;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.MissingConstructorException;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization", "unchecked"})
public class TrestleParserTest {

    private TestClasses.GAULTestClass gaulTestClass;
    private ExpandedGAULTests expandedGAULClass;
    private OWLDataFactory df;
    private TemporalObject temporal;
    private TemporalObject temporalPoint;
    private TestClasses.GAULMethodTest testMethod;
    private TestClasses.GAULComplexClassTest complexObjectClass;
    private TrestleParser tp;

    @BeforeEach
    public void Setup() {
        gaulTestClass = new TestClasses.GAULTestClass(1234, "gaulTestClass");
        expandedGAULClass = new ExpandedGAULTests();
        testMethod = new TestClasses.GAULMethodTest();
        complexObjectClass = new TestClasses.GAULComplexClassTest();
        df = OWLManager.getOWLDataFactory();
        LocalDateTime dt = LocalDateTime.of(1989, 3, 26, 0, 0);
        LocalDate ld = LocalDate.of(1989, 3, 26);
        temporal = TemporalObjectBuilder.exists().from(dt).to(dt.plusYears(1)).build(); //.withRelations();
        temporalPoint = TemporalObjectBuilder.exists().at(ld).build(); // .withRelations();
        tp = new TrestleParser(df, TRESTLE_PREFIX, true, "");
    }

    @Test
    public void TestSimpleGAULClass() {
//        Test the class
        final OWLClass owlClass = tp.classParser.getObjectClass(gaulTestClass);
        final OWLClass gaul_test1 = df.getOWLClass(IRI.create(TRESTLE_PREFIX, "GAUL_Test"));
        assertEquals(gaul_test1, owlClass, "Wrong OWL Class");
//        Test the named individual
        OWLNamedIndividual owlNamedIndividual = tp.classParser.getIndividual(gaulTestClass);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, "test_me"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Test the data property parser
//        Code
        final OWLDataProperty adm0_code = df.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "ADM0_Code"));
        final OWLLiteral adm0_code_literal = df.getOWLLiteral("1234", OWL2Datatype.XSD_INTEGER);
        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = tp.classParser.getFacts(gaulTestClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(3, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");
        final OWLDataPropertyAssertionAxiom parsed_code = owlDataPropertyAssertionAxioms.get().get(0);
        assertEquals(gaul_test, parsed_code.getSubject(), "Wrong named individual");
        assertEquals(adm0_code, parsed_code.getProperty(), "Data property IRIs don't match");
        assertEquals(adm0_code_literal, parsed_code.getObject(), "Data property values are wrong");

//        Name
        final OWLDataProperty adm0_name = df.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "adm0_name"));
        final OWLLiteral adm0_name_literal = df.getOWLLiteral("gaulTestClass");
        final OWLDataPropertyAssertionAxiom parsed_name = owlDataPropertyAssertionAxioms.get().get(1);
        assertEquals(gaul_test, parsed_name.getSubject(), "Wrong named individual");
        assertEquals(adm0_name, parsed_name.getProperty(), "Data property IRIs don't match");
        assertEquals(adm0_name_literal, parsed_name.getObject(), "Data property values are wrong");
    }

    @Test
    public void TestGAULComplexObjectClass() {

//        Test the class
        final OWLClass owlClass = tp.classParser.getObjectClass(complexObjectClass);
        final OWLClass gaul_test1 = df.getOWLClass(IRI.create(TRESTLE_PREFIX, "gaul-complex"));
        assertEquals(gaul_test1, owlClass, "Wrong OWL Class");
//        Test the named individual
//        Since we're using a UUID, we'll need to set it so we can match correctly
        final UUID individualUUID = UUID.randomUUID();
        complexObjectClass.id = individualUUID;
        OWLNamedIndividual owlNamedIndividual = tp.classParser.getIndividual(complexObjectClass);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, individualUUID.toString()));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Test the data property parser
//        Code
//        final OWLDataProperty adm0_code = dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "ADM0_Code"));
//        final OWLLiteral adm0_code_literal = dfStatic.getOWLLiteral("1234", OWL2Datatype.XSD_INTEGER);
        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = tp.classParser.getFacts(complexObjectClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(7, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");
        final OWLDataPropertyAssertionAxiom parsed_code = owlDataPropertyAssertionAxioms.get().get(0);
        assertEquals(gaul_test, parsed_code.getSubject(), "Wrong named individual");

//        Test the temporals
        Optional<List<TemporalObject>> temporalObjects = tp.temporalParser.getTemporalObjects(complexObjectClass);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(1, temporalObjects.get().size(), "Wrong number of objects");
        assertEquals(LocalDate.of(1989, 3, 26), temporalObjects.get().get(0).asInterval().getFromTime());

//        Construction

    }

    @Test
    public void TestExpandedGAULObject() {

//        Test the new gaul test
        //        Test the named individual
        OWLNamedIndividual owlNamedIndividual = tp.classParser.getIndividual(expandedGAULClass);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, "test_region"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = tp.classParser.getFacts(expandedGAULClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(3, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");

//        Test the temporal
        Optional<List<TemporalObject>> temporalObjects = tp.temporalParser.getTemporalObjects(expandedGAULClass);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(4, temporalObjects.get().size(), "Wrong number of temporal objects");
//        Check for the same type and scope for interval
        assertEquals(temporal.getType(), temporalObjects.get().get(0).getType(), "Wrong temporal type");
        assertEquals(temporal.getScope(), temporalObjects.get().get(0).getScope(), "Wrong temporal scope");
//        assertEquals(1, temporalObjects.get().get(0).getTemporalRelations().size(), "Wrong # of temporal relations");
//        assertEquals(gaul_test, temporalObjects.get().get(0).getTemporalRelations().stream().findFirst().get(), "Wrong temporal relation");
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
//        assertEquals(1, temporalObjects.get().get(1).getTemporalRelations().size(), "Wrong # of temporal relations");
//        assertEquals(gaul_test, temporalObjects.get().get(1).getTemporalRelations().stream().findFirst().get(), "Wrong temporal relation");

//        Check methods
//        Individual
        owlNamedIndividual = tp.classParser.getIndividual(testMethod);
        gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, "string_from_method"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Data properties
        owlDataPropertyAssertionAxioms = tp.classParser.getFacts(testMethod);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have method properties");
        assertEquals(5, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of data properties");

//        Get the adm0_code
        final Optional<OWLDataPropertyAssertionAxiom> adm0_code = owlDataPropertyAssertionAxioms
                .get()
                .stream()
                .filter(axiom -> axiom.getProperty().asOWLDataProperty().getIRI().getRemainder().get().equals("adm0_code"))
                .findAny();

        final Optional<OWLDataPropertyAssertionAxiom> asWKT = owlDataPropertyAssertionAxioms
                .get()
                .stream()
                .filter(axiom -> axiom.getProperty().asOWLDataProperty().getIRI().getRemainder().get().equals("asWKT"))
                .findAny();

        assertTrue(adm0_code.isPresent());
        assertTrue(asWKT.isPresent());
        assertEquals(OWL2Datatype.XSD_INT, adm0_code.get().getObject().getDatatype().getBuiltInDatatype(), "Should have integer datatype");
        assertEquals(testMethod.getAdm0_code1(), adm0_code.get().getObject().parseInteger(), "Invalid ADM0_Code");
        assertEquals(testMethod.test_name, asWKT.get().getObject().getLiteral(), "Invalid Spatial");
    }

    @Test
    public void testObjectConstructor() throws MissingConstructorException {
        List<OWLDataPropertyAssertionAxiom> testProperties = new ArrayList<>();
        List<TemporalObject> testTemporals = new ArrayList<>();
        final OWLNamedIndividual owlNamedIndividual = df.getOWLNamedIndividual(IRI.create("trestle:", "string_from_method"));
//        Build the data objects
        final OWLDataPropertyAssertionAxiom admcode = df.getOWLDataPropertyAssertionAxiom(
                df.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "adm0_code")),
                owlNamedIndividual,
                df.getOWLLiteral(4326));
        testProperties.add(admcode);

        final OWLDataPropertyAssertionAxiom adm0Name = df.getOWLDataPropertyAssertionAxiom(
                df.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "adm0_name")),
                owlNamedIndividual,
                df.getOWLLiteral("test region"));
        testProperties.add(adm0Name);

        final OWLDataPropertyAssertionAxiom testName = df.getOWLDataPropertyAssertionAxiom(
                df.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "test_name")),
                owlNamedIndividual,
                df.getOWLLiteral("new_test"));
        testProperties.add(testName);

        final IntervalTemporal defaultTemporal = TemporalObjectBuilder
                .valid()
                .from(LocalDateTime.of(1998, 3, 26, 0, 0))
                .to(LocalDateTime.of(1998, 3, 26, 0, 0).plusYears(1))
                .isDefault(true)
                .withParameterNames("defaultTime", null)
                .build();
//                .withRelations(owlNamedIndividual);
        testTemporals.add(defaultTemporal);


        final IntervalTemporal intervalTemporal = TemporalObjectBuilder
                .valid()
                .from(LocalDateTime.of(1989, 3, 26, 0, 0))
                .to(LocalDateTime.of(1989, 3, 26, 0, 0).plusYears(5))
                .withParameterNames("intervalStart", "intervalEnd")
                .build();
//                .withRelations(owlNamedIndividual);
        testTemporals.add(intervalTemporal);

//        Build the inputs and test the constructor
        List<Class<?>> inputClasses = new ArrayList<>();
        List<Object> inputObjects = new ArrayList<>();
        final Optional<List<OWLDataProperty>> propertyMembers = ClassBuilder.getPropertyMembers(TestClasses.GAULMethodTest.class);
//        if (propertyMembers.isPresent()) {
//            propertyMembers.get().forEach(property -> {
////                inputClasses.add(property.get)
//            });
//        }

        final ConstructorArguments constructorArguments = new ConstructorArguments();

//        Properties
        testProperties.forEach(property -> {
            final Class<?> javaClass = TypeConverter.lookupJavaClassFromOWLDatatype(property, TestClasses.GAULMethodTest.class);
            inputClasses.add(javaClass);
            final Object literalValue = TypeConverter.extractOWLLiteral(javaClass, property.getObject());
//            final Object literalValue = javaClass.cast(property.getObject().getLiteral());
            inputObjects.add(literalValue);
            constructorArguments.addArgument(
                    tp.classParser.matchWithClassMember(TestClasses.GAULMethodTest.class, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                    javaClass,
                    literalValue);
        });

        //        Temporals
        testTemporals.forEach(temporal -> {
            if (temporal.isPoint()) {
                constructorArguments.addArgument(temporal.asPoint().getParameterName(),
                        LocalDateTime.class,
                        temporal.asPoint().getPointTime());
                inputClasses.add(LocalDateTime.class);
                inputObjects.add(temporal.asPoint().getPointTime());
            } else {
//                Add the from time
                constructorArguments.addArgument(temporal.asInterval().getStartName(),
                        LocalDateTime.class,
                        temporal.asInterval().getFromTime());
                inputClasses.add(LocalDateTime.class);
                inputObjects.add(temporal.asInterval().getFromTime());
                if (!temporal.asInterval().isDefault()) {
                    final Optional<LocalDateTime> toTime = temporal.asInterval().getToTime();
                    if (toTime.isPresent()) {
                        constructorArguments.addArgument(temporal.asInterval().getEndName(),
                                LocalDateTime.class,
                                toTime.get());
                        inputClasses.add(LocalDateTime.class);
                        inputObjects.add(toTime.get());
                    }
                }
            }
        });

//        Check that they match
        assertEquals(inputClasses.size(), constructorArguments.getTypes().size(), "Wrong number of property classes");
        assertEquals(inputObjects.size(), constructorArguments.getValues().size(), "Wrong number of Property Values");

        final TestClasses.GAULMethodTest expectedClass = new TestClasses.GAULMethodTest();
//        final GAULMethodTest gaulMethodTest = ClassBuilder.constructObject(GAULMethodTest.class, inputClasses, inputObjects);
        final TestClasses.GAULMethodTest gaulMethodTest = ClassBuilder.constructObject(TestClasses.GAULMethodTest.class, constructorArguments);
        assertEquals(expectedClass, gaulMethodTest, "Should match");
    }

    @DatasetClass(name = "GAUL_Test")
    protected class ExpandedGAULTests {

        public int adm0_code;
        @IndividualIdentifier
        public String adm0_name;
        @Spatial
        public String test_name;
        @DefaultTemporal(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        @Ignore
        public LocalDateTime testtime;
        private String privateField;
        @DefaultTemporal(type = TemporalType.POINT, scope = TemporalScope.EXISTS, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDateTime testpoint;
        @StartTemporal(type = TemporalType.INTERVAL)
        public LocalDateTime teststart;
        @EndTemporal()
        public LocalDateTime testend;
        @StartTemporal(type = TemporalType.POINT)
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

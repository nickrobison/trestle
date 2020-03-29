package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.parser.clojure.ClojureProvider;
import com.nickrobison.trestle.reasoner.parser.clojure.ClojureTypeConverterProvider;
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
import java.util.*;

import static com.nickrobison.trestle.common.StaticIRI.GEOSPARQLPREFIX;
import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization", "unchecked", "WeakerAccess"})
public class TrestleParserTest {

    public TrestleParserTest() {
//        Not used
    }

    private TestClasses.GAULTestClass gaulTestClass;
    private ExpandedGAULTests expandedGAULClass;
    private OWLDataFactory df;
    private TemporalObject temporal;
    private TemporalObject temporalPoint;
    private TestClasses.GAULMethodTest testMethod;
    private TestClasses.GAULComplexClassTest complexObjectClass;
    private TestClasses.MultiLangTest multiLangTest;
    private TemporalParser tp;
    private IClassParser cp;
    private IClassBuilder cb;
    private IClassRegister cr;
    private ITypeConverter typeConverter;

    @BeforeEach
    public void Setup() {
        gaulTestClass = new TestClasses.GAULTestClass(1234, "gaulTestClass");
        expandedGAULClass = new ExpandedGAULTests();
        testMethod = new TestClasses.GAULMethodTest();
        complexObjectClass = new TestClasses.GAULComplexClassTest();
        multiLangTest = new TestClasses.MultiLangTest();
        df = OWLManager.getOWLDataFactory();
        LocalDateTime dt = LocalDateTime.of(1989, 3, 26, 0, 0);
        LocalDate ld = LocalDate.of(1989, 3, 26);
        temporal = TemporalObjectBuilder.exists().from(dt).to(dt.plusYears(1)).build(); //.withRelations();
        temporalPoint = TemporalObjectBuilder.exists().at(ld).build(); // .withRelations();
        this.typeConverter = ClojureTypeConverterProvider.buildClojureTypeConverter(df);
        final Object clojureParser = ClojureProvider.buildClojureParser(TRESTLE_PREFIX, true, "", 4326, typeConverter);
        cb = (IClassBuilder) clojureParser;
        cp = (IClassParser) clojureParser;
        cr = (IClassRegister) clojureParser;
        this.tp = new TemporalParser(cp);

    }

    @Test
    public void TestSimpleGAULClass() throws TrestleClassException {

//        Register some classes
        cr.registerClass(cp.getObjectClass(TestClasses.GAULTestClass.class), TestClasses.GAULTestClass.class);

//        Test the class
        final OWLClass owlClass = cp.getObjectClass(gaulTestClass);
        final OWLClass gaul_test1 = df.getOWLClass(IRI.create(TRESTLE_PREFIX, "GAUL_Test"));
        assertEquals(gaul_test1, owlClass, "Wrong OWL Class");
//        Test the named individual
        OWLNamedIndividual owlNamedIndividual = cp.getIndividual(gaulTestClass);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, "test_me"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Test the data property parser
//        Code
        final IRI id_iri = IRI.create(TRESTLE_PREFIX, "id");
        final OWLDataProperty id = df.getOWLDataProperty(id_iri);
        final OWLLiteral id_literal = df.getOWLLiteral("1234", OWL2Datatype.XSD_INTEGER);
        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = cp.getFacts(gaulTestClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(3, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");
        final OWLDataPropertyAssertionAxiom parsed_code = owlDataPropertyAssertionAxioms.get().get(0);
        assertEquals(gaul_test, parsed_code.getSubject(), "Wrong named individual");
        assertEquals(id, parsed_code.getProperty(), "Data property IRIs don't match");
        assertEquals(id_literal, parsed_code.getObject(), "Data property values are wrong");

//        Name
        final OWLDataProperty adm0_name = df.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "adm0_name"));
        final OWLLiteral adm0_name_literal = df.getOWLLiteral("gaulTestClass");
        final OWLDataPropertyAssertionAxiom parsed_name = owlDataPropertyAssertionAxioms.get().get(1);
        assertEquals(gaul_test, parsed_name.getSubject(), "Wrong named individual");
        assertEquals(adm0_name, parsed_name.getProperty(), "Data property IRIs don't match");
        assertEquals(adm0_name_literal, parsed_name.getObject(), "Data property values are wrong");

//        Check for Fact name
        final Optional<Class<?>> factDatatype = cp.getFactDatatype(TestClasses.GAULTestClass.class, id_iri.toString());
        assertAll(() -> assertTrue(factDatatype.isPresent(), "Should have datatype"),
                () -> assertEquals(int.class, factDatatype.get(), "Should equal primitive int"));
    }

    @Test
    public void TestGAULComplexObjectClass() throws TrestleClassException {

        cr.registerClass(cp.getObjectClass(TestClasses.GAULComplexClassTest.class), TestClasses.GAULComplexClassTest.class);

//        Test the class
        final OWLClass owlClass = cp.getObjectClass(complexObjectClass);
        final OWLClass gaul_test1 = df.getOWLClass(IRI.create(TRESTLE_PREFIX, "gaul-complex"));
        assertEquals(gaul_test1, owlClass, "Wrong OWL Class");
//        Test the named individual
//        Since we're using a UUID, we'll need to set it so we can match correctly
        final UUID individualUUID = UUID.randomUUID();
        complexObjectClass.id = individualUUID;
        OWLNamedIndividual owlNamedIndividual = cp.getIndividual(complexObjectClass);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, individualUUID.toString()));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Test the data property parser
//        Code
//        final OWLDataProperty adm0_code = dfStatic.getOWLDataProperty(IRI.create(TRESTLE_PREFIX, "ADM0_Code"));
//        final OWLLiteral adm0_code_literal = dfStatic.getOWLLiteral("1234", OWL2Datatype.XSD_INTEGER);
        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = cp.getFacts(complexObjectClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(7, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");
        final OWLDataPropertyAssertionAxiom parsed_code = owlDataPropertyAssertionAxioms.get().get(0);
        assertEquals(gaul_test, parsed_code.getSubject(), "Wrong named individual");

//        Test the temporals
        Optional<List<TemporalObject>> temporalObjects = tp.getTemporalObjects(complexObjectClass);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(1, temporalObjects.get().size(), "Wrong number of objects");
        assertEquals(LocalDate.of(1989, 3, 26), temporalObjects.get().get(0).asInterval().getFromTime());

//        Construction

    }

    @Test
    public void TestExpandedGAULObject() throws TrestleClassException {

        cr.registerClass(cp.getObjectClass(ExpandedGAULTests.class), ExpandedGAULTests.class);
        cr.registerClass(cp.getObjectClass(TestClasses.GAULMethodTest.class), TestClasses.GAULMethodTest.class);

//        Test the new gaul test
        //        Test the named individual
        OWLNamedIndividual owlNamedIndividual = cp.getIndividual(expandedGAULClass);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, "test_region"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = cp.getFacts(expandedGAULClass);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(3, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");

        //        Check member matching
        final IRI memberIRI = IRI.create(TRESTLE_PREFIX, "name");
        final String filteredName = cp.matchWithClassMember(TestClasses.GAULMethodTest.class, memberIRI.getShortForm());
        assertEquals("name", filteredName, "Should match member name");

//        Check spatial member access
        final Optional<OWLDataPropertyAssertionAxiom> spatialFact = cp.getSpatialFact(testMethod);
        assertAll(() -> assertTrue(spatialFact.isPresent(), "Should have spatial fact"),
                () -> assertEquals("<http://www.opengis.net/def/crs/OGC/1.3/CRS84> new_test", spatialFact.get().getObject().getLiteral(), "Spatial should match"));


//        Test the temporal
        Optional<List<TemporalObject>> temporalObjects = tp.getTemporalObjects(expandedGAULClass);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(1, temporalObjects.get().size(), "Wrong number of temporal objects");
//        Check for the same type and scope for interval
        assertEquals(temporal.getType(), temporalObjects.get().get(0).getType(), "Wrong temporal type");
        assertEquals(temporal.getScope(), temporalObjects.get().get(0).getScope(), "Wrong temporal scope");
//        assertEquals(1, temporalObjects.get().get(0).getTemporalRelations().size(), "Wrong # of temporal relations");
//        assertEquals(gaul_test, temporalObjects.get().get(0).getTemporalRelations().stream().findFirst().get(), "Wrong temporal relation");
//        Check for the correct values on the built temporal objects
        //        Interval
        assertTrue(temporalObjects.get().get(0).isInterval(), "Should have build an interval object");
        assertEquals(LocalDateTime.of(1998, 3, 26, 0, 0), temporalObjects.get().get(0).asInterval().getFromTime());
        assertEquals(LocalDateTime.of(1998, 3, 26, 0, 0).plusYears(1), temporalObjects.get().get(0).asInterval().getToTime().get());

//        Check methods
//        Individual
        owlNamedIndividual = cp.getIndividual(testMethod);
        gaul_test = df.getOWLNamedIndividual(IRI.create(TRESTLE_PREFIX, "string_from_method"));
        assertEquals(gaul_test, owlNamedIndividual, "Wrong named individual");

//        Data properties
        owlDataPropertyAssertionAxioms = cp.getFacts(testMethod);
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
        assertEquals("<http://www.opengis.net/def/crs/OGC/1.3/CRS84> " + testMethod.test_name, asWKT.get().getObject().getLiteral(), "Invalid Spatial");

        // Check for related fields
        assertTrue(this.cp.isFactRelated(ExpandedGAULTests.class, "adm0_code"), "Should be related");
    }

    @Test
    public void multiLangTest() throws TrestleClassException {

        cr.registerClass(cp.getObjectClass(TestClasses.MultiLangTest.class), TestClasses.MultiLangTest.class);
        final Optional<List<OWLDataPropertyAssertionAxiom>> facts = cp.getFacts(multiLangTest);
        assertAll(() -> assertTrue(facts.isPresent(), "Should have facts"),
                () -> assertEquals(7, facts.get().size(), "Should have lots of facts"));

//        Try to match on a multilang lang fact
        final IRI iri = IRI.create(TRESTLE_PREFIX, "testString");
        final String classMember = cp.matchWithClassMember(TestClasses.MultiLangTest.class, iri.getShortForm(), "en-GB");
        assertEquals("englishGBString", classMember, "Should match with en-GB method");

//        Check the return type
        final Optional<Class<?>> factDatatype = cp.getFactDatatype(TestClasses.MultiLangTest.class, iri.toString());
        assertEquals(String.class, factDatatype.get(), "Should have String datatype");

//        Check to match on the non-language
        final IRI idi_iri = IRI.create(TRESTLE_PREFIX, "id");
        final String id = cp.matchWithClassMember(TestClasses.MultiLangTest.class, "id");
        assertEquals("id", id, "Should get ID field");
        final Optional<Class<?>> factDatatype1 = cp.getFactDatatype(TestClasses.MultiLangTest.class, idi_iri.toString());
        assertEquals(String.class, factDatatype1.get(), "Should have string for ID class");

    }

    @Test
    public void testObjectConstructor() throws TrestleClassException {

        cr.registerClass(cp.getObjectClass(TestClasses.GAULMethodTest.class), TestClasses.GAULMethodTest.class);
        cr.registerClass(cp.getObjectClass(ExpandedGAULTests.class), ExpandedGAULTests.class);

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
                df.getOWLDataProperty(IRI.create(GEOSPARQLPREFIX, "asWKT")),
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
        final Optional<List<OWLDataProperty>> propertyMembers = cb.getPropertyMembers(TestClasses.GAULMethodTest.class);
//        if (propertyMembers.isPresent()) {
//            propertyMembers.get().forEach(property -> {
////                inputClasses.add(property.get)
//            });
//        }


        final ConstructorArguments constructorArguments = new ConstructorArguments();


//        Properties
        testProperties.forEach(property -> {
            final Class<?> javaClass = this.typeConverter.lookupJavaClassFromOWLDatatype(property, cp.getFactDatatype(TestClasses.GAULMethodTest.class, property.getProperty().asOWLDataProperty().toStringID()).get());
            inputClasses.add(javaClass);
            final Object literalValue = this.typeConverter.extractOWLLiteral(javaClass, property.getObject());
//            final Object literalValue = javaClass.cast(property.getObject().getLiteral());
            inputObjects.add(literalValue);
            constructorArguments.addArgument(
                    cp.matchWithClassMember(TestClasses.GAULMethodTest.class, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
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
//        final TestClasses.GAULMethodTest gaulMethodTest = ClassBuilder.constructObject(TestClasses.GAULMethodTest.class, constructorArguments);
        final TestClasses.GAULMethodTest gaulMethodTest = cb.constructObject(TestClasses.GAULMethodTest.class, constructorArguments);
        assertEquals(expectedClass, gaulMethodTest, "Should match");

//        Try to build something with extra constructor args
        cp.parseClass(ExpandedGAULTests.class);
        final ConstructorArguments extraArgs = new ConstructorArguments();
        extraArgs.addArgument("adm0_code", int.class, 42);
        extraArgs.addArgument("should_not_match", String.class, "Nothing there");
        extraArgs.addArgument("nope_integer", Integer.class, 1);
        final ExpandedGAULTests constructedObject = cb.constructObject(ExpandedGAULTests.class, extraArgs);
        assertEquals(new ExpandedGAULTests(42), constructedObject, "Extra arguments objects should match");
    }

    @DatasetClass(name = "GAUL_Test")
    public static class ExpandedGAULTests {

        @Related
        public int adm0_code;
        @IndividualIdentifier
        public String adm0_name;
        @Spatial
        public String test_name;
        @DefaultTemporal(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        public LocalDateTime testtime;
        private String privateField;

        public ExpandedGAULTests() {
            this.adm0_code = 4326;
            this.test_name = "new_test";
            this.adm0_name = "test region";
            this.testtime = LocalDateTime.of(1998, 3, 26, 0, 0);
            this.privateField = "don't read me";
        }

        public ExpandedGAULTests(int adm0_code) {
            this.adm0_code = adm0_code;
            this.test_name = "new_test";
            this.adm0_name = "test region";
            this.testtime = LocalDateTime.of(1998, 3, 26, 0, 0);
            this.privateField = "don't read me";
        }

        @Ignore
        public String getPrivateField() {
            return this.privateField;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExpandedGAULTests that = (ExpandedGAULTests) o;
            return adm0_code == that.adm0_code &&
                    Objects.equals(adm0_name, that.adm0_name) &&
                    Objects.equals(test_name, that.test_name) &&
                    Objects.equals(testtime, that.testtime) &&
                    Objects.equals(privateField, that.privateField);
        }

        @Override
        public int hashCode() {

            return Objects.hash(adm0_code, adm0_name, test_name, testtime, privateField);
        }
    }
}

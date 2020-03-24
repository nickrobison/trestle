package com.nickrobison.trestle.testing;

import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.collectionOfIRI;
import static com.nickrobison.trestle.common.StaticIRI.hasFactIRI;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 12/1/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization.fields.uninitialized"})
@Tag("integration")
public abstract class OntologyTest {

    protected OWLDataFactory df;
    protected ITrestleOntology ontology;
    protected Config config;
    protected InputStream inputStream;

    @BeforeEach
    public void setup() throws OWLOntologyCreationException, IOException {
        df = OWLManager.getOWLDataFactory();
        config = ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf"));
        final IRI iri = IRI.create(config.getString("trestle.ontology.location"));
        if (!iri.isAbsolute()) {
            final Path cwd = Paths.get("");
            final Path absPath = Paths.get(cwd.toAbsolutePath().toString(), config.getString("trestle.ontology.location"));
            inputStream = absPath.toUri().toURL().openConnection().getInputStream();
        } else {
            inputStream = iri.toURI().toURL().openConnection().getInputStream();
        }

        setupOntology();
    }

    protected abstract void setupOntology() throws OWLOntologyCreationException;

    @Test
    public void testTypedLiterals() throws MissingOntologyEntity {
        final String testString = "Test String";

//       Try to create two typed literals
        final OWLClass owlCl = df.getOWLClass(IRI.create("trestle:", "test"));
        final OWLNamedIndividual typedIndividual = df.getOWLNamedIndividual(IRI.create("trestle:", "typedIndividual"));
        final OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, typedIndividual);
        final OWLDataProperty testStringProperties = df.getOWLDataProperty(IRI.create("trestle:", "testString"));
        final OWLLiteral englishLiteral = df.getOWLLiteral(String.format("%s@en", testString), OWL2Datatype.RDF_PLAIN_LITERAL);
        final OWLLiteral frenchLiteral = df.getOWLLiteral(String.format("%s@fr", testString), OWL2Datatype.RDF_PLAIN_LITERAL);
        final OWLLiteral plainLiteral = df.getOWLLiteral(testString, OWL2Datatype.RDF_PLAIN_LITERAL);
        final OWLDataPropertyAssertionAxiom englishAxiom = df.getOWLDataPropertyAssertionAxiom(testStringProperties, typedIndividual, englishLiteral);
        final OWLDataPropertyAssertionAxiom frenchAxiom = df.getOWLDataPropertyAssertionAxiom(testStringProperties, typedIndividual, frenchLiteral);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(englishAxiom);
        ontology.writeIndividualDataProperty(frenchAxiom);
        ontology.writeIndividualDataProperty(typedIndividual, testStringProperties, plainLiteral);
        final Set<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getAllDataPropertiesForIndividual(typedIndividual);
        Assertions.assertEquals(3, allDataPropertiesForIndividual.size(), "Should have the 4 literals");

//        Check sparql
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "SELECT DISTINCT ?m ?object ?property WHERE {" +
                "?m rdf:type :test ." +
                "?m ?object ?property ." +
                "FILTER(?property = 'Test String'@fr)}";

        final TrestleResultSet trestleResultSet = ontology.executeSPARQLResults(queryString);
        Assertions.assertEquals(1, trestleResultSet.getResults().size(), "Should only have the french version");
        Assertions.assertEquals("Test String", trestleResultSet.getResults().get(0).getLiteral("property").get().getLiteral(), "Should have the correct string");
        Assertions.assertEquals("fr", trestleResultSet.getResults().get(0).getLiteral("property").get().getLang(), "Should be french language");

    }

    protected abstract void shutdownOntology();

    @Test
    public void SimpleCreationTest() throws OWLOntologyStorageException, MissingOntologyEntity {

        final OWLNamedIndividual test_individual = df.getOWLNamedIndividual(IRI.create("trestle:", "test_individual"));
        final OWLClass owlClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));
        final OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlClass, test_individual);
        final OWLDataProperty trestle_property = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));
        final OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom = df.getOWLDataPropertyAssertionAxiom(trestle_property, test_individual, 42);

        final OWLDataProperty test_new_property = df.getOWLDataProperty(IRI.create("trestle:", "test_new_property"));
        final OWLLiteral owlLiteral = df.getOWLLiteral("hello world", OWL2Datatype.XSD_STRING);
        final OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom1 = df.getOWLDataPropertyAssertionAxiom(test_new_property, test_individual, owlLiteral);

//        Check if the ontology has what we want
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        Assertions.assertFalse(ontology.containsResource(test_individual), "Shouldn't have the individual");
        Assertions.assertTrue(ontology.containsResource(owlClass), "Should have the class");
        Assertions.assertTrue(ontology.containsResource(trestle_property), "Should have the ADM_0 Code");
        Assertions.assertFalse(ontology.containsResource(test_new_property), "Shouldn't have test property");
        this.ontology.returnAndCommitTransaction(trestleTransaction);


//        Try to write everything
        final TrestleTransaction t1 = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom1);
        this.ontology.returnAndCommitTransaction(t1);

        final TrestleTransaction t2 = this.ontology.createandOpenNewTransaction(false);
        Optional<Set<OWLLiteral>> individualProperty = ontology.getIndividualDataProperty(test_individual, trestle_property);
        Assertions.assertTrue(individualProperty.isPresent(), "Should have values");
        Assertions.assertEquals(1, individualProperty.get().size(), "Wrong number of values");
        Assertions.assertEquals(42, individualProperty.get().stream().findFirst().get().parseInteger(), "Wrong property");

        individualProperty = ontology.getIndividualDataProperty(test_individual, test_new_property);
        Assertions.assertTrue(individualProperty.isPresent(), "Should have values");
        Assertions.assertEquals(1, individualProperty.get().size(), "Wrong number of values");
        Assertions.assertEquals(owlLiteral, individualProperty.get().stream().findFirst().get(), "Wrong property literal");
        this.ontology.returnAndCommitTransaction(t2);

////        Try to write a property value to an individual that doesn't exist
//        final OWLNamedIndividual missing_individual = df.getOWLNamedIndividual(IRI.create("trestle:", "missing_individual"));
//        final OWLDataPropertyAssertionAxiom propertyForMissingIndividual = df.getOWLDataPropertyAssertionAxiom(
//                trestle_property,
//                missing_individual,
//                42);
//        ontology.writeIndividualDataProperty(propertyForMissingIndividual);
//
////        Try to write a value for a non-existent property to an existing individual
//        final OWLDataProperty missing_data_property = df.getOWLDataProperty(IRI.create("trestle:", "missing_data_property"));
//        final OWLDataPropertyAssertionAxiom missingPropertyForIndividual = df.getOWLDataPropertyAssertionAxiom(
//                missing_data_property,
//                test_individual,
//                42);
//        ontology.writeIndividualDataProperty(propertyForMissingIndividual);
//
////        Test object Property as well
//        final OWLObjectProperty missing_object_property = df.getOWLObjectProperty(IRI.create("trestle:", "missing_object_property"));
//        final OWLObjectPropertyAssertionAxiom missingObjectProperty = df.getOWLObjectPropertyAssertionAxiom(
//                missing_object_property,
//                test_individual,
//                missing_individual);
//
//        ontology.writeIndividualObjectProperty(missingObjectProperty);
    }

    @Test
    public void testAbortHandling() throws MissingOntologyEntity {
        final OWLNamedIndividual test_individual = df.getOWLNamedIndividual(IRI.create("trestle:", "test_individual"));
        final OWLClass owlClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));
        final OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlClass, test_individual);
        final OWLDataProperty trestle_property = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));
        final OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom = df.getOWLDataPropertyAssertionAxiom(trestle_property, test_individual, 42);

        final OWLDataProperty test_new_property = df.getOWLDataProperty(IRI.create("trestle:", "test_new_property"));
        final OWLLiteral owlLiteral = df.getOWLLiteral("hello world", OWL2Datatype.XSD_STRING);
        final OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom1 = df.getOWLDataPropertyAssertionAxiom(test_new_property, test_individual, owlLiteral);

//        Try to write an individual and a single data property
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(true);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom);
        this.ontology.returnAndCommitTransaction(trestleTransaction);
        Set<OWLDataPropertyAssertionAxiom> dataProperties = ontology.getAllDataPropertiesForIndividual(test_individual);
//        assertEquals(1, dataProperties.get().size(), "Should only have based data properties after aborting previous transaction");


        TrestleTransaction t2 = this.ontology.createandOpenNewTransaction(true);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom1);
        this.ontology.returnAndAbortTransaction(t2);
        this.ontology.returnAndCommitTransaction(t2);
        final Set<OWLDataPropertyAssertionAxiom> abortedProperties = ontology.getAllDataPropertiesForIndividual(test_individual);
        Assertions.assertEquals(dataProperties.size(), abortedProperties.size(), "Should only have based data properties after aborting previous transaction");

        t2 = this.ontology.createandOpenNewTransaction(true);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom1);
        this.ontology.returnAndCommitTransaction(t2);
        final Set<OWLDataPropertyAssertionAxiom> committedProperties = ontology.getAllDataPropertiesForIndividual(test_individual);
        Assertions.assertTrue(committedProperties.size() > dataProperties.size(), "Should only have based data properties after aborting previous transaction");
    }

    @Test
    public void testRelationAssociation() {

//        Check to ensure the relation is inferred
        final TrestleTransaction trestleTransaction = this.ontology.createandOpenNewTransaction(false);
        final OWLNamedIndividual testMaputo = df.getOWLNamedIndividual(IRI.create("trestle:", "maputo:2013:3000"));
        final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(StaticIRI.hasFactIRI);
        final Optional<List<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(testMaputo, owlObjectProperty);
        assertAll(() -> assertTrue(individualObjectProperty.isPresent(), "Should have related facts"),
                () -> assertEquals(4, individualObjectProperty.get().size(), "Wrong number of facts"));

//        Now for the sparql query
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "SELECT DISTINCT ?f ?m WHERE { ?m rdf:type :GAUL . " +
                "?m :has_fact ?f ." +
                "?f ?property ?object ." +
                "FILTER(!isURI(?object) && !isBlank(?object) && ?object = 41374) }";

        final TrestleResultSet resultSet = ontology.executeSPARQLResults(queryString);
        Assertions.assertEquals(1, resultSet.getResults().size(), "Wrong number of relations");

//        Test for spatial/temporal object relations and that they're inferred correctly.
        queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "SELECT DISTINCT ?m ?p ?o WHERE { ?m rdf:type :GAUL . ?m ?p ?o. ?p rdfs:subPropertyOf :Temporal_Relation . " +
                "VALUES ?m {<http://nickrobison.com/dissertation/trestle.owl#municipal1:1990:2013>} }";

        final TrestleResultSet trestleResultSet = ontology.executeSPARQLResults(queryString);
        Set<OWLObjectPropertyAssertionAxiom> temporalRelations = trestleResultSet.getResults().stream().map(solution ->
                df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(IRI.create(solution.unwrapIndividual("p").toStringID())),
                        df.getOWLNamedIndividual(IRI.create(solution.unwrapIndividual("m").toStringID())),
                        df.getOWLNamedIndividual(IRI.create(solution.unwrapIndividual("o").toStringID()))))
                .collect(Collectors.toSet());
        Assertions.assertAll(() -> Assertions.assertEquals(14, temporalRelations.size(), "Wrong number of temporal relations for municipal1"),
                () -> Assertions.assertTrue(temporalRelations
                        .stream()
                        .anyMatch(relation -> relation.getObject().equals(df.getOWLNamedIndividual(IRI.create("http://nickrobison.com/dissertation/trestle.owl#municipal2:1990:2013")))), "municipal1 is not related to municipal2")
        );
        this.ontology.returnAndCommitTransaction(trestleTransaction);
    }

    //    Override Tests
    abstract public void testByteParsing() throws MissingOntologyEntity;

    @AfterEach
    public void shutdown() {
        shutdownOntology();
    }

    @Test
    @Disabled
    public void testPropertyChaining() {
//        Try to write some test rules, maybe?


//        Try to read test objects
        final Optional<List<OWLObjectPropertyAssertionAxiom>> collectionMembers = ontology.getIndividualObjectProperty(df.getOWLNamedIndividual("trestle:", "Cidade_de_maputo_collection"), df.getOWLObjectProperty(StaticIRI.collectionOfIRI));
        Assertions.assertAll(() -> Assertions.assertTrue(collectionMembers.isPresent()),
                () -> Assertions.assertTrue(collectionMembers.get().size() > 0),
                () -> Assertions.assertEquals(df.getOWLNamedIndividual(IRI.create("trestle:", "maputo:2013:3000")), collectionMembers.get().stream().findFirst().get().getSubject(), "Should have maputo object"));
    }
}

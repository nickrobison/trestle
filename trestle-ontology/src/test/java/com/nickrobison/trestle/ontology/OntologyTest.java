package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.conceptOfIRI;
import static com.nickrobison.trestle.common.StaticIRI.hasFactIRI;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 12/1/16.
 */
@Tag("integration")
abstract public class OntologyTest {

    protected OWLDataFactory df;
    protected ITrestleOntology ontology;
    protected Config config;
    protected InputStream inputStream;

    @BeforeEach
    public void setup() throws OWLOntologyCreationException, IOException {
        df = OWLManager.getOWLDataFactory();
        config = ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf"));
        final IRI iri = IRI.create(config.getString("trestle.ontology.location"));
        inputStream = iri.toURI().toURL().openConnection().getInputStream();
        setupOntology();
    }

    abstract void setupOntology() throws OWLOntologyCreationException;

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
        assertEquals(3, allDataPropertiesForIndividual.size(), "Should have the 4 literals");

//        Check sparql
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "SELECT DISTINCT ?m ?object ?property WHERE {" +
                "?m rdf:type :test ." +
                "?m ?object ?property ." +
                "FILTER(?property = 'Test String'@fr)}";

        final TrestleResultSet trestleResultSet = ontology.executeSPARQLTRS(queryString);
        assertEquals(1, trestleResultSet.getResults().size(), "Should only have the french version");
        assertEquals("Test String", trestleResultSet.getResults().get(0).getLiteral("property").getLiteral(), "Should have the correct string");
        assertEquals("fr", trestleResultSet.getResults().get(0).getLiteral("property").getLang(), "Should be french language");

    }

    abstract void shutdownOntology();

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
        assertFalse(ontology.containsResource(test_individual), "Shouldn't have the individual");
        assertTrue(ontology.containsResource(owlClass), "Should have the class");
        assertTrue(ontology.containsResource(trestle_property), "Should have the ADM_0 Code");
        assertFalse(ontology.containsResource(test_new_property), "Shouldn't have test property");


//        Try to write everything
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom1);

        Optional<Set<OWLLiteral>> individualProperty = ontology.getIndividualDataProperty(test_individual, trestle_property);
        assertTrue(individualProperty.isPresent(), "Should have values");
        assertEquals(1, individualProperty.get().size(), "Wrong number of values");
        assertEquals(42, individualProperty.get().stream().findFirst().get().parseInteger(), "Wrong property");

        individualProperty = ontology.getIndividualDataProperty(test_individual, test_new_property);
        assertTrue(individualProperty.isPresent(), "Should have values");
        assertEquals(1, individualProperty.get().size(), "Wrong number of values");
        assertEquals(owlLiteral, individualProperty.get().stream().findFirst().get(), "Wrong property literal");

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
    public void testRelationAssociation() {

//        Check to ensure the relation is inferred
        final OWLNamedIndividual test_maputo = df.getOWLNamedIndividual(IRI.create("trestle:", "maputo:2013:3000"));
        final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(hasFactIRI);
        final Optional<List<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(test_maputo, owlObjectProperty);
        assertTrue(individualObjectProperty.isPresent(), "Should have related facts");
        assertEquals(4, individualObjectProperty.get().size(), "Wrong number of facts");

//        Now for the sparql query
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "SELECT DISTINCT ?f ?m ?t WHERE { ?m rdf:type :GAUL . " +
                "?m :has_fact ?f ." +
                "?f ?property ?object ." +
                "?f :valid_time ?t." +
                "FILTER(!isURI(?object) && !isBlank(?object) && ?object = 41374) }";

        final TrestleResultSet resultSet = ontology.executeSPARQLTRS(queryString);
        assertEquals(1, resultSet.getResults().size(), "Wrong number of relations");

//        Test for spatial/temporal object relations and that they're inferred correctly.
        queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "SELECT DISTINCT ?m ?p ?o WHERE { ?m rdf:type :GAUL . ?m ?p ?o. ?p rdfs:subPropertyOf :Temporal_Relation . " +
                "VALUES ?m {<http://nickrobison.com/dissertation/trestle.owl#municipal1:1990:2013>} }";

        final TrestleResultSet trestleResultSet = ontology.executeSPARQLTRS(queryString);
        Set<OWLObjectPropertyAssertionAxiom> temporalRelations = trestleResultSet.getResults().stream().map(solution ->
                df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(IRI.create(solution.getIndividual("p").toStringID())),
                        df.getOWLNamedIndividual(IRI.create(solution.getIndividual("m").toStringID())),
                        df.getOWLNamedIndividual(IRI.create(solution.getIndividual("o").toStringID()))))
                .collect(Collectors.toSet());
        assertAll(() -> assertEquals(12, temporalRelations.size(), "Wrong number of temporal relations for municipal1"),
                () -> assertTrue(temporalRelations
                        .stream()
                        .anyMatch(relation -> relation.getObject().equals(df.getOWLNamedIndividual(IRI.create("http://nickrobison.com/dissertation/trestle.owl#municipal2:1990:2013")))), "test_maputo is not related to municipal2")
        );
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
        final Optional<List<OWLObjectPropertyAssertionAxiom>> conceptMembers = ontology.getIndividualObjectProperty(df.getOWLNamedIndividual("trestle:", "Cidade_de_maputo_concept"), df.getOWLObjectProperty(conceptOfIRI));
        assertAll(() -> assertTrue(conceptMembers.isPresent()),
                () -> assertTrue(conceptMembers.get().size() > 0),
                () -> assertEquals(df.getOWLNamedIndividual(IRI.create("trestle:", "maputo:2013:3000")), conceptMembers.get().stream().findFirst().get().getSubject(), "Should have maputo object"));
    }
}

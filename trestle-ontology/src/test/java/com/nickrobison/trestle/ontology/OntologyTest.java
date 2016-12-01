package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.hasFactIRI;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 12/1/16.
 */
abstract public class OntologyTest {

    protected OWLDataFactory df;
    protected ITrestleOntology ontology;
    protected Config config;
    protected InputStream inputStream;

    @BeforeEach
    public void setup() throws OWLOntologyCreationException, IOException {
        df = OWLManager.getOWLDataFactory();
        config = ConfigFactory.parseResources("test.configuration.conf");
        final IRI iri = IRI.create(config.getString("trestle.ontology.location"));
        inputStream = iri.toURI().toURL().openConnection().getInputStream();
        setupOntology();
    }

    abstract void setupOntology() throws OWLOntologyCreationException;

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
    }

    @Test
    public void testRelationAssociation() {

//        Check to ensure the relation is inferred
        final OWLNamedIndividual test_maputo = df.getOWLNamedIndividual(IRI.create("trestle:", "maputo:2013:3000"));
        final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(hasFactIRI);
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(test_maputo, owlObjectProperty);
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

        final List<QuerySolution> resultSet = ResultSetFormatter.toList(ontology.executeSPARQL(queryString));
        assertEquals(1, resultSet.size(), "Wrong number of relations");

    }

    //    Override Tests
    abstract public void testByteParsing() throws MissingOntologyEntity;

    @AfterEach
    public void shutdown() {
        shutdownOntology();
    }
}

package com.nickrobison.trestle.ontology;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.LocalOntology;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.ontology.VirtuosoOntology;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 7/25/16.
 */
@SuppressWarnings({"Duplicates", "initialization"})
public class VirtuosoOntologyTest {



    private VirtuosoOntology ontology;
    private OWLDataFactory df;

    @BeforeEach
    public void setupNewOntology() throws OWLOntologyCreationException {
        final IRI iri = IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl");
        df = OWLManager.getOWLDataFactory();

        ontology = (VirtuosoOntology) new OntologyBuilder()
                .fromIRI(iri)
                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
                .name("trestle")
                .build().get();

        ontology.initializeOntology();
    }

    @Test
    public void simpleTest() {
//        ontology.initializeOntology();

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#> " +
                "SELECT * WHERE {?m rdf:type ?type . ?type rdfs:subClassOf ?class}";
//        String queryString = " SELECT ?subject ?prop ?object WHERE { ?subject ?prop ?object } ";

//        final ResultSet resultSet = ontology.executeSPARQL(queryString);
//        assertEquals(29, resultSet.getRowNumber(), "Wrong number of classes");

//        final long tripleCount = ontology.getTripleCount();
//        assertEquals(381, tripleCount, "Inference is wrong");

        final OWLNamedIndividual burundi_0 = df.getOWLNamedIndividual(IRI.create("trestle:", "Burundi_0"));
        final OWLDataProperty property = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));

        Optional<Set<OWLLiteral>> individualProperty = ontology.getIndividualProperty(burundi_0, property);
        assertEquals(43, individualProperty.get().stream().findFirst().get().parseInteger(), "ADM0_Code is wrong");

//        Try for wrong individual
        final OWLNamedIndividual burundi_wrong = df.getOWLNamedIndividual(IRI.create("trestle:", "Bwrong_0"));
        final Optional<Set<OWLLiteral>> wrongIndividual = ontology.getIndividualProperty(burundi_wrong, property);
        assertFalse(wrongIndividual.isPresent(), "Should be empty optional");

//        Try for wrong property
        final OWLDataProperty property_wrong = df.getOWLDataProperty(IRI.create("trestle:", "ADM1_Code"));
        final Optional<Set<OWLLiteral>> propertyWrong = ontology.getIndividualProperty(burundi_0, property_wrong);
        assertFalse(propertyWrong.isPresent(), "Should be empty optional");

//        Try wkt literal
        final OWLNamedIndividual test_maputo = df.getOWLNamedIndividual(IRI.create("trestle:", "test_maputo"));
        final OWLDataProperty asWKT = df.getOWLDataProperty(IRI.create("geosparql:", "asWKT"));
        individualProperty = ontology.getIndividualProperty(burundi_0, asWKT);
        final OWLLiteral wktLiteral = individualProperty.get().stream().findFirst().get();
//        Virtuoso returns wkts as doubles and without a comma
        assertEquals("POINT(30.0 10.0)", wktLiteral.getLiteral(), "WKT is wrong");


//        Test object properties
        final OWLObjectProperty has_temporal = df.getOWLObjectProperty(IRI.create("trestle:", "has_temporal"));
        Optional<Set<OWLObjectProperty>> individualObjectProperty = ontology.getIndividualObjectProperty(burundi_0, has_temporal);
        assertEquals("Burundi_Valid", individualObjectProperty.get().stream().findFirst().get().getIRI().getRemainder().get(), "Should be burundi_valid");

//        Try for wrong individual
        individualObjectProperty = ontology.getIndividualObjectProperty(burundi_wrong, has_temporal);
        assertFalse(individualObjectProperty.isPresent(), "Should have no properties");

//        Try for wrong property
        final OWLObjectProperty has_wrong = df.getOWLObjectProperty(IRI.create("trestle:", "has_wrong"));
        individualObjectProperty = ontology.getIndividualObjectProperty(burundi_0, has_wrong);
        assertFalse(individualObjectProperty.isPresent(), "Should have no properties");

//        Try for inferred property
//        FIXME(nrobison): Inference is totally broken.
//        final OWLNamedIndividual test_muni2 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni2"));
//        individualObjectProperty = ontology.getIndividualObjectProperty(test_muni2, has_temporal);
//        assertTrue(individualObjectProperty.isPresent(), "Should have inferred property");
//        assertEquals("test_muni1_valid", individualObjectProperty.get().stream().findFirst().get().getIRI().getRemainder().get(), "Should be test_muni_1_valid");

    }

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
        assertTrue( ontology.containsResource(trestle_property), "Should have the ADM_0 Code");
        assertFalse(ontology.containsResource(test_new_property), "Shouldn't have test property");


//        Try to write everything
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom);
        ontology.writeIndividualDataProperty(owlDataPropertyAssertionAxiom1);

        Optional<Set<OWLLiteral>> individualProperty = ontology.getIndividualProperty(test_individual, trestle_property);
        assertTrue(individualProperty.isPresent(), "Should have values");
        assertEquals(1, individualProperty.get().size(), "Wrong number of values");
        assertEquals(42, individualProperty.get().stream().findFirst().get().parseInteger(), "Wrong property");

        individualProperty = ontology.getIndividualProperty(test_individual, test_new_property);
        assertTrue(individualProperty.isPresent(), "Should have values");
        assertEquals(1, individualProperty.get().size(), "Wrong number of values");
        assertEquals(owlLiteral, individualProperty.get().stream().findFirst().get(), "Wrong property literal");

        ontology.writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), false);

    }

    @AfterEach
    public void CloseOntology() {
        ontology.close(true);
    }
}

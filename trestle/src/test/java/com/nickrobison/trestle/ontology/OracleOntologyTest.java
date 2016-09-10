package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.nickrobison.trestle.common.StaticIRI.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 6/24/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization", "Duplicates", "unchecked"})
public class OracleOntologyTest {

    private OracleOntology ontology;
    private OWLDataFactory df;

    @BeforeEach
    public void setupNewOntology() throws OWLOntologyCreationException {
        final IRI iri = IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl");
        df = OWLManager.getOWLDataFactory();

        ontology = (OracleOntology) new OntologyBuilder()
                .withDBConnection(
                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
                        "spatialUser",
                        "spatial1")
                .fromIRI(iri)
                .name("trestle_test3")
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

        final List<QuerySolution> resultSet = ResultSetFormatter.toList(ontology.executeSPARQL(queryString));
        assertEquals(39, resultSet.size(), "Wrong number of classes");

        final long tripleCount = ontology.getTripleCount();
        assertEquals(513, tripleCount, "Inference is wrong");

        final OWLNamedIndividual burundi_0 = df.getOWLNamedIndividual(IRI.create("trestle:", "Burundi_0"));
        final OWLDataProperty property = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));

        Optional<Set<OWLLiteral>> individualProperty = ontology.getIndividualDataProperty(burundi_0, property);
        assertEquals(43, individualProperty.get().stream().findFirst().get().parseInteger(), "ADM0_Code is wrong");

//        Try for wrong individual
        final OWLNamedIndividual burundi_wrong = df.getOWLNamedIndividual(IRI.create("trestle:", "Bwrong_0"));
        final Optional<Set<OWLLiteral>> wrongIndividual = ontology.getIndividualDataProperty(burundi_wrong, property);
        assertFalse(wrongIndividual.isPresent(), "Should be empty optional");

//        Try for wrong property
        final OWLDataProperty property_wrong = df.getOWLDataProperty(IRI.create("trestle:", "ADM1_Code"));
        final Optional<Set<OWLLiteral>> propertyWrong = ontology.getIndividualDataProperty(burundi_0, property_wrong);
        assertFalse(propertyWrong.isPresent(), "Should be empty optional");

//        Try wkt literal
        final OWLNamedIndividual test_maputo = df.getOWLNamedIndividual(IRI.create("trestle:", "test_maputo"));
        final OWLDataProperty asWKT = df.getOWLDataProperty(IRI.create("geosparql:", "asWKT"));
        individualProperty = ontology.getIndividualDataProperty(burundi_0, asWKT);
        final OWLLiteral wktLiteral = individualProperty.get().stream().findFirst().get();
        assertEquals("POINT (30 10)", wktLiteral.getLiteral(), "WKT is wrong");


//        Test object properties
        final OWLObjectProperty has_temporal = df.getOWLObjectProperty(IRI.create("trestle:", "has_temporal"));
        Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(burundi_0, has_temporal);
        assertEquals("Burundi_Valid", individualObjectProperty.get().stream().findFirst().get().getObject().asOWLNamedIndividual().getIRI().getRemainder().get(), "Should be burundi_valid");

//        Try for wrong individual
        individualObjectProperty = ontology.getIndividualObjectProperty(burundi_wrong, has_temporal);
        assertFalse(individualObjectProperty.isPresent(), "Should have no properties");

//        Try for wrong property
        final OWLObjectProperty has_wrong = df.getOWLObjectProperty(IRI.create("trestle:", "has_wrong"));
        individualObjectProperty = ontology.getIndividualObjectProperty(burundi_0, has_wrong);
        assertFalse(individualObjectProperty.isPresent(), "Should have no properties");

//        Try for inferred property
        final OWLNamedIndividual test_muni2 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni2"));
        individualObjectProperty = ontology.getIndividualObjectProperty(test_muni2, has_temporal);
        assertEquals("test_muni1_valid", individualObjectProperty.get().stream().findFirst().get().getObject().asOWLNamedIndividual().getIRI().getRemainder().get(), "Should be test_muni_1_valid");

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

        ontology.writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), false);

    }

    @Test
    public void testByteParsing() throws MissingOntologyEntity {

        int smallInt = 4321;
        int bigInt = Integer.MAX_VALUE;
        int negativeInt = Integer.MIN_VALUE;
        long smallLong = 4321;
        long negativeLong = -4321;
        long negativeBigLong = Long.MIN_VALUE;
        long bigLong = Long.MAX_VALUE;
        Double bigFloat = 4321.43;

        final OWLNamedIndividual long_test = df.getOWLNamedIndividual(IRI.create("trestle:", "long_test"));
        OWLDataProperty aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_small"));
        OWLLiteral owlLiteral = df.getOWLLiteral(Long.toString(smallLong), OWL2Datatype.XSD_LONG);
        final OWLClass owlCl = df.getOWLClass(IRI.create("trestle:", "test"));
        OWLClassAssertionAxiom owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        Optional<Set<OWLLiteral>> individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Big long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "long_big"));
        owlLiteral = df.getOWLLiteral(Long.toString(bigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(bigLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

//        Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_small"));
        owlLiteral = df.getOWLLiteral(Integer.toString(smallInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(smallInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Big Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "int_big"));
        owlLiteral = df.getOWLLiteral(Integer.toString(bigInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(bigInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Negative Int
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_int"));
        owlLiteral = df.getOWLLiteral(Integer.toString(negativeInt), OWL2Datatype.XSD_INTEGER);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Integer.toString(negativeInt), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Double
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "float"));
        owlLiteral = df.getOWLLiteral(Double.toString(bigFloat), OWL2Datatype.XSD_DECIMAL);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Double.toString(bigFloat), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_DECIMAL, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        //        Negative Long
        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(negativeLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
//        Oracle can't tell the difference between an int and a small long, so it treats them as longs.
        assertEquals(OWL2Datatype.XSD_INTEGER, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

        aLong = df.getOWLDataProperty(IRI.create("trestle:", "neg_big_long"));
        owlLiteral = df.getOWLLiteral(Long.toString(negativeBigLong), OWL2Datatype.XSD_LONG);
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(owlCl, long_test);
        ontology.createIndividual(owlClassAssertionAxiom);
        ontology.writeIndividualDataProperty(df.getOWLDataPropertyAssertionAxiom(aLong, long_test, owlLiteral));
        individualDataProperty = ontology.getIndividualDataProperty(long_test, aLong);
        assertEquals(Long.toString(negativeBigLong), individualDataProperty.get().stream().findFirst().get().getLiteral(), "Wrong long value");
        assertEquals(OWL2Datatype.XSD_LONG, individualDataProperty.get().stream().findFirst().get().getDatatype().getBuiltInDatatype(), "Should be long");

    }

    @Test
    public void testRelationAssociation() {

//        Ensure ontology has correct relational classes
        final OWLNamedIndividual muni1_muni2 = df.getOWLNamedIndividual(IRI.create("trestle:", "muni1_muni2"));
        final Set<OWLDataPropertyAssertionAxiom> muniProperties = ontology.getAllDataPropertiesForIndividual(muni1_muni2);
        assertEquals(1, muniProperties.size(), "Wrong number of properties");

        final Set<OWLObjectPropertyAssertionAxiom> allObjectPropertiesForIndividual = ontology.getAllObjectPropertiesForIndividual(muni1_muni2);
        assertEquals(14, allObjectPropertiesForIndividual.size(), "Wrong number of object properties");

//        Check to ensure the relation is transitive and inferred
        final OWLNamedIndividual test_muni4 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni4"));
        final OWLObjectProperty owlObjectProperty = df.getOWLObjectProperty(hasRelationIRI);
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(test_muni4, owlObjectProperty);
        assertTrue(individualObjectProperty.isPresent(), "Should have related_to properties");
        assertEquals(7, individualObjectProperty.get().size(), "Wrong number of related to properties");

        final OWLClass gaulClass = df.getOWLClass(IRI.create("trestle:", "GAUL"));
        final QueryBuilder queryBuilder = new QueryBuilder(ontology.getUnderlyingPrefixManager());
        final String builtString = queryBuilder.buildRelationQuery(test_muni4, gaulClass, 0.6);

        //        Now for the sparql query

        List<QuerySolution> resultSet = ResultSetFormatter.toList(ontology.executeSPARQL(builtString));
        assertEquals(4, resultSet.size(), "Wrong number of relations");

//        If we have the right number of relations, let's build a set of individuals and get their validity intervals;
        final Set<OWLNamedIndividual> individuals = resultSet.stream()
                .map(result -> df.getOWLNamedIndividual(result.getResource("f").getURI()))
                .collect(Collectors.toSet());
        assertEquals(4, individuals.size(), "Should have the same number of individuals");

        Map<OWLNamedIndividual, TemporalObject> intervals = new HashMap<>();
        individuals.forEach(individual -> {
            final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty1 = ontology.getIndividualObjectProperty(individual, hasTemporalIRI);
            if (individualObjectProperty1.isPresent()) {
                final Optional<Set<OWLLiteral>> validFrom = ontology.getIndividualDataProperty(individualObjectProperty1.get().stream().findFirst().get().getObject().asOWLNamedIndividual(), temporalValidFromIRI);
                final Optional<Set<OWLLiteral>> validTo = ontology.getIndividualDataProperty(individualObjectProperty1.get().stream().findFirst().get().getObject().asOWLNamedIndividual(), temporalValidToIRI);
                final IntervalTemporal intervalTemporal = TemporalObjectBuilder.valid().from(LocalDateTime.parse(validFrom.get().stream().findFirst().get().getLiteral()))
                        .to(LocalDateTime.parse(validTo.get().stream().findFirst().get().getLiteral()))
                        .withRelations(individual);
                intervals.put(individual, intervalTemporal);
            }
        });

        assertEquals(4, intervals.size(), "Should have 4 intervals");


    }

    @AfterEach
    public void CloseOntology() {
        ontology.close(true);
    }


}

import com.hp.hpl.jena.query.ResultSet;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.ontology.OracleOntology;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by nrobison on 6/24/16.
 */
public class OracleTrestleTest {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void simpleTest() throws OWLOntologyCreationException, SQLException {

        final IRI iri = IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl");

        final OracleOntology ontology = (OracleOntology) new OntologyBuilder()
                .withDBConnection(
                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
                        "spatialUser",
                        "spatial1")
                .fromIRI(iri)
                .name("trestle")
                .build().get();
//        ontology.initializeOntology();

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#> " +
                "SELECT * WHERE {?m rdf:type ?type . ?type rdfs:subClassOf ?class}";
//        String queryString = " SELECT ?subject ?prop ?object WHERE { ?subject ?prop ?object } ";

        final ResultSet resultSet = ontology.executeSPARQL(queryString);
        assertEquals("Wrong number of classes", 29, resultSet.getRowNumber());

        final long tripleCount = ontology.getTripleCount();
        assertEquals("Inference is wrong", 381, tripleCount);

        final OWLDataFactory df = OWLManager.getOWLDataFactory();
        final OWLNamedIndividual burundi_0 = df.getOWLNamedIndividual(IRI.create("trestle:", "Burundi_0"));
        final OWLDataProperty property = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));

        Optional<Set<OWLLiteral>> individualProperty = ontology.getIndividualProperty(burundi_0, property);
        assertEquals("ADM0_Code is wrong", 43, individualProperty.get().stream().findFirst().get().parseInteger());

//        Try for wrong individual
        final OWLNamedIndividual burundi_wrong = df.getOWLNamedIndividual(IRI.create("trestle:", "Bwrong_0"));
        final Optional<Set<OWLLiteral>> wrongIndividual = ontology.getIndividualProperty(burundi_wrong, property);
        assertFalse("Should be empty optional", wrongIndividual.isPresent());

//        Try for wrong property
        final OWLDataProperty property_wrong = df.getOWLDataProperty(IRI.create("trestle:", "ADM1_Code"));
        final Optional<Set<OWLLiteral>> propertyWrong = ontology.getIndividualProperty(burundi_0, property_wrong);
        assertFalse("Should be empty optional", propertyWrong.isPresent());

//        Try wkt literal
        final OWLNamedIndividual test_maputo = df.getOWLNamedIndividual(IRI.create("trestle:", "test_maputo"));
        final OWLDataProperty asWKT = df.getOWLDataProperty(IRI.create("geosparql:", "asWKT"));
        individualProperty = ontology.getIndividualProperty(burundi_0, asWKT);
        final OWLLiteral wktLiteral = individualProperty.get().stream().findFirst().get();
        assertEquals("WKT is wrong", "POINT (30 10)", wktLiteral.getLiteral());


//        Test object properties
        final OWLObjectProperty has_temporal = df.getOWLObjectProperty(IRI.create("trestle:", "has_temporal"));
        Optional<Set<OWLObjectProperty>> individualObjectProperty = ontology.getIndividualObjectProperty(burundi_0, has_temporal);
        assertEquals("Should be burundi_valid", "Burundi_Valid", individualObjectProperty.get().stream().findFirst().get().getIRI().getRemainder().get());

//        Try for wrong individual
        individualObjectProperty = ontology.getIndividualObjectProperty(burundi_wrong, has_temporal);
        assertFalse("Should have no properties", individualObjectProperty.isPresent());

//        Try for wrong property
        final OWLObjectProperty has_wrong = df.getOWLObjectProperty(IRI.create("trestle:", "has_wrong"));
        individualObjectProperty = ontology.getIndividualObjectProperty(burundi_0, has_wrong);
        assertFalse("Should have no properties", individualObjectProperty.isPresent());

//        Try for inferred property
        final OWLNamedIndividual test_muni2 = df.getOWLNamedIndividual(IRI.create("trestle:", "test_muni2"));
        individualObjectProperty = ontology.getIndividualObjectProperty(test_muni2, has_temporal);
        assertEquals("Should be test_muni_1_valid", "test_muni1_valid", individualObjectProperty.get().stream().findFirst().get().getIRI().getRemainder().get());


        ontology.close(false);
    }


}

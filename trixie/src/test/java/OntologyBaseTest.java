import com.hp.hpl.jena.query.ResultSet;
import com.nickrobison.trixie.ontology.ITrixieOntology;
import com.nickrobison.trixie.ontology.OntologyBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.*;

/**
 * Created by nrobison on 6/3/16.
 */
@RunWith(Parameterized.class)
public class OntologyBaseTest {

    private ITrixieOntology ontology;
    private OWLDataFactory df;


    public OntologyBaseTest(ITrixieOntology ontology) {
        this.ontology = ontology;
        df = OWLManager.getOWLDataFactory();

    }

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        URL ontologyResource = OntologyBaseTest.class.getClassLoader().getResource("main_geo.owl");
        final IRI iri = IRI.create(ontologyResource);

//        Build ontologies
//        Local Ontology
        Optional<ITrixieOntology> localOntology = Optional.empty();
        try {
//            localOntology = LocalOntology
//                    .from(iri)
//                    .build();
            localOntology = new OntologyBuilder()
                    .fromIRI(iri)
//                    .name("Test ontology")
                    .build();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

//        Oracle Ontology
//        Optional<ITrixieOntology> oracleOntology = Optional.empty();
//        try {
//            oracleOntology = OracleOntology.withDBConnection(iri,
//                    "jdbc:oracle:thin:@oracle:1521:spatial",
//                    "spatial",
//                    "spatialUser")
//                    .build();
//        } catch (OWLOntologyCreationException e) {
//            e.printStackTrace();
//        }

        return Arrays.asList(new Object[][]{
                {localOntology.orElseThrow(NullPointerException::new)}
//                {oracleOntology.orElseThrow(NullPointerException::new)}
        });
    }


    @Test
    public void testBaseIndividuals() {

        final OWLClass crsClass = df.getOWLClass(IRI.create("main_geo:", "CRS").toString(), ontology.getUnderlyingPrefixManager());
        final OWLClass geographicClass = df.getOWLClass(IRI.create("main_geo:", "GeographicCRS").toString(), ontology.getUnderlyingPrefixManager());
        final OWLClass projectedClass = df.getOWLClass(IRI.create("main_geo:", "ProjectedCRS").toString(), ontology.getUnderlyingPrefixManager());

        final Set<OWLNamedIndividual> instances = ontology.getInstances(crsClass, false);
        assertEquals("Should 2", 2, instances.size());

        final Set<OWLNamedIndividual> instances1 = ontology.getInstances(geographicClass, false);
        assertEquals("Should be 1", 1, instances1.size());

        ontology.getInstances(projectedClass, true);
        assertEquals("Should be 1", 1, instances1.size());

    }

    @Test
    public void testIndividualGet() {
//        Query for specific members

        final OWLNamedIndividual wgs_84 = df.getOWLNamedIndividual(ontology.getFullIRI("main_geo:", "WGS_84"));
        final Optional<OWLNamedIndividual> individual = ontology.getIndividual(wgs_84);
        assertTrue("Missing individual", individual.isPresent());

        final OWLNamedIndividual wgs_84_2 = df.getOWLNamedIndividual(ontology.getFullIRI("main_geo:", "WGS_84_2"));
        final Optional<OWLNamedIndividual> individual2 = ontology.getIndividual(wgs_84_2);
        assertFalse("Shouldn't return missing individual", individual2.isPresent());
    }

    //    Load and query

    // Base Load tests
//    TODO(nrobison): This fails on local ontologies
    @Test
    public void testBaseSPARQLQuery() {

        //        Try to read the base individuals back from the database
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX : <http://nickrobison.com/dissertation/main_geo.owl#> " +
                "SELECT * WHERE {?m rdf:type ?type . ?type rdfs:subClassOf ?class}";
        final ResultSet rs = ontology.executeSPARQL(queryString);
        assertEquals("Incorrect number of class results", 44, rs.getRowNumber());


        //        Try to read out one of the CRS individuals

        final OWLNamedIndividual wgs_84 = df.getOWLNamedIndividual(IRI.create("main_geo:", "WGS_84"));
        final Optional<OWLNamedIndividual> baseIndividual = ontology.getIndividual(wgs_84);
        assertTrue("Base CRS should exist", baseIndividual.isPresent());
    }

    @Test
    public void testOntologyLoading() {
//        ontology.initializeOracleOntology();
        ontology.initializeOntology(true);

//        Try to read the base individuals back from the database
        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT * WHERE {?m rdf:type ?type . ?type rdfs:subClassOf ?class}";
        final ResultSet rs = ontology.executeSPARQL(queryString);
        assertEquals("Incorrect number of class results", 17081, rs.getRowNumber());

//        Try to query from the ontology
        final OWLClass crsClass = df.getOWLClass(ontology.getFullIRI(IRI.create("main_geo:", "CRS")));
        final Set<OWLNamedIndividual> instances = ontology.getInstances(crsClass, false);
        assertTrue("Should be more than 2 CRS individuals", instances.size() > 2);


//        Try to read out one of the CRS individuals

        final OWLNamedIndividual wgs_84 = df.getOWLNamedIndividual(IRI.create("main_geo:", "WGS_84"));
        final Optional<OWLNamedIndividual> baseIndividual = ontology.getIndividual(wgs_84);
        assertTrue("Base CRS should exist", baseIndividual.isPresent());

        final OWLNamedIndividual newCRS = df.getOWLNamedIndividual(IRI.create("main_geo:", "4326"));
        final Optional<OWLNamedIndividual> newIndividual = ontology.getIndividual(newCRS);
        assertTrue("New CRS should exist", newIndividual.isPresent());

//        Write out the ontology
        try {
            ontology.writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), true);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }

//    @Af
/*    public void writeOntology() throws OWLOntologyStorageException {
//        optionOntology.get().close();
        ontology.close();

//        optionOntology.get().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), true);
    }*/
}

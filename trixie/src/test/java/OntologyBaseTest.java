import com.hp.hpl.jena.query.ResultSet;
import com.nickrobison.trixie.ontology.ITrixieOntology;
import com.nickrobison.trixie.ontology.LocalOntology;
import com.nickrobison.trixie.ontology.OracleOntology;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

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
        Optional<ITrixieOntology> localOntology = Optional.empty();
        try {
             localOntology = LocalOntology
                    .from(iri)
                    .build();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        return Arrays.asList(new Object[][] {
            {localOntology.orElseThrow(NullPointerException::new)}
        });
    }

//    @Before
//    public void setUp() {
//        df = OWLManager.getOWLDataFactory();
////        URL ontologyResource = OntologyBaseTest.class.getClassLoader().getResource("main_geo.owl");
////        assertNotNull("OWL File Missing", ontologyResource);
//
////        final IRI iri = IRI.create();
////        optionOntology = createInstance();
//        ontology = createInstance();
////        optionOntology = OracleOntology.withDBConnection(iri,
////                "jdbc:oracle:thin:@oracle:1521:spatial",
////                "spatial",
////                "spatialUser")
////                .build();
//    }


    @Test
    public void testBaseEPSGCodes() {
//        ITrixieOntology ontology = optionOntology.get();

        final OWLClass crsClass = df.getOWLClass(IRI.create("main_geo:", "CRS").toString(), ontology.getUnderlyingPrefixManager());
        final OWLClass geographicClass = df.getOWLClass(IRI.create("main_geo:", "GeographicCRS").toString(), ontology.getUnderlyingPrefixManager());
        final OWLClass projectedClass = df.getOWLClass(IRI.create("main_geo:", "ProjectedCRS").toString(), ontology.getUnderlyingPrefixManager());

//        ontology.initializeOntology(false);

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
//        final ITrixieOntology ontology = optionOntology.get();

        final OWLNamedIndividual wgs_84 = df.getOWLNamedIndividual(ontology.getFullIRI("main_geo:", "WGS_84"));
        final Optional<OWLNamedIndividual> individual = ontology.getIndividual(wgs_84);
        assertTrue("Missing individual", individual.isPresent());

        final OWLNamedIndividual wgs_84_2 = df.getOWLNamedIndividual(ontology.getFullIRI("main_geo:", "WGS_84_2"));
        final Optional<OWLNamedIndividual> individual2 = ontology.getIndividual(wgs_84_2);
        assertFalse("Shouldn't return missing individual", individual2.isPresent());
    }

    //    Load and query
//    @Test
//    public void testBaseEPSGLoading() {
////    ITrixieOntology ontology = optionOntology.get();
//
//        final OWLClass crsClass = df.getOWLClass(IRI.create("main_geo:", "CRS").toString(), ontology.getUnderlyingPrefixManager());
//
//        ontology.initializeOntology(false);
//
//        final Set<OWLNamedIndividual> instances = ontology.getInstances(crsClass, true);
//        assertTrue("Should more than 2", instances.size() > 2);
//
////    Test individual loaded get
//    }

    // Oracle tests
//    @Test
//    public void testOracleBaseOntology() {
////        ITrixieOntology ontology = optionOntology.get();
//
//        ontology.initializeOracleOntology();
//
//        //        Try to read the base individuals back from the database
//        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
//                "SELECT * WHERE {?m rdf:type ?type . ?type rdfs:subClassOf ?class}";
//        final ResultSet rs = ontology.executeSPARQL(queryString);
//        assertEquals("Incorrect number of class results", 44, rs.getRowNumber());
//
//
//        //        Try to read out one of the CRS individuals
//
//        final OWLNamedIndividual wgs_84 = df.getOWLNamedIndividual(IRI.create("main_geo:", "WGS_84"));
//        final Optional<OWLNamedIndividual> baseIndividual = ontology.getIndividual(wgs_84);
//        assertTrue("Base CRS should exist", baseIndividual.isPresent());
//    }
//
//    @Test
//    public void testOracleLoading() {
////        ITrixieOntology ontology = optionOntology.get();
//
////        TODO(nrobison): Add some test individuals to save.
//
////        FIXME(nrobison): Seems like the WKT Strings are throwing errors.
////        ontology.initializeOracleOntology();
//        ontology.initializeOntology(true);
//
////        Try to read the base individuals back from the database
//        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
//                "SELECT * WHERE {?m rdf:type ?type . ?type rdfs:subClassOf ?class}";
//        final ResultSet rs = ontology.executeSPARQL(queryString);
//        assertEquals("Incorrect number of class results", 17081, rs.getRowNumber());
//
////        Try to query from the ontology
//        final OWLClass crsClass = df.getOWLClass(ontology.getFullIRI(IRI.create("main_geo:", "CRS")));
//        final Set<OWLNamedIndividual> instances = ontology.getInstances(crsClass, true);
//        assertTrue("Should be more than 2 CRS individuals", instances.size() > 2);
//
//
////        Try to read out one of the CRS individuals
//
//        final OWLNamedIndividual wgs_84 = df.getOWLNamedIndividual(IRI.create("main_geo:", "WGS_84"));
//        final Optional<OWLNamedIndividual> baseIndividual = ontology.getIndividual(wgs_84);
//        assertTrue("Base CRS should exist", baseIndividual.isPresent());
//
//        final OWLNamedIndividual newCRS = df.getOWLNamedIndividual(IRI.create("main_geo:", "4326"));
//        final Optional<OWLNamedIndividual> newIndividual = ontology.getIndividual(newCRS);
//        assertTrue("New CRS should exist", newIndividual.isPresent());
//    }

//    @Af
/*    public void writeOntology() throws OWLOntologyStorageException {
//        optionOntology.get().close();
        ontology.close();

//        optionOntology.get().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), true);
    }*/
}

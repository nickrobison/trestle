import com.hp.hpl.jena.query.ResultSet;
import com.nickrobison.trixie.ontology.ITrixieOntology;
import com.nickrobison.trixie.ontology.OracleOntology;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Created by nrobison on 6/3/16.
 */
public class OntologySetup {

    private static URL ontologyResource;
    private Optional<ITrixieOntology> optionOntology;
    private static OWLDataFactory df;

    @BeforeClass
    public static void init() {
        df = OWLManager.getOWLDataFactory();
        ontologyResource = OntologySetup.class.getClassLoader().getResource("main_geo.owl");
    }


    @Test
    @Before
    public void testOntologyInit() throws OWLOntologyCreationException {
        assertNotNull("OracleOntology Missing", ontologyResource);

        final IRI iri = IRI.create(ontologyResource);
        optionOntology = OracleOntology.withDBConnection(iri,
                "jdbc:oracle:thin:@oracle:1521:spatial",
                "spatial",
                "spatialUser")
                .build();
        assertNotNull("No optionalOntology returned", optionOntology.isPresent());
    }

    @Test
    public void testEPSGLoading() {
        ITrixieOntology ontology = optionOntology.get();

        final OWLClass crsClass = df.getOWLClass(IRI.create("main_geo:", "CRS").toString(), ontology.getUnderlyingPrefixManager());

        ontology.initializeOntology(false);

        final Set<OWLNamedIndividual> instances = ontology.getInstances(crsClass);
        assertEquals("Should 2", 2, instances.size());
    }

    @Test
    public void testOracleLoading() {
        ITrixieOntology ontology = optionOntology.get();

//        TODO(nrobison): Add some test individuals to save.

//        FIXME(nrobison): Seems like the WKT Strings are throwing errors.
        ontology.initializeOracleOntology();

//        Try to read the base individuals back from the database
//        String queryString = "SELECT * WHERE {?m <rdf:type> ?type . ?type <rdfs:subClassOf> ?class}";
        String queryString = " SELECT ?subject ?prop ?object WHERE { ?subject ?prop ?object } ";
        final ResultSet rs = ontology.executeSPARQL(queryString);

        assertEquals("Incorrect number of results", 286, rs.getRowNumber());

    }

    @After
    public void writeOntology() throws OWLOntologyStorageException {
        optionOntology.get().close();

//        optionOntology.get().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), true);
    }
}

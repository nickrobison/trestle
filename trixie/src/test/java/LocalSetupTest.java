import com.nickrobison.trixie.ontology.ITrixieOntology;
import com.nickrobison.trixie.ontology.OracleOntology;
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
import static junit.framework.TestCase.assertTrue;

/**
 * Created by nrobison on 6/15/16.
 */
@SuppressWarnings("Duplicates")
public class LocalSetupTest {

    private static URL ontologyResource;
    private Optional<ITrixieOntology> optionOntology;
    private static OWLDataFactory df;

    @BeforeClass
    public static void init() {
        df = OWLManager.getOWLDataFactory();
        ontologyResource = LocalSetupTest.class.getClassLoader().getResource("main_geo.owl");
    }

    @Before
    public void testOntologyInit() throws OWLOntologyCreationException {
        assertNotNull("OWL File Missing", ontologyResource);

        final IRI iri = IRI.create(ontologyResource);
        optionOntology = OracleOntology.withDBConnection(iri,
                "jdbc:oracle:thin:@oracle:1521:spatial",
                "spatial",
                "spatialUser")
                .build();
        assertTrue("No optionalOntology returned", optionOntology.isPresent());
    }

    @Test
    public void testBaseEPSGCodes() {
        ITrixieOntology ontology = optionOntology.get();

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
}

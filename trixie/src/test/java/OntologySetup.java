import com.nickrobison.trixie.ontology.IOntology;
import com.nickrobison.trixie.ontology.Ontology;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by nrobison on 6/3/16.
 */
public class OntologySetup {

    private static URL ontologyResource;
    private Optional<IOntology> optionOntology;
    private static OWLDataFactory df;

    @BeforeClass
    public static void init() {
        df = OWLManager.getOWLDataFactory();
        ontologyResource = OntologySetup.class.getClassLoader().getResource("main_geo.owl");
    }


    @Test
    @Before
    public void testOntologyInit() throws OWLOntologyCreationException {
        assertNotNull("Ontology Missing", ontologyResource);

        final IRI iri = IRI.create(ontologyResource);
        optionOntology = Ontology.from(iri).build();
        assertNotNull("No optionOntology returned", optionOntology.isPresent());
    }

    @Test
    public void testEPSGLoading() {
        IOntology ontology = optionOntology.get();
        final OWLClass crsClass = df.getOWLClass(IRI.create("main_geo:", "CRS").toString(), ontology.getUnderlyingPrefixManager());


        ontology.initializeOntology(false);

        final Set<OWLNamedIndividual> instances = ontology.getInstances(crsClass);
        assertTrue("Should be more than 2", instances.size() > 2);
    }

    @After
    public void writeOntology() throws OWLOntologyStorageException {

        optionOntology.get().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), true);
    }
}

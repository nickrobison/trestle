package com.nickrobison.trestle.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelGetter;
import com.hp.hpl.jena.rdf.model.ModelReader;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.query.spatial.EntityDefinition;
import org.apache.jena.query.spatial.SpatialDatasetFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 6/15/16.
 */
// TODO(nrobison): Make this actually work
//    DO NOT USE!!!!! TOTALLY BROKEN, NOT A SINGLE THING WORKS
public class LocalOntology implements ITrestleOntology {

    private final String ontologyName;
    private final static Logger logger = LoggerFactory.getLogger(LocalOntology.class);
    private final OWLOntology ontology;
    private final PelletReasoner reasoner;
    private final DefaultPrefixManager pm;
    private final OntModel model;
    private final Dataset ds;


    LocalOntology(String ontologyName, OWLOntology ont, DefaultPrefixManager pm, PelletReasoner reasoner) {
        this.ontologyName = ontologyName;
        ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
//        Instead of a database object, we use a Jena model to support the RDF querying
        this.ds = initialiseTDB();


//        spatial stuff
        Directory indexDirectory;
        Dataset spatialDataset = null;
        try {
            indexDirectory = FSDirectory.open(new File("./target/data/lucene/"));
//            Not sure if these entity and geo fields are correct, but oh well.
            EntityDefinition ed = new EntityDefinition("uri", "geo");
//            Create a spatial dataset that combines the TDB dataset + the spatial index
             spatialDataset = SpatialDatasetFactory.createLucene(this.ds, indexDirectory, ed);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final OntModelSpec spec = PelletReasonerFactory.THE_SPEC;
        spec.setImportModelGetter(new LocalTDBModelGetter(spatialDataset));
        this.model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
        this.model.read(ontologyToIS(), null);
        TDB.sync(this.model);

    }

    private Dataset initialiseTDB() {
        final String tdbPath = "target/data/tdb";
        new File(tdbPath).mkdirs();

        return TDBFactory.createDataset(tdbPath);
    }

    public boolean isConsistent() {
        return reasoner.isConsistent();
    }

    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {

    }

    public void applyChange(OWLAxiomChange... axiom) {
        applyChanges(axiom);
    }

    private void applyChanges(OWLAxiomChange... axioms) {
        ontology.getOWLOntologyManager().applyChanges(Arrays.asList(axioms));
    }

    public void close() {
        reasoner.dispose();
    }

    public OWLOntology getUnderlyingOntology() {
        return this.ontology;
    }

    public DefaultPrefixManager getUnderlyingPrefixManager() {
        return this.pm;
    }

    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass, boolean direct) {
        return reasoner.getInstances(owlClass, direct).getFlattened();
    }

//    TODO(nrobison): Does this actually work on a local ontology?
    public Optional<OWLNamedIndividual> getIndividual(OWLNamedIndividual individual) {
        final Set<OWLNamedIndividual> entities = reasoner.getSameIndividuals(individual).getEntities();
        if (entities.contains(individual)) {
            return Optional.of(individual);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Set<OWLLiteral>> getIndividualProperty(OWLNamedIndividual individual, OWLDataProperty property) {
        final Set<OWLLiteral> dataPropertyValues = reasoner.getDataPropertyValues(individual, property);
        if (dataPropertyValues.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(dataPropertyValues);
        }
    }

    public IRI getFullIRI(IRI iri) {
        return pm.getIRI(iri.toString());
    }

    public IRI getFullIRI(String prefix, String suffix) {
        return getFullIRI(IRI.create(prefix, suffix));
    }

//    oracle boolean has no effect here, since it's a local ontology
    public void initializeOntology() {

//        TODO(nrobison): No need for EPSG codes right now.
//        logger.debug("Parsing and loading EPSG codes");
//        //        Parse and apply the EPSG codes to the ontology
//        final List<AddAxiom> owlAxiomChanges = EPSGParser.parseEPSGCodes(this.ontology, this.pm);
//        applyChanges((OWLAxiomChange[]) owlAxiomChanges.toArray());

//        TODO(nrobison): Need to write this to the Jena model.
    }

    public ResultSet executeSPARQL(String queryString) {
        final Query query = QueryFactory.create(queryString);
        final QueryExecution qExec = SparqlDLExecutionFactory.create(query, this.model);
        final ResultSet resultSet = qExec.execSelect();
        qExec.close();
        ResultSetFormatter.out(System.out, resultSet, query);

        return resultSet;
    }

    private ByteArrayInputStream ontologyToIS() {
        //        We need to read out the ontology into a bytestream and then read it back into the oracle format
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
//            Jena doesn't support OWL/XML, so we need base RDF.
            ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        } catch (OWLOntologyStorageException e) {
            throw new RuntimeException("Cannot save ontology to bytearray", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

//    This comes from an online gist, not sure if it's really necessary or not, but it seems to work
//    https://gist.github.com/ijdickinson/3830267
    static class LocalTDBModelGetter implements ModelGetter {

        private final Dataset ds;

        LocalTDBModelGetter(Dataset dataset) {
            this.ds = dataset;
        }

        @Override
        public Model getModel(String URL) {
            throw new NotImplemented("getModel( String ) is no implemented");
        }

        @Override
        public Model getModel(String URL, ModelReader loadIfAbsent) {
            Model m = ds.getNamedModel(URL);

            if (m == null) {
                m = ModelFactory.createDefaultModel();
                loadIfAbsent.readModel(m, URL);
                ds.addNamedModel(URL, m);
            }

            return m;
        }
    }
}

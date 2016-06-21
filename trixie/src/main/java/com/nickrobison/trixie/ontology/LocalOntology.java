package com.nickrobison.trixie.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.nickrobison.trixie.common.EPSGParser;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 6/15/16.
 */
// TODO(nrobison): Make this actually work
public class LocalOntology implements ITrixieOntology {

    private final String ontologyName;
    private final static Logger logger = LoggerFactory.getLogger(LocalOntology.class);
    private final OWLOntology ontology;
    private final PelletReasoner reasoner;
    private final DefaultPrefixManager pm;
    private final OntModel model;


    LocalOntology(String ontologyName, OWLOntology ont, DefaultPrefixManager pm, PelletReasoner reasoner) {
        this.ontologyName = ontologyName;
        ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
//        Instead of a database object, we use a Jena model to support the RDF querying
        this.model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
        this.model.read(ontologyToIS(), null);

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

    public IRI getFullIRI(IRI iri) {
        return pm.getIRI(iri.toString());
    }

    public IRI getFullIRI(String prefix, String suffix) {
        return getFullIRI(IRI.create(prefix, suffix));
    }

//    oracle boolean has no effect here, since it's a local ontology
    public void initializeOntology(boolean oracle) {

//        TODO(nrobison): No need for EPSG codes right now.
//        logger.debug("Parsing and loading EPSG codes");
//        //        Parse and apply the EPSG codes to the ontology
//        final List<AddAxiom> owlAxiomChanges = EPSGParser.parseEPSGCodes(this.ontology, this.pm);
//        applyChanges((OWLAxiomChange[]) owlAxiomChanges.toArray());

//        TODO(nrobison): Need to write this to the Jena model.
    }

    public void initializeOracleOntology(IRI filename) {

    }

    public void initializeOracleOntology() {

    }

    public ResultSet executeSPARQL(String query) {
        final Query q = QueryFactory.create(query);
        final QueryExecution qe = SparqlDLExecutionFactory.create(q, this.model);

        return qe.execSelect();
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
}

package com.nickrobison.trixie.ontology;

import com.nickrobison.trixie.db.oracle.OracleDatabase;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by nrobison on 5/23/16.
 */
public class Ontology implements IOntology {

    private final static Logger log = Logger.getLogger(Ontology.class);
    private final OWLOntology ontology;
    private final FaCTPlusPlusReasoner reasoner;
    private final DefaultPrefixManager pm;

    Ontology(OWLOntology ont, DefaultPrefixManager pm, FaCTPlusPlusReasoner reasoner) {
        this.ontology = ont;
        this.pm = pm;
        this.reasoner = reasoner;
    }

    /**
     *
     * @param iri - IRI of the Ontology to load
     * @return - new Builder to instantiate ontology
     */
    public static Builder from(IRI iri) {
        return new Builder(iri);
    }

    /**
     *
     * @return - Returns the raw underlying ontology
     */
    public OWLOntology getUnderlyingOntology() { return this.ontology;}

    /**
     *
     * @return - Returns the raw underlying prefix manager
     */
    public DefaultPrefixManager getUnderlyingPrefixManager() { return this.pm;}

    public void applyChange(OWLAxiomChange ... axiom) {
        applyChanges(axiom);
    }

    private void applyChanges(OWLAxiomChange ... axioms) {
        ontology.getOWLOntologyManager().applyChanges(Arrays.asList(axioms));
    }

    public boolean isConsistent() {
        return reasoner.isConsistent();
    }

    public Set<OWLNamedIndividual> getInstances(OWLClass owlClass) {
        return reasoner.getInstances(owlClass, true).getFlattened();
    }

    public void initializeOracleOntology(IRI filename) {
        OracleDatabase oraDB;
        try {
            oraDB = new OracleDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Problem with Oracle", e);
        }

        oraDB.loadOntology(filename.toString());
    }

    /**
     *
     * @param path - IRI of location to write ontology
     * @param validate - boolean validate ontology before writing
     * @throws OWLOntologyStorageException
     */
    public void writeOntology(IRI path, boolean validate) throws OWLOntologyStorageException {
        if (validate) {
            if (!isConsistent()) {
                throw new RuntimeException("Ontology is invalid");
            }
        }
        ontology.getOWLOntologyManager().saveOntology(ontology, new OWLXMLDocumentFormat(), path);
    }
}

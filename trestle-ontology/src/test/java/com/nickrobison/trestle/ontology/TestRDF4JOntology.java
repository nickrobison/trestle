package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import org.eclipse.rdf4j.repository.Repository;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

/**
 * Created by nickrobison on 4/19/20.
 */
public class TestRDF4JOntology extends RDF4JOntology {


    protected TestRDF4JOntology(String ontologyName, Repository repository, OWLOntology ontology, DefaultPrefixManager pm) {
        super(ontologyName, repository, ontology, pm);
    }

    @Override
    protected void closeDatabase(boolean drop) {
        // Not used
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        // Not used
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        // Not used
    }

    @Override
    public void abortDatasetTransaction(boolean write) {
        // Not used
    }

    @Override
    public boolean isConsistent() {
        return false;
    }

    @Override
    public void runInference() {
        // Not used
    }

    @Override
    public void initializeOntology() {
        // Not used
    }

    @Override
    public TrestleResultSet executeSPARQLResults(String queryString) {
        return null;
    }

    @Override
    public void executeUpdateSPARQL(String queryString) {

    }
}

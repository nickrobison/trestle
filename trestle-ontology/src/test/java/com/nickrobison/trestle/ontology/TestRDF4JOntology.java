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

    }

    @Override
    public void openDatasetTransaction(boolean write) {

    }

    @Override
    public void commitDatasetTransaction(boolean write) {

    }

    @Override
    public void abortDatasetTransaction(boolean write) {

    }

    @Override
    public boolean isConsistent() {
        return false;
    }

    @Override
    public void runInference() {

    }

    @Override
    public void initializeOntology() {

    }

    @Override
    public TrestleResultSet executeSPARQLResults(String queryString) {
        return null;
    }

    @Override
    public void executeUpdateSPARQL(String queryString) {

    }
}

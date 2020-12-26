package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResult;
import com.nickrobison.trestle.ontology.utils.RDF4JLiteralFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.eclipse.rdf4j.repository.Repository;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

/**
 * Created by nickrobison on 4/19/20.
 */
public class TestRDF4JOntology extends RDF4JOntology {


    protected TestRDF4JOntology(String ontologyName, Repository repository, OWLOntology ontology, DefaultPrefixManager pm, RDF4JLiteralFactory factory) {
        super(ontologyName, repository, ontology, pm, factory);
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
    public void initializeOntology() {
        // Not used
    }

    @Override
    public Flowable<TrestleResult> executeSPARQLResults(String queryString) {
        return null;
    }

    @Override
    public Completable executeUpdateSPARQL(String queryString) {
        return null;
    }
}

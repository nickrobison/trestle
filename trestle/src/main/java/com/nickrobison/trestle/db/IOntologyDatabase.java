package com.nickrobison.trestle.db;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.IRI;

import java.io.InputStream;

/**
 * Created by nrobison on 6/1/16.
 */
public interface IOntologyDatabase {

    void disconnect();

    void loadBaseOntology(String filename);

    void loadBaseOntology(InputStream is);

    void exportBaseOntology(String filename);

    void enableBulkLoading();

    void rebuildIndexes();

    Resource getIndividual(IRI iri);

    void writeTuple(String subject, String predicate, String object);

    ResultSet executeRawSPARQL(String queryString);
}

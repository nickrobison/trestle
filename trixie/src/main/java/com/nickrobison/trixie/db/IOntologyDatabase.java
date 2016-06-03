package com.nickrobison.trixie.db;

/**
 * Created by nrobison on 6/1/16.
 */
public interface IOntologyDatabase {

    void loadBaseOntology(String filename);

    void exportBaseOntology(String filename);

    void enableBulkLoading();

    void rebuildIndexes();

    void writeTuple(String subject, String predicate, String object);
}

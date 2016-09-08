package com.nickrobison.trestle.ontology;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.shared.Lock;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by nrobison on 7/22/16.
 */
@SuppressWarnings({"initialization"})
@ThreadSafe
public class VirtuosoOntology extends JenaOntology {

    private static final Logger logger = LoggerFactory.getLogger(VirtuosoOntology.class);
    private static VirtModel virtModel;

    VirtuosoOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        super(name, initializeVirtModel(name, connectionString, username, password), ont, pm);
    }

    private static Model initializeVirtModel(String name, String connectionString, String username, String password) {
        virtModel = VirtModel.openDatabaseModel(name, connectionString, username, password);
        return ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), virtModel);
    }

    @Override
    public boolean isConsistent() {
        return true;
    }

    @Override
    public void initializeOntology() {
        logger.info("Dropping model {}", this.ontologyName);
        if (!virtModel.isEmpty()) {
            virtModel.removeAll();
        }

        logger.info("Writing new ontology");

        try {
            this.model.read(ontologytoIS(this.ontology), null);
        } catch (OWLOntologyStorageException e) {
            logger.error("Cannot read ontology", e);
            throw new RuntimeException("Cannot read ontology", e);
        }
        logger.debug("Finished writing ontology");
    }

    @Override
//    Need to override the SPARQL command because the geospatial extensions will cause Jena to fail the query parsing.
//    TODO(nrobison): This should return a list, not this weird ResultSet thing.
    public ResultSet executeSPARQL(String queryString) {
        ResultSet resultSet;
        final VirtuosoQueryExecution queryExecution = VirtuosoQueryExecutionFactory.create(queryString, (VirtGraph) this.virtModel.getGraph());
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            resultSet = queryExecution.execSelect();
            resultSet = ResultSetFactory.copyResults(resultSet);
        } finally {
            queryExecution.close();
            this.model.leaveCriticalSection();
            this.commitTransaction();
        }

        return resultSet;
    }

    @Override
    public void close(boolean drop) {
        if (drop) {
            logger.info("Dropping model {}", this.ontologyName);
            virtModel.removeAll();
        }
        logger.info("Closing model", this.ontologyName);
        this.model.close();
        virtModel.close();
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        virtModel.begin();
    }

    @Override
    public void commitDatasetTransaction() {
        virtModel.commit();
    }

    protected static ByteArrayInputStream ontologytoIS(OWLOntology ontology) throws OWLOntologyStorageException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}

package com.nickrobison.trestle.ontology;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by nrobison on 7/22/16.
 */
@SuppressWarnings({"initialization"})
public class VirtuosoOntology extends JenaOntology {

    private static final Logger logger = LoggerFactory.getLogger(VirtuosoOntology.class);
    private static VirtModel virtModel;
    private boolean locked = false;

    VirtuosoOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        super(name, initializeVirtModel(name, connectionString, username, password), ont, pm);
    }

    private static Model initializeVirtModel(String name, String connectionString, String username, String password) {
        virtModel = VirtModel.openDatabaseModel(name, connectionString, username, password);
//        return ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), virtModel);
        return ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), virtModel);
//        return ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF, model);
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
//        if (!model.isEmpty()) {
//            model.removeAll();
//        }

        logger.info("Writing new ontology");

        try {
            virtModel.read(ontologytoIS(this.ontology), null);
        } catch (OWLOntologyStorageException e) {
            logger.error("Cannot read ontology", e);
            throw new RuntimeException("Cannot read ontology", e);
        }
        logger.debug("Finished writing ontology");
        ((InfModel) this.model).getReasoner().bindSchema(virtModel);
//        ((InfModel) this.model).rebind();
//        ((InfModel) this.model).prepare();
    }

    @Override
//    Need to override the SPARQL command because the geospatial extensions will cause Jena to fail the query parsing.
    public ResultSet executeSPARQL(String queryString) {
        this.openTransaction(false);
        final VirtuosoQueryExecution queryExecution = VirtuosoQueryExecutionFactory.create(queryString, (VirtGraph) this.virtModel.getGraph());
        final ResultSet resultSet = queryExecution.execSelect();
        ResultSetFormatter.out(System.out, resultSet, queryExecution.getQuery());
        queryExecution.close();
        this.commitTransaction();

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
    public void openTransaction(boolean write) {
        if (!locked) {
            logger.debug("Opening transaction");
            virtModel.begin();
        } else {
            logger.debug("Model is locked, keeping transaction alive");
        }
    }

    @Override
    public void commitTransaction() {
        if (!locked) {
            logger.debug("Closing transaction");
            virtModel.commit();
        } else {
            logger.debug("Model is locked, not committing");
        }
    }


    @Override
    public void lock () {
        this.locked = true;
    }

    @Override
    public void openAndLock(boolean write) {
        logger.debug("Locking open");
        openTransaction(write);
        lock();
    }

    @Override
    public void unlock() {
        this.locked = false;
    }

    @Override
    public void unlockAndClose() {
        logger.debug("Unlocking and closing");
        unlock();
        commitTransaction();
    }

    protected static ByteArrayInputStream ontologytoIS(OWLOntology ontology) throws OWLOntologyStorageException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}

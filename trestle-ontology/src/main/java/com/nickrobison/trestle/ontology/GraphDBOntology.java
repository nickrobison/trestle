package com.nickrobison.trestle.ontology;

import afu.org.apache.commons.io.FileUtils;
import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.ontotext.jena.SesameDataset;
import com.ontotext.trree.OwlimSchemaRepository;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.Lock;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 1/10/17.
 */

public class GraphDBOntology extends JenaOntology {

    private static final Logger logger = LoggerFactory.getLogger(GraphDBOntology.class);
    public static final String DATA_DIRECTORY = "./target/data";
    private static SailRepositoryConnection connection;
    private static SesameDataset dataset;


    GraphDBOntology(String ontologyName, OWLOntology ont, DefaultPrefixManager pm) {
        super(ontologyName, constructJenaModel(), ont, pm);
    }

    private static Model constructJenaModel() {
        OwlimSchemaRepository schema = new OwlimSchemaRepository();
        schema.setDataDir(new File(DATA_DIRECTORY));
        Map<String, String> schemaParams = new HashMap<>();
        schemaParams.put("storage-folder", "./");
        schemaParams.put("repository-type", "file-repository");
        schemaParams.put("ruleset", "rdfs");
        schemaParams.put("base-URL", "http://nickrobison.com/dissertation/trestle.owl#");
        schema.setParameters(schemaParams);
        final SailRepository repository = new SailRepository(schema);
        repository.initialize();
        connection = repository.getConnection();
        dataset = new SesameDataset(connection);

        return ModelFactory.createModelForGraph(dataset.getDefaultGraph());
    }

    @Override
    public boolean isConsistent() {
        return false;
    }

    @Override
    public void initializeOntology() {
        logger.info("Dropping GraphDB ontology {}", ontologyName);
        if (!model.isEmpty()) {
            model.removeAll();
        }

//        We need to read out the ontology into a bytestream and then read it back into the oracle format
        logger.info("Creating new model {}", ontologyName);
        logger.debug("Writing out the ontology to byte array");

        try {
            connection.begin();
            connection.add(ontologytoIS(this.ontology), "urn:base", RDFFormat.RDFXML);
            connection.commit();
        } catch (OWLOntologyStorageException | IOException e) {
            logger.error("Cannot read ontology into model", e);
            throw new RuntimeException("Cannot read ontology in model", e);
        }
    }

    @Override
    public void close(boolean drop) {
        if (drop) {
            this.model.close();
            dataset.close();
            connection.close();
            logger.info("Dropping model {} at {}", this.ontologyName, DATA_DIRECTORY);
            try {
                FileUtils.deleteDirectory(new File(DATA_DIRECTORY));
            } catch (IOException e) {
                logger.error("Could not delete data directory {}", DATA_DIRECTORY, e);
            }
        }
        logger.debug("Opened {} transactions, committed {}", this.openedTransactions.get(), this.committedTransactions.get());
    }

    @Override
    public void runInference() {
    }

    @Override
    public TrestleResultSet executeSPARQLTRS(String queryString) {
        final TrestleResultSet resultSet;
        final Query query = QueryFactory.create(queryString);
        final QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            resultSet = this.buildResultSet(qexec.execSelect());
        } finally {
            qexec.close();
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }
        return resultSet;
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        connection.begin();
//        if (write) {
//            connection.
//            dataset.begin(ReadWrite.WRITE);
//            logger.debug("GraphDB model write transaction opened");
//        } else {
//            dataset.begin(ReadWrite.READ);
//            logger.debug("GraphDB model read transaction opened");
//        }
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        connection.commit();
//        dataset.commit();
        logger.debug("GraphDB model transaction committed");
    }
}

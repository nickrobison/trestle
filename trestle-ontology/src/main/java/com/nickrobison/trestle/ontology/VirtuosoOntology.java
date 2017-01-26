package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResultSet;
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
import virtuoso.jena.driver.*;

import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by nrobison on 7/22/16.
 */
@SuppressWarnings({"initialization", "Duplicates"})
@ThreadSafe
public class VirtuosoOntology extends JenaOntology {

    private static final Logger logger = LoggerFactory.getLogger(VirtuosoOntology.class);
    private final String TRESTLE_RULES;
    private static VirtModel virtModel;

    VirtuosoOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
        super(name, initializeVirtModel(name, connectionString, username, password), ont, pm);
        TRESTLE_RULES = String.format("%s_trestle_rules", name);
        try {
            virtModel.setRuleSet(TRESTLE_RULES);
        } catch (Exception e) {
            logger.error("Ruleset {} doesn't exist", TRESTLE_RULES, e);
        }
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
        virtModel.removeRuleSet(TRESTLE_RULES, this.ontologyName);
        try {
            if (!virtModel.isEmpty()) {
                virtModel.removeAll();
            }
        } catch (Exception e) {
            logger.warn("Unable to remove ruleset {}", TRESTLE_RULES, e);
        }

        logger.info("Writing new ontology");

        try {
            this.model.read(ontologytoIS(this.ontology), null);
        } catch (OWLOntologyStorageException e) {
            logger.error("Cannot read ontology", e);
            throw new RuntimeException("Cannot read ontology", e);
        }
        logger.debug("Finished writing ontology");

//        Create a new ruleset
//        We have to do this after we load the ontology for disk, otherwise it won't know to create the inferencing rules.
        virtModel.createRuleSet(TRESTLE_RULES, this.ontologyName);
        virtModel.setRuleSet(TRESTLE_RULES);
        logger.info("Creating ruleset {} for ontology {}", TRESTLE_RULES, this.ontologyName);
    }

    @Override
    public TrestleResultSet executeSPARQLResults(String queryString) {
//        ResultSet resultSet;
        final TrestleResultSet resultSet;
        final QueryExecution queryExecution = VirtuosoQueryExecutionFactory.create(queryString, (VirtGraph) virtModel.getGraph());
        this.openTransaction(false);
        this.model.enterCriticalSection(Lock.READ);
        try {
            resultSet = this.buildResultSet(queryExecution.execSelect());
        } finally {
            queryExecution.close();
            this.model.leaveCriticalSection();
            this.commitTransaction(false);
        }
        return resultSet;
    }

    public void runInference() {
    }

    @Override
    public void close(boolean drop) {
        if (drop) {
            logger.info("Removing ruleset {}", TRESTLE_RULES);
            virtModel.removeRuleSet(TRESTLE_RULES, this.ontologyName);
            logger.info("Dropping model {}", this.ontologyName);
            virtModel.removeAll();
        }
        logger.info("Closing model", this.ontologyName);
        this.model.close();
        virtModel.close();
        logger.debug("Opened {} transactions, committed {}", this.openedTransactions.get(), this.committedTransactions.get());
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        virtModel.begin();
        logger.debug("Virtuoso model transaction opened");
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        virtModel.commit();
        logger.debug("Virtuoso model transaction committed");
    }

    protected static ByteArrayInputStream ontologytoIS(OWLOntology ontology) throws OWLOntologyStorageException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ontology.saveOntology(new RDFXMLDocumentFormat(), out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}

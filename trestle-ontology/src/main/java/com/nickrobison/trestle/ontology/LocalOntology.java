package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.ontology.types.TrestleResultSet;
import com.nickrobison.trestle.ontology.utils.SharedOntologyFunctions;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.query.spatial.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.shared.Lock;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by nrobison on 6/15/16.
 */
@SuppressWarnings({"initialization", "Duplicates"})
public class LocalOntology extends JenaOntology {

    private static final String DATA_DIRECTORY = "./target/data";
    private static final Logger logger = LoggerFactory.getLogger(LocalOntology.class);
    private static Dataset tdbDataset;
    private static Dataset luceneDataset;
    private final SpatialIndex index;
//    private ThreadLocal<Boolean> locked;
//    private ThreadLocal<Boolean> writeTransaction;
//    private boolean locked = false;
//    private boolean writeTransaction = false;
    private final DatasetGraphSpatial datasetGraphSpatial;
    private final SpatialIndexContext spatialIndexContext;


    LocalOntology(String ontologyName, OWLOntology ont, DefaultPrefixManager pm) {
        super(ontologyName, constructJenaModel(), ont, pm, new QueryBuilder(QueryBuilder.Dialect.JENA, pm));
        datasetGraphSpatial = (DatasetGraphSpatial) luceneDataset.asDatasetGraph();
        this.index = datasetGraphSpatial.getSpatialIndex();
        spatialIndexContext = new SpatialIndexContext(this.index);
        this.singleWriterOntology = true;

//        //        Set threadlocals
//        this.writeTransaction = new ThreadLocal<Boolean>() {
//            @Override
//            protected Boolean initialValue() {
//                return false;
//            }
//        };
//        this.locked = new ThreadLocal<Boolean>() {
//            @Override
//            protected Boolean initialValue() {
//                return false;
//            }
//        };
    }

    //    I'm suppressing this because I think we need the resource, and this is going away in 0.10
    @SuppressWarnings({"squid:S2095"})
    private static Model constructJenaModel() {
        tdbDataset = initialiseTDB();
        //        spatial stuff
        Directory indexDirectory;
        Dataset spatialDataset = null;
        logger.debug("Building TDB and Lucene database");
        try {
            indexDirectory = FSDirectory.open(new File(DATA_DIRECTORY + "/lucene"));
//            Not sure if these entity and geo fields are correct, but oh well.
            EntityDefinition ed = new EntityDefinition("entityField", "geoField");
//            ed.setSpatialContextFactory("com.spatial4j.core.context.jts.JtsSpatialContextFactory");
            ed.setSpatialContextFactory(SpatialQuery.JTS_SPATIAL_CONTEXT_FACTORY_CLASS);
//            Create a spatial dataset that combines the TDB dataset + the spatial index
//            SpatialDatasetFactory.createLucene(ds, indexDirectory, ed);
            spatialDataset = SpatialDatasetFactory.createLucene(tdbDataset, indexDirectory, ed);
            logger.debug("Lucene index is up and running");
        } catch (IOException e) {
            throw new RuntimeException("Cannot create spatial dataset", e);
        }
        final Reasoner owlReasoner = ReasonerRegistry.getOWLReasoner();
//        I don't think I need the TDBmodelgetter, since I just bind it to the default model
        Model model = ModelFactory.createInfModel(owlReasoner, spatialDataset.getDefaultModel());
//        TDB.sync(this.model);
//        this.graph = this.model.getGraph();
        luceneDataset = spatialDataset;

        return model;
    }

    private static Dataset initialiseTDB() {
        final String tdbPath = DATA_DIRECTORY + "/tdb";
        new File(tdbPath).mkdirs();

        return TDBFactory.createDataset(tdbPath);
    }

    @Override
    public boolean isConsistent() {
        return ((InfModel) this.model).validate().isValid();
    }

    @Override
    public void initializeOntology() {
//        this.openTransaction(true);
        logger.info("Dropping local ontology {}", ontologyName);
        if (!model.isEmpty()) {
            model.removeAll();
        }

        //        We need to read out the ontology into a bytestream and then read it back into the oracle format
        logger.info("Creating new model {}", ontologyName);
        logger.debug("Writing out the ontology to byte array");

        try {
            model.read(SharedOntologyFunctions.ontologytoIS(this.ontology), null);
        } catch (OWLOntologyStorageException e) {
            logger.error("Cannot read ontology into model", e);
            throw new RuntimeException("Cannot read ontology in model", e);
        }
//        TDB.sync(this.model);
//        this.commitTransaction();
//        updateSpatialIndex();
    }

//    http://stackoverflow.com/questions/35801520/geospatial-queries-with-apache-jena
//    private void updateSpatialIndex() {
//        logger.info("Updating spatial index");
////        this.openTransaction(true);
//        this.index.startIndexing();
////        this.openTransaction(true);
//        final Iterator<Quad> quadIterator = datasetGraphSpatial.find(Node.ANY, Node.ANY, Node.ANY, Node.ANY);
//        while (quadIterator.hasNext()) {
//            final Quad next = quadIterator.next();
//            this.spatialIndexContext.index(next.getGraph(), next.getSubject(), next.getPredicate(), next.getObject());
//        }
////        this.commitTransaction();
//        this.index.finishIndexing();
////        this.commitTransaction();
//    }

    @Override
    @SuppressWarnings({"return.type.incompatible"})
    public TrestleResultSet executeSPARQLResults(String queryString) {
        final TrestleResultSet resultSet;
        final Query query = QueryFactory.create(queryString);
        final QueryExecution qExec = QueryExecutionFactory.create(query, luceneDataset);
        this.openTransaction(false);
        model.enterCriticalSection(Lock.READ);
        try {
            resultSet = this.buildResultSet(qExec.execSelect());
        } finally {
            qExec.close();
            model.leaveCriticalSection();
            this.commitTransaction(false);
        }

        return resultSet;
    }

    @Override
    public void runInference() {

    }

    @Override
    public void close(boolean drop) {
        this.commitTransaction(false);
        model.close();
        TDB.closedown();
        if (drop) {
            try {
                logger.debug("Deleting {}", DATA_DIRECTORY);
                FileUtils.deleteDirectory(new File(DATA_DIRECTORY));
            } catch (IOException e) {
                logger.error("Could not delete data directory", e);
            }
        }

        logger.info("Opened {} transactions, committed {}", this.openedTransactions.get(), this.committedTransactions.get());
    }

    @Override
    public void openDatasetTransaction(boolean write) {
        if (write) {
            luceneDataset.begin(ReadWrite.WRITE);
//            this.model.enterCriticalSection(Lock.WRITE);
            logger.debug("Opened writable transaction");
        } else {
            luceneDataset.begin(ReadWrite.READ);
//            this.model.enterCriticalSection(Lock.WRITE);
            logger.debug("Opened read-only transaction");
        }
    }

    @Override
    public void commitDatasetTransaction(boolean write) {
        luceneDataset.commit();
//        this.model.leaveCriticalSection();
    }

    @Override
    public void abortDatasetTransaction(boolean write) {
        luceneDataset.abort();
    }
}

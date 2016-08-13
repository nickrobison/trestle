//package com.nickrobison.trestle.ontology;
//
//import com.complexible.common.protocols.server.Server;
//import com.complexible.common.protocols.server.ServerException;
//import com.complexible.stardog.Stardog;
//import com.complexible.stardog.api.ConnectionConfiguration;
//import com.complexible.stardog.api.admin.AdminConnection;
//import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
//import com.complexible.stardog.api.reasoning.ReasoningConnection;
//import com.complexible.stardog.jena.SDJenaFactory;
//import com.complexible.stardog.protocols.snarl.SNARLProtocolConstants;
//import org.apache.jena.ontology.OntModel;
//import org.apache.jena.rdf.model.Model;
//import org.semanticweb.owlapi.model.OWLOntology;
//import org.semanticweb.owlapi.model.OWLOntologyStorageException;
//import org.semanticweb.owlapi.util.DefaultPrefixManager;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Created by nrobison on 8/9/16.
// */
//@SuppressWarnings({"initialization.fields.uninitialized"})
//public class StardogOntology extends JenaOntology {
//
//    private static final Logger logger = LoggerFactory.getLogger(StardogOntology.class);
//    private static AdminConnection adminConnection;
//    private static ReasoningConnection connection;
//
//    private boolean locked = false;
//
//    StardogOntology(String name, OWLOntology ont, DefaultPrefixManager pm, String connectionString, String username, String password) {
//        super(name, createStardogModel(name, connectionString, username, password), ont, pm);
//    }
//
//    private static Model createStardogModel(String name, String connectionString, String username, String password) {
//
//        final Server aServer;
//        if (connectionString.contains("embedded")) {
//            try {
//                aServer = Stardog
//                        .buildServer()
//                        .bind(SNARLProtocolConstants.EMBEDDED_ADDRESS)
//                        .start();
//            } catch (ServerException e) {
//                logger.error("Cannot setup Stardog server", e);
//                throw new RuntimeException("Cannot setup stardog server", e);
//            }
////            We need to open an admin connection in order to create/drop the model.
//            adminConnection = AdminConnectionConfiguration
//                    .toEmbeddedServer()
//                    .credentials(username, password)
//                    .connect();
//        } else {
////            TODO(nrobison): Do this
//            try {
//                aServer = Stardog
//                        .buildServer()
//                        .bind(SNARLProtocolConstants.EMBEDDED_ADDRESS)
//                        .start();
//            } catch (ServerException e) {
//                logger.error("Cannot setup Stardog server", e);
//                throw new RuntimeException("Cannot setup stardog server", e);
//            }
//        }
//
////        If the database doesn't exist, create it.
//        if (!adminConnection.list().contains(name)) {
//            logger.info("Ontology {} doesn't exist in the Stardog database", name);
//            adminConnection.memory(name)
//                    .create();
//        }
//
////            Now a normal connection to do other stardoggy things.
//        connection = ConnectionConfiguration
//                .to(name)
//                .credentials(username, password)
//                .reasoning(true)
//                .connect()
//                .as(ReasoningConnection.class);
//
////        Stardog uses its own built-in reasoner, so we can just return the basic model and keep moving
//        return SDJenaFactory.createModel(connection);
//
//    }
//
//    @Override
//    public boolean isConsistent() {
//        return ((OntModel) this.model).validate().isValid();
//    }
//
//    @Override
//    public void initializeOntology() {
//        logger.info("Dropping Stardog model {}", this.ontologyName);
//        if (adminConnection.list().contains(this.ontologyName)) {
//            adminConnection.drop(this.ontologyName);
//        }
//
//        adminConnection.createMemory(this.ontologyName);
//        this.openAndLock(true);
//
//        try {
////            model.getReader("N3").read(this.model, ontologytoIS(this.ontology), "");
//            model.read(ontologytoIS(this.ontology), "");
//        } catch (OWLOntologyStorageException e) {
//            logger.error("Cannot load ontology into Stardog model", e);
//        }
//        this.unlockAndCommit();
//    }
//
//    @Override
//    public void close(boolean drop) {
//        logger.info("Disconnecting from Stardog server {}", this.ontologyName);
//        this.model.close();
//        if (drop) {
//            logger.info("Dropping Stardog model {}", this.ontologyName);
//            adminConnection.drop(this.ontologyName);
//        }
//        adminConnection.close();
//        connection.close();
//    }
//
//    @Override
//    public void lock() {
//        this.locked = true;
//    }
//
//    @Override
//    public void openAndLock(boolean write) {
//        logger.debug("Locking open");
//        openTransaction(write);
//        lock();
//    }
//
//    @Override
//    public void unlock() {
//        this.locked = false;
//    }
//
//    @Override
//    public void unlockAndCommit() {
//        logger.debug("Unlocking and committing");
//        commitTransaction();
//        lock();
//    }
//
//    @Override
//    public void commitTransaction() {
//        if (!locked) {
//            logger.debug("Closing transaction");
//            model.commit();
//        } else {
//            logger.debug("Model is locked, not committing");
//        }
//    }
//
//    @Override
//    public void openTransaction(boolean write) {
//        if (!locked) {
//            logger.debug("Opening transaction");
//            model.begin();
//        } else {
//            logger.debug("Model is locked, keeping transaction alive");
//        }
//    }
//}

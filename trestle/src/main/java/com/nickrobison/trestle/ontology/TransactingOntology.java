package com.nickrobison.trestle.ontology;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 9/7/16.
 */
abstract class TransactingOntology {

    private static final Logger logger = LoggerFactory.getLogger(TransactingOntology.class);
    private final static OntologySecurityManager securityManager = new OntologySecurityManager();

//    Thread locals
    private ThreadLocal<Boolean> threadLocked = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };

    private ThreadLocal<Boolean> threadInTransaction = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };

    /**
     * Open a transaction and transactionLock it, for lots of bulk action
     */
    private void lock() {
        this.threadLocked.set(true);
//        this.applicationLock = true;
    }

    /**
     * Unlock the model to allow for closing the transaction
     */
    private void unlock() {
        this.threadLocked.set(false);
    }

    /**
     * Open a transaction and transactionLock it
     * @param write - Open writable transaction?
     */
    public void openAndLock(boolean write) {
        logger.debug("Locking open");

        if (this.threadLocked.get()) {
            logger.debug("Thread already locked, continuing");
        } else {
            logger.debug("Trying to open unlocked transaction");
            openTransaction(write);
            lock();
            logger.debug("Transaction opened and locked");
        }
    }

    /**
     * Unlock the transaction and commit it
     */
    public void unlockAndCommit() {
        logger.debug("Unlocking and closing");
        unlock();
        logger.debug("Trying to commit transaction");
        commitTransaction();
        logger.debug("Committed transaction");
    }

    public void openTransaction(boolean write) {

        if (!this.threadLocked.get()) {
            if (!this.threadInTransaction.get()) {
                logger.debug("Trying to open transaction");
                this.openDatasetTransaction(write);
                logger.debug("Opened transaction");
                this.threadInTransaction.set(true);
            } else {
                logger.debug("Thread unlocked, but already in a transaction");
            }
        } else {
            logger.debug("Thread locked, continuing");
        }
    }

    public void commitTransaction() {
        if (!this.threadLocked.get()) {
            if (this.threadInTransaction.get()) {
                logger.debug("Trying to commit transaction");
                this.commitDatasetTransaction();
                logger.debug("Committed dataset transaction");
                this.threadInTransaction.set(false);
            } else {
                logger.debug("Thread unlocked, but not in transaction");
            }
        } else {
            logger.debug("Thread locked, not commiting");
        }
    }


    private static class OntologySecurityManager extends SecurityManager {

        public String getCallerClassName(int callstackDepth) {
            return getClassContext()[callstackDepth].getName();
        }
    }


    public abstract void openDatasetTransaction(boolean write);

    public abstract void commitDatasetTransaction();

}

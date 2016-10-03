package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.transactions.TrestleTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nrobison on 9/7/16.
 */
abstract class TransactingOntology {

    private static final Logger logger = LoggerFactory.getLogger(TransactingOntology.class);
    private final static OntologySecurityManager securityManager = new OntologySecurityManager();
    protected final AtomicInteger openedTransactions = new AtomicInteger();
    protected final AtomicInteger committedTransactions = new AtomicInteger();

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

    private ThreadLocal<TrestleTransaction> threadTransactionObject = new ThreadLocal<>();

//    public TrestleTransaction createandOpenNewTransaction(TrestleTransaction transactionObject) {
//        threadTransactionObject.set(transactionObject);
//
//    }

    public TrestleTransaction createandOpenNewTransaction(boolean write) {
        if (threadTransactionObject.get() == null) {
            logger.debug("Unowned transaction, opening a new one");
            this.openAndLock(write);
            final TrestleTransaction trestleTransaction = new TrestleTransaction(System.nanoTime(), write);
            threadTransactionObject.set(trestleTransaction);
            return trestleTransaction;
        } else {
            logger.debug("Thread transaction owned, returning empty object");
            return new TrestleTransaction();
        }
    }

    /**
     * Try to commit the current thread transaction, if the object owns the currently open transaction
     * @param transaction - Transaction object to try to commit current transaction with
     */
    public void returnAndCommitTransaction(TrestleTransaction transaction) {
        final TrestleTransaction trestleTransaction = threadTransactionObject.get();
        if (trestleTransaction != null) {
            if (trestleTransaction.equals(transaction)) {
                logger.debug("Owns transaction, committing");
                threadTransactionObject.set(null);
                this.unlockAndCommit();
            } else {
                logger.debug("Doesn't own transaction, continuing");
            }
        } else {
            logger.warn("Null transaction object, how did that happen?");
        }
    }

    /**
     * Open a transaction and Lock it, for lots of bulk action
     */
    private void lock() {
        this.threadLocked.set(true);
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
        if (this.threadTransactionObject.get() == null) {

            if (this.threadLocked.get()) {
                logger.debug("Thread already locked, continuing");
            } else {
                logger.debug("Trying to open unlocked transaction");
                openTransaction(write);
                logger.debug("Locking open");
                lock();
                logger.debug("Transaction opened and locked");
            }
        } else {
            logger.warn("Thread transaction owned by transaction object, not opening or locking");
        }
    }

    /**
     * Unlock the transaction and commit it
     */
    public void unlockAndCommit() {

        if (threadTransactionObject.get() == null) {
            logger.debug("Unlocking and closing");
            unlock();
            logger.debug("Trying to commit transaction");
            commitTransaction();
            logger.debug("Committed transaction");
        } else {
            logger.debug("Thread owned by transaction object, not unlocking or committing");
        }
    }

    public void openTransaction(boolean write) {

        if (!this.threadLocked.get()) {
            if (!this.threadInTransaction.get()) {
                logger.debug("Trying to open transaction");
                this.openDatasetTransaction(write);
                logger.debug("Opened transaction");
                this.threadInTransaction.set(true);
                this.openedTransactions.incrementAndGet();
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
                this.committedTransactions.incrementAndGet();
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

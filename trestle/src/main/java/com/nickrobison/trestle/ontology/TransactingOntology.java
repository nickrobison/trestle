package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.transactions.TrestleTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by nrobison on 9/7/16.
 */
abstract class TransactingOntology {

    private static final Logger logger = LoggerFactory.getLogger(TransactingOntology.class);
    private static final OntologySecurityManager securityManager = new OntologySecurityManager();
    protected final AtomicInteger openedTransactions = new AtomicInteger();
    protected final AtomicInteger committedTransactions = new AtomicInteger();
    protected static boolean singleWriterOntology = false;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final Condition writeCondition = writeLock.newCondition();
    private final Object writerAvailable = new Object();
//    private Boolean writerAvailable = true;

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

    private ThreadLocal<Boolean> threadInWriteTransaction = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };

    private ThreadLocal<Boolean> threadTransactionInherited = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return false;
        }
    };

    private ThreadLocal<TrestleTransaction> threadTransactionObject = new ThreadLocal<>();

    /**
     * Takes an existing transaction object and inherits
     * @param transactionObject
     * @param write
     * @return
     */
    public TrestleTransaction createandOpenNewTransaction(TrestleTransaction transactionObject, boolean write) {
        logger.debug("Inheriting transaction from existing transaction object, setting flags, but not opening new transaction");
//        this.openAndLock(write);
        this.threadLocked.set(true);
        this.threadInTransaction.set(true);
        threadTransactionObject.set(transactionObject);
        threadTransactionInherited.set(true);
        return transactionObject;
    }

    public TrestleTransaction createandOpenNewTransaction(boolean write) {
        if (threadTransactionObject.get() == null) {
            logger.debug("Unowned transaction, opening a new one");
            this.openAndLock(write);
            final TrestleTransaction trestleTransaction = new TrestleTransaction(System.nanoTime(), write);
            threadTransactionObject.set(trestleTransaction);
            return trestleTransaction;
        } else {
            logger.warn("Thread transaction owned, returning empty object");
            return new TrestleTransaction();
        }
    }

    /**
     * Try to commit the current thread transaction, if the object owns the currently open transaction
     * @param transaction - Transaction object to try to commit current transaction with
     */
    public void returnAndCommitTransaction(TrestleTransaction transaction) {
//        If the transaction state is inherited, don't commit
        if (!threadTransactionInherited.get()) {
            final TrestleTransaction trestleTransaction = threadTransactionObject.get();
            if (trestleTransaction != null) {
                if (trestleTransaction.equals(transaction)) {
                    logger.debug("Owns transaction, committing");
                    threadTransactionObject.set(null);
                    this.unlockAndCommit(transaction.isWriteTransaction());
                } else {
                    logger.debug("Doesn't own transaction, continuing");
                }
            } else {
                logger.warn("Null transaction object, how did that happen?");
            }
        } else {
            logger.debug("Transaction state is inherited, continuing");
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
            logger.debug("Thread transaction owned by transaction object, not opening or locking");
        }
    }

    /**
     * Unlock the transaction and commit it
     * @param write - Is this a write transaction?
     */
    public void unlockAndCommit(boolean write) {

        if (threadTransactionObject.get() == null) {
            logger.debug("Unlocking and closing");
            unlock();
            logger.debug("Trying to commit transaction");
            commitTransaction(write);
            logger.debug("Committed transaction");
        } else {
            logger.debug("Thread owned by transaction object, not unlocking or committing");
        }
    }

    public void openTransaction(boolean write) {

        if (!this.threadLocked.get()) {
            if (!this.threadInTransaction.get()) {

                logger.debug("Trying to open transaction");
//                If it's a writable transaction, then we need to wait to be free
//                if (write) {
//                    writeLock.
//                    synchronized (writerAvailable) {
//                        try {
//                            logger.debug("Thread {} waiting for write lock", Thread.currentThread().getName());
//                            writerAvailable.wait();
//                        } catch (InterruptedException e) {
//                            logger.error("Opening transaction interrupted", e);
//                        }
//                    }
//                }
//                    synchronized (writerAvailable) {
////                        this.writeLock.lock();
//                        this.writerAvailable = false;
//                        this.openDatasetTransaction(true);
//                        this.threadInWriteTransaction.set(true);
//                    }
//                } else {
//                    this.openDatasetTransaction(false);
//                }
                logger.debug("Thread {} taking the lock", Thread.currentThread().getName());
                this.openDatasetTransaction(write);
                logger.debug("Opened transaction");
                this.threadInTransaction.set(true);
                this.openedTransactions.incrementAndGet();
                if (write) {
                    this.threadInWriteTransaction.set(true);
                }
            } else {
                logger.debug("Thread unlocked, but already in a transaction");
            }
        } else {
            logger.debug("Thread locked, continuing");
        }
    }

    public void commitTransaction(boolean write) {
        if (!this.threadLocked.get()) {
            if (this.threadInTransaction.get()) {
                logger.debug("Trying to commit transaction");
                this.commitDatasetTransaction(write);
//                if (this.threadInWriteTransaction.get()) {
//                    logger.debug("In write transaction");
//                    synchronized (writerAvailable) {
//                        writerAvailable.notifyAll();
//                        logger.debug("Thread {} releasing the write lock", Thread.currentThread().getName());
//                    }
//                }
//                if (this.threadInWriteTransaction.get()) {
//                    this.commitDatasetTransaction();
//                    this.writeLock.unlock();
//                    this.threadInWriteTransaction.set(false);
//                } else {
//                    this.commitDatasetTransaction();
//                }
                logger.debug("Committed dataset transaction");
                this.threadInTransaction.set(false);
                this.threadTransactionInherited.set(false);
                this.committedTransactions.incrementAndGet();
            } else {
                logger.debug("Thread unlocked, but not in transaction");
            }
        } else {
            logger.debug("Thread locked, not committing");
        }
    }


    private static class OntologySecurityManager extends SecurityManager {

        public String getCallerClassName(int callstackDepth) {
            return getClassContext()[callstackDepth].getName();
        }
    }


    public abstract void openDatasetTransaction(boolean write);

    public abstract void commitDatasetTransaction(boolean write);

}

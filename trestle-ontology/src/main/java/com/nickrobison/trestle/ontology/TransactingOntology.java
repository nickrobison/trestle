package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.transactions.TrestleTransaction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by nrobison on 9/7/16.
 */
abstract class TransactingOntology implements ITrestleOntology {

    private static final Logger logger = LoggerFactory.getLogger(TransactingOntology.class);
    private static final OntologySecurityManager securityManager = new OntologySecurityManager();
    protected final AtomicInteger openWriteTransactions = new AtomicInteger();
    protected final AtomicInteger openReadTransactions = new AtomicInteger();
    protected final AtomicLong openedTransactions = new AtomicLong();
    protected final AtomicLong committedTransactions = new AtomicLong();
    protected static boolean singleWriterOntology = false;

//    Thread locals
    private ThreadLocal<Boolean> threadLocked = ThreadLocal.withInitial(() -> false);

    private ThreadLocal<Boolean> threadInTransaction = ThreadLocal.withInitial(() -> false);

    private ThreadLocal<Boolean> threadInWriteTransaction = ThreadLocal.withInitial(() -> false);

    private ThreadLocal<Boolean> threadTransactionInherited = ThreadLocal.withInitial(() -> false);

    private ThreadLocal<TrestleTransaction> threadTransactionObject = new ThreadLocal<>();

    /**
     * Set the current thread transaction state, using the information inherited from the TrestleTransaction object
     *
     * @param transactionObject - Transaction Object to take ownership of thread transaction
     * @param write - Writable transaction?
     * @return
     */
    @Override
    public TrestleTransaction createandOpenNewTransaction(TrestleTransaction transactionObject, boolean write) {
        logger.debug("Inheriting transaction from existing transaction object, setting flags, but not opening new transaction");
        this.threadLocked.set(true);
        this.threadInTransaction.set(true);
        threadTransactionObject.set(transactionObject);
        threadTransactionInherited.set(true);
        return transactionObject;
    }

    /**
     * Set the current thread transaction state as a read transaction, using the information inherited from the TrestleTransaction object
     * @param transactionObject - Existing TrestleTransactionObject
     * @return - TrestleTransaction object inheriting from parent transaction
     */
    @Override
    public TrestleTransaction createandOpenNewTransaction(TrestleTransaction transactionObject) {
        return createandOpenNewTransaction(transactionObject, transactionObject.isWriteTransaction());
    }

    /**
     * Create and open a new transaction.
     * If the thread is already in an open transaction, we return an empty TrestleTransaction object
     * @param write - Is this a write transaction?
     * @return
     */
    @Override
    public TrestleTransaction createandOpenNewTransaction(boolean write) {
        if (threadTransactionObject.get() == null) {
            logger.debug("Unowned transaction, opening a new one");
            final TrestleTransaction trestleTransaction = new TrestleTransaction(System.nanoTime(), write);
            trestleTransaction.setConnection(this.getOntologyConnection());
            threadTransactionObject.set(trestleTransaction);
            this.openAndLock(write, true);
            return trestleTransaction;
        } else {
            logger.warn("Thread transaction owned, returning empty object");
            final TrestleTransaction trestleTransaction = new TrestleTransaction(write);
            trestleTransaction.setConnection(this.getOntologyConnection());
            return trestleTransaction;
        }
    }

    /**
     * Return a TrestleTransaction object and attempt to commit the current Transaction
     * If the TrestleTransaction object does not own the current transaction, we continue without committing
     * @param transaction - Transaction object to try to commit current transaction with
     */
    @Override
    public void returnAndCommitTransaction(TrestleTransaction transaction) {
//        If the transaction state is inherited, don't commit
        if (!threadTransactionInherited.get()) {
            final TrestleTransaction trestleTransaction = threadTransactionObject.get();
            if (trestleTransaction != null) {
                if (trestleTransaction.equals(transaction)) {
                    logger.debug("Owns transaction, committing");
                    this.unlockAndCommit(transaction.isWriteTransaction(), true);
                    threadTransactionObject.set(null);
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
     *
     * @param write - Open writable transaction?
     */
    public void openAndLock(boolean write) {
        this.openAndLock(write, false);
    }

    /**
     * Open a transaction and lock it
     * Optionally force the transaction, even if an existing transaction object is set
     * Used to initially lock a transaction when a new transaction object is created
     * @param write - Open writable transaction?
     * @param force - Force open transaction?
     */
    private void openAndLock(boolean write, boolean force) {
        if (force) {
            logger.trace("Forcing open transaction");
        }
//        If there's no existing transaction, or the transactions is being forced open
        if (this.threadTransactionObject.get() == null || force) {

            if (this.threadLocked.get()) {
                logger.trace("Thread already locked, continuing");
            } else {
                logger.trace("Trying to open unlocked transaction");
                openTransaction(write, force);
                logger.trace("Locking open");
                lock();
                logger.trace("Transaction opened and locked");
            }
        } else {
            logger.trace("Thread transaction owned by transaction object, not opening or locking");
        }
    }

    /**
     * Unlock the transaction and commit it
     *
     * @param write - Is this a write transaction?
     */
    public void unlockAndCommit(boolean write) {
        this.unlockAndCommit(write, false);
    }

    /**
     * Unlock the transaction and commit it
     * Optionally, for the transaction to unlock, even if there's an existing transaction object
     * Used to close the transaction when the transaction object is returned
     * @param write - Writable transaction?
     * @param force - Force transaction to commit?
     */
    private void unlockAndCommit(boolean write, boolean force) {
        if (force) {
            logger.trace("Forcing closed transaction");
        }
//        If there's no exisiting transaction object, or the transaction is being forced closed
        if (threadTransactionObject.get() == null || force) {
            logger.trace("Unlocking and closing");
            unlock();
            logger.trace("Trying to commit transaction");
            commitTransaction(write, force);
            logger.trace("Committed transaction");
        } else {
            logger.trace("Thread owned by transaction object, not unlocking or committing");
        }
    }

    /**
     * Open transaction
     * @param write - Open a writable transaction
     */
    public void openTransaction(boolean write) {
        this.openTransaction(write, false);
    }


    /**
     * Open transaction, optionally force it open, even if an existing transaction object is present
     * @param write - Open a writable transaction
     * @param force - Force open transaction
     */
    private void openTransaction(boolean write, boolean force) {
        if (force) {
            logger.trace("Forcing open transaction");
        }
//        If the thread has a transaction object, don't open a new transaction
        if (threadTransactionObject.get() == null || force) {
//            If the thread is locked, don't open another transaction
            if (!this.threadLocked.get()) {
//                If the thread is already in a transaction, don't open another one
                if (!this.threadInTransaction.get()) {
                    logger.trace("Trying to open transaction");
                    logger.trace("Thread {} taking the lock", Thread.currentThread().getName());
                    this.openDatasetTransaction(write);
                    logger.debug("Opened transaction");

                    this.threadInTransaction.set(true);
                    this.openedTransactions.incrementAndGet();
//                Track read/write transactions
                        if (write) {
                            this.openWriteTransactions.incrementAndGet();
                        } else {
                            this.openReadTransactions.incrementAndGet();
                        }
                        logger.debug("{}/{} open read/write transactions", this.openReadTransactions.get(), this.openWriteTransactions.get());
                    if (write) {
                        this.threadInWriteTransaction.set(true);
                    }
                } else {
                    logger.trace("Thread unlocked, but already in a transaction");
                }
            } else {
                logger.trace("Thread locked, continuing");
            }
        } else {
            logger.trace("Thread owned by transaction object, not unlocking or committing");
        }
    }

    /**
     * Commit transaction
     * @param write - Is this a write transaction?
     */
    public void commitTransaction(boolean write) {
        this.commitTransaction(write, false);
    }

    /**
     * Commit transaction, optionally force closing it
     * @param write - Is this a writable transaction?
     * @param force - Force commit transaction?
     */
    private void commitTransaction(boolean write, boolean force) {
        if (force) {
            logger.trace("Forcing closed transaction");
        }
        if (threadTransactionObject.get() == null || force) {
            if (!this.threadLocked.get() || force) {
                if (this.threadInTransaction.get()) {
                    logger.trace("Trying to commit transaction");
                    this.commitDatasetTransaction(write);
                    logger.trace("Committed dataset transaction");
                    this.threadInTransaction.set(false);
                    this.threadTransactionInherited.set(false);
                    this.committedTransactions.incrementAndGet();
                        if (write) {
                            this.openWriteTransactions.incrementAndGet();
                        } else {
                            this.openReadTransactions.incrementAndGet();
                        }
                        logger.debug("{}/{} open read/write transactions", this.openReadTransactions.get(), this.openWriteTransactions.get());
                } else {
                    logger.trace("Thread unlocked, but not in transaction");
                }
            } else {
                logger.trace("Thread locked, not committing");
            }
        } else {
            logger.trace("Thread owned by transaction object, not committing");
        }
    }

    /**
     * Get the current number of opened transactions, for the lifetime of the application
     * @return - long of opened transactions
     */
    public long getOpenedTransactionCount() {
        return this.openedTransactions.get();
    }

    /**
     * Get the current number of committed transactions, for the lifetime of the application
     * @return - long of committed transactions
     */
    public long getCommittedTransactionCount() {
        return this.committedTransactions.get();
    }


    private static class OntologySecurityManager extends SecurityManager {

        public String getCallerClassName(int callstackDepth) {
            return getClassContext()[callstackDepth].getName();
        }
    }


    public abstract void openDatasetTransaction(boolean write);

    public abstract void commitDatasetTransaction(boolean write);

    /**
     * Set the thread repository connection from the TrestleTransaction object
     */
    public abstract void setOntologyConnection();

    /**
     * Get the thread repository connection to use with the TrestleTransaction object
     * @return - RepositoryConnection for transaction
     */
    public abstract @Nullable RepositoryConnection getOntologyConnection();

    protected TrestleTransaction getThreadTransactionObject() {
        return this.threadTransactionObject.get();
    }

}

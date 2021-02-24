package com.nickrobison.trestle.ontology;

import com.codahale.metrics.annotation.Gauge;
import com.nickrobison.trestle.reasoner.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by nrobison on 9/7/16.
 */
@SuppressWarnings({"type.argument.type.incompatible"})
@Metriced
abstract class TransactingOntology implements ITrestleOntology {

    private static final Logger logger = LoggerFactory.getLogger(TransactingOntology.class);
    //    Thread locals
    private ThreadLocal<Boolean> threadLocked = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private ThreadLocal<Boolean> threadInTransaction = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private ThreadLocal<Boolean> threadInWriteTransaction = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private ThreadLocal<Boolean> threadTransactionInherited = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private ThreadLocal<@Nullable TrestleTransaction> threadTransactionObject = new ThreadLocal<>();


    public static final String OPEN_READ_WRITE_TRANSACTIONS = "{}/{} open read/write transactions";
    public static final String TRANSACTION = "transaction";
    protected final AtomicInteger openWriteTransactions = new AtomicInteger();
    protected final AtomicInteger openReadTransactions = new AtomicInteger();
    protected final AtomicLong openedTransactions = new AtomicLong();
    protected final AtomicLong committedTransactions = new AtomicLong();
    protected final AtomicLong abortedTransactions = new AtomicLong();
    protected static boolean singleWriterOntology = Boolean.FALSE;

    TransactingOntology() {
//        Unused
    }

    /**
     * Set the current thread transaction state, using the information inherited from the TrestleTransaction object
     *
     * @param transactionObject - Transaction Object to take ownership of thread transaction
     * @param write             - {@code true} Writable transaction
     * @return - {@link TrestleTransaction}
     */
    @Override
    public TrestleTransaction createandOpenNewTransaction(@Nullable TrestleTransaction transactionObject, boolean write) {
        if (transactionObject == null) {
            logger.trace("Passed null transaction object, opening a new transaction. Write? {}", write);
            return createandOpenNewTransaction(write);
        } else {
//            Inherit logging context
            final Map<String, String> context = MDC.getCopyOfContextMap();
            final Long txID = transactionObject.getTransactionID();
            if (context == null) {
                logger.debug("Got empty logging context from inherited transaction {}", txID);
                MDC.put(TRANSACTION, Long.toString(txID));
            } else {
                MDC.setContextMap(context);
            }
            logger.trace("Inheriting transaction from existing transaction object {}, setting flags, but not opening new transaction", txID);
            this.threadLocked.set(Boolean.TRUE);
            this.threadInTransaction.set(Boolean.TRUE);
            threadTransactionObject.set(transactionObject);
            threadTransactionInherited.set(Boolean.TRUE);
            this.setOntologyConnection();
            return new TrestleTransaction(transactionObject, transactionObject.isWriteTransaction());
        }
    }

    /**
     * Set the current thread transaction state as a read transaction, using the information inherited from the TrestleTransaction object
     *
     * @param transactionObject - Existing TrestleTransactionObject
     * @return - TrestleTransaction object inheriting from parent transaction
     */
    @Override
    public TrestleTransaction createandOpenNewTransaction(@Nullable TrestleTransaction transactionObject) {
        if (transactionObject == null) {
            logger.warn("Null transaction object. Creating new read-only transaction, as nothing is specified");
            return createandOpenNewTransaction(Boolean.FALSE);
        } else {
            return createandOpenNewTransaction(transactionObject, transactionObject.isWriteTransaction());
        }
    }

    /**
     * Create and open a new transaction.
     * If the thread is already in an open transaction, we return an empty {@link TrestleTransaction} object
     *
     * @param write - {@code true} this a write transaction
     * @return - {@link TrestleTransaction}
     */
    @Override
//    We can suppress this, because the first call is to check whether the transaction object is null or not
    @SuppressWarnings({"dereference.of.nullable"})
    public TrestleTransaction createandOpenNewTransaction(boolean write) {
        final TrestleTransaction threadTransaction = threadTransactionObject.get();
        if (threadTransaction == null) {
            final long transactionID = System.nanoTime();
//            Add the logging context
            logger.debug("Unowned transaction, opening new transaction {}", transactionID);
            MDC.put(TRANSACTION, Long.toString(transactionID));
            final TrestleTransaction trestleTransaction = new TrestleTransaction(transactionID, write);
            trestleTransaction.setConnection(this.getOntologyConnection());
            threadTransactionObject.set(trestleTransaction);
            this.openAndLock(write, Boolean.TRUE);
            return trestleTransaction;
        } else {
            logger.trace("Thread transaction owned by {}, returning empty object", threadTransaction.getTransactionID());
            final TrestleTransaction trestleTransaction = new TrestleTransaction((TrestleTransaction) null, write);
            trestleTransaction.setConnection(this.getOntologyConnection());
            return trestleTransaction;
        }
    }

    /**
     * Return a TrestleTransaction object and attempt to commit the current Transaction
     * If the TrestleTransaction object does not own the current transaction, we continue without committing
     *
     * @param transaction - Transaction object to try to commit current transaction with
     */
    @Override
    public void returnAndCommitTransaction(TrestleTransaction transaction) {
//        If the transaction state is inherited, don't commit
        if (transaction.isRoot()) {
            logger.trace("Owns transaction, committing transaction {}", transaction.getTransactionID());
            this.unlockAndCommit(transaction.isWriteTransaction(), Boolean.TRUE);
            threadTransactionObject.set(null);
//                    Clear the logging context
            MDC.remove(TRANSACTION);
        } else {
            logger.trace("Transaction {} inherited state, continuing", transaction.getTransactionID());
        }
    }

    @Override
    public void returnAndAbortTransaction(TrestleTransaction transaction) {
        if (transaction.isRoot()) {
            logger.trace("Transaction object {} owns transaction, aborting", transaction.getTransactionID());
            this.unlockAndAbort(transaction.isWriteTransaction(), Boolean.TRUE);
            threadTransactionObject.set(null);
            //                    Clear the logging context
            MDC.remove(TRANSACTION);
        } else {
            logger.trace("Transaction {} inherited state, not aborting", transaction.getTransactionID());
        }
    }

    @Override
    public void returnAndAbortWithForce(TrestleTransaction trestleTransaction) {
        logger.error("Force aborting the transaction!");
        this.unlockAndAbort(trestleTransaction.isWriteTransaction(), Boolean.TRUE);
        threadTransactionObject.set(null);
    }

    /**
     * Open a transaction and Lock it, for lots of bulk action
     */
    private void lock() {
        this.threadLocked.set(Boolean.TRUE);
    }

    /**
     * Unlock the model to allow for closing the transaction
     */
    private void unlock() {
        this.threadLocked.set(Boolean.FALSE);
    }

    /**
     * Open a transaction and transactionLock it
     *
     * @param write - Open writable transaction?
     */
    @Override
    public void openAndLock(boolean write) {
        this.openAndLock(write, Boolean.FALSE);
    }

    /**
     * Open a transaction and lock it
     * Optionally force the transaction, even if an existing transaction object is set
     * Used to initially lock a transaction when a new transaction object is created
     *
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
    @Override
    public void unlockAndCommit(boolean write) {
        this.unlockAndCommit(write, Boolean.FALSE);
    }

    /**
     * Unlock the transaction and commit it
     * Optionally, for the transaction to unlock, even if there's an existing transaction object
     * Used to close the transaction when the transaction object is returned
     *
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
     * Unlock the transaction and abort it
     *
     * @param write - Is this a write transaction?
     */
    public void unlockAndAbort(boolean write) {
        this.unlockAndAbort(write, Boolean.FALSE);
    }

    /**
     * Unlock the transaction and abort it
     * Optionally, for the transaction to unlock, even if there's an existing transaction object
     * Used to rollback the transaction when the transaction object is returned with an error
     *
     * @param write - Writable transaction?
     * @param force - Force transaction to rollback?
     */
    private void unlockAndAbort(boolean write, boolean force) {
        if (force) {
            logger.trace("Forcing rollback of transaction");
        }
//        If there's no exisiting transaction object, or the transaction is being forced closed
        if (threadTransactionObject.get() == null || force) {
            logger.trace("Unlocking and rolling-back");
            unlock();
            logger.trace("Trying to rollback transaction");
            abortTransaction(write, force);
            logger.trace("Rolled-back transaction");
        } else {
            logger.trace("Thread owned by transaction object, not unlocking or rolling-back");
        }
    }

    /**
     * Open transaction
     *
     * @param write - Open a writable transaction
     */
    @Override
    public void openTransaction(boolean write) {
        this.openTransaction(write, Boolean.FALSE);
    }


    /**
     * Open transaction, optionally force it open, even if an existing transaction object is present
     *
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

                    this.threadInTransaction.set(Boolean.TRUE);
                    this.openedTransactions.incrementAndGet();
//                Track read/write transactions
                    if (write) {
                        this.openWriteTransactions.incrementAndGet();
                    } else {
                        this.openReadTransactions.incrementAndGet();
                    }
                    logger.debug(OPEN_READ_WRITE_TRANSACTIONS, this.openReadTransactions.get(), this.openWriteTransactions.get());
                    if (write) {
                        this.threadInWriteTransaction.set(Boolean.TRUE);
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
     *
     * @param write - Is this a write transaction?
     */
    @Override
    public void commitTransaction(boolean write) {
        this.commitTransaction(write, false);
    }

    /**
     * Commit transaction, optionally force closing it
     *
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
                    this.threadInTransaction.set(Boolean.FALSE);
                    this.threadTransactionInherited.set(Boolean.FALSE);
                    this.committedTransactions.incrementAndGet();
                    if (write) {
                        this.openWriteTransactions.decrementAndGet();
                    } else {
                        this.openReadTransactions.decrementAndGet();
                    }
                    logger.debug(OPEN_READ_WRITE_TRANSACTIONS, this.openReadTransactions.get(), this.openWriteTransactions.get());
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

    private void abortTransaction(boolean write, boolean force) {
        if (force) {
            logger.trace("Forcing rollback of transaction");
        }
        if (threadTransactionObject.get() == null || force) {
            if (!this.threadLocked.get() || force) {
                if (this.threadInTransaction.get()) {
                    logger.trace("Trying to rollback transaction");
                    this.abortDatasetTransaction(write);
                    logger.trace("Rolled-back dataset transaction");
                    this.threadInTransaction.set(Boolean.FALSE);
                    this.threadTransactionInherited.set(Boolean.FALSE);
                    this.abortedTransactions.incrementAndGet();
                    if (write) {
                        this.openWriteTransactions.decrementAndGet();
                    } else {
                        this.openReadTransactions.decrementAndGet();
                    }
                    logger.debug(OPEN_READ_WRITE_TRANSACTIONS, this.openReadTransactions.get(), this.openWriteTransactions.get());
                } else {
                    logger.trace("Thread unlocked, but not in transaction");
                }
            } else {
                logger.trace("Thread locked, not rolling-back");
            }
        } else {
            logger.trace("Thread owned by transaction object, not rolling-back");
        }
    }

    @Override
    public long getOpenedTransactionCount() {
        return this.openedTransactions.get();
    }

    @Override
    public long getCommittedTransactionCount() {
        return this.committedTransactions.get();
    }

    @Override
    public long getAbortedTransactionCount() {
        return this.abortedTransactions.get();
    }


    @Override
    public int getCurrentlyOpenTransactions() {
        return this.openReadTransactions.get() + this.openWriteTransactions.get();
    }

    @Override
    @Gauge(name = "trestle-open-write-transactions", absolute = true)
    public int getOpenWriteTransactions() {
        return this.openWriteTransactions.get();
    }

    @Override
    @Gauge(name = "trestle-open-read-transactions", absolute = true)
    public int getOpenReadTransactions() {
        return this.openReadTransactions.get();
    }


    @CounterIncrement(name = "trestle-opened-dataset-transactions", absolute = true)
    public abstract void openDatasetTransaction(boolean write);

    @CounterIncrement(name = "trestle-committed-dataset-transactions", absolute = true)
    public abstract void commitDatasetTransaction(boolean write);

    @CounterIncrement(name = "trestle-aborted-dataset-transactions", absolute = true)
    public abstract void abortDatasetTransaction(boolean write);

    /**
     * Set the thread repository connection from the TrestleTransaction object
     */
    public abstract void setOntologyConnection();

    /**
     * Get the thread repository connection to use with the TrestleTransaction object
     *
     * @return - RepositoryConnection for transaction
     */
    public abstract RepositoryConnection getOntologyConnection();

    protected @Nullable TrestleTransaction getThreadTransactionObject() {
        return this.threadTransactionObject.get();
    }
}

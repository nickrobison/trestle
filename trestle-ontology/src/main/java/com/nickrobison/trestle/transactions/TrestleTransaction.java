package com.nickrobison.trestle.transactions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Objects;

/**
 * Created by nrobison on 10/3/16.
 */
public class TrestleTransaction {

    private final @Nullable Long transactionID;
    private final Boolean writeTransaction;
    private volatile @Nullable RepositoryConnection connection;
    private final @Nullable TrestleTransaction parent;
    private final String openedThread;

    /**
     * Create a new TrestleTransaction with the current timestamp, indicating the tread owns the current transaction
     *
     * @param id    - Long of unix timestamp
     * @param write - Is this a write transaction?
     */
    public TrestleTransaction(Long id, boolean write) {
        this.transactionID = id;
        this.writeTransaction = write;
        this.openedThread = Thread.currentThread().getName();
        this.parent = null;
    }

    /**
     * Create TrestleTransaction object that does not own the current transaction
     *
     * @param parent - {@link TrestleTransaction} which this transaction is a child of
     * @param write - Is this a write transaction?
     */
    public TrestleTransaction(@Nullable TrestleTransaction parent, boolean write) {
        this.transactionID = null;
        this.writeTransaction = write;
        this.parent = parent;
        this.openedThread = Thread.currentThread().getName();
        if (parent != null) {
            this.connection = parent.getConnection();
        }
    }

    /**
     * Gets the ID of the transaction object, if the object doesn't own the transaction, then it returns 0
     *
     * @return - Long of transaction object
     */
    public Long getTransactionID() {
        return Objects.requireNonNullElse(this.transactionID, 0L);
    }

    /**
     * Determines if the transaction object owns a writable transaction
     * If the object doesn't own any transactions, this will be null and thus return false.
     *
     * @return - Writable transaction?
     */
    public Boolean isWriteTransaction() {
        return writeTransaction;
    }

    /**
     * Does the object own any transaction?
     * If the transactionID is null, than it doesn't own any transactions at all.
     *
     * @return - True is object owns any transactions
     */
    public boolean ownsATransaction() {
        return transactionID != null;
    }

    /**
     * Get the ontology RepositoryConnection to use for the remainder of the transaction
     *
     * @return - RepositoryConnection for current transaction
     */
    public @Nullable RepositoryConnection getConnection() {
        return connection;
    }

    public void setConnection(@Nullable RepositoryConnection connection) {
        this.connection = connection;
    }

    public TrestleTransaction getParent() {
        return parent;
    }

    public String getOpenedThread() {
        return openedThread;
    }

    public boolean isRoot() {
        return this.parent == null;
    }

    @Override
    @SuppressWarnings({"all"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrestleTransaction that = (TrestleTransaction) o;

        if (!getTransactionID().equals(that.getTransactionID())) return false;
        if (!writeTransaction.equals(that.writeTransaction)) return false;
        return getConnection() != null ? getConnection().equals(that.getConnection()) : that.getConnection() == null;
    }

    @Override
    public int hashCode() {
        int result = getTransactionID().hashCode();
        result = 31 * result + writeTransaction.hashCode();
        result = 31 * result + (getConnection() != null ? getConnection().hashCode() : 0);
        return result;
    }
}

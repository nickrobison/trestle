package com.nickrobison.trestle.transactions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Created by nrobison on 10/3/16.
 */
public class TrestleTransaction {

    private final @Nullable Long transactionID;
    private final Boolean writeTransaction;
    private volatile @Nullable RepositoryConnection connection;

    /**
     * Create a new TrestleTransaction with the current timestamp, indicating the tread owns the current transaction
     *
     * @param id    - Long of unix timestamp
     * @param write - Is this a write transaction?
     */
    public TrestleTransaction(Long id, boolean write) {
        this.transactionID = id;
        this.writeTransaction = write;
    }

    /**
     * Create TrestleTransaction object that does not own the current transaction
     *
     * @param write - Is this a write transaction?
     */
    public TrestleTransaction(boolean write) {
        this.transactionID = null;
        this.writeTransaction = write;
    }

    /**
     * Gets the ID of the transaction object, if the object doesn't own the transaction, then it returns 0
     *
     * @return - Long of transaction object
     */
    public Long getTransactionID() {
        if (this.transactionID == null) {
            return 0L;
        }
        return this.transactionID;
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
     * For a {@link com.nickrobison.trestle.ontology.JenaOntology}, the connection will always be null
     *
     * @return - RepositoryConnection for current transaction
     */
    public @Nullable RepositoryConnection getConnection() {
        return connection;
    }

    public void setConnection(@Nullable RepositoryConnection connection) {
        this.connection = connection;
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
    @SuppressWarnings({"all"})
    public int hashCode() {
        int result = getTransactionID().hashCode();
        result = 31 * result + writeTransaction.hashCode();
        result = 31 * result + (getConnection() != null ? getConnection().hashCode() : 0);
        return result;
    }
}

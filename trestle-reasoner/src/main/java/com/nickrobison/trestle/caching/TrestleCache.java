package com.nickrobison.trestle.caching;

import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Created by nrobison on 5/1/17.
 */
public interface TrestleCache {
    /**
     * Read, from cache, an available record that is valid for the specific temporal value encoded in the {@link TrestleIRI}
     *
     * @param clazz         - Generic class of to cast result to
     * @param individualIRI - {@link TrestleIRI} to retrieve
     * @param <T>           - Generic return type
     * @return - Cast object
     */
    <T> @Nullable T getTrestleObject(Class<T> clazz, TrestleIRI individualIRI);

    /**
     * Write TrestleObject
     *
     * @param individualIRI - {@link TrestleIRI} to add as key index/cache at a specific temporal interval
     * @param startTemporal - Start temporal from Unix epoch (ms)
     * @param endTemporal   - End temporal from Unix epoch (ms)
     * @param value         - Value to write to cache
     * @throws java.io.NotSerializableException if the value class isn't serializable (even if using a local cache)
     */
    void writeTrestleObject(TrestleIRI individualIRI, long startTemporal, long endTemporal, @NonNull Object value);

    /**
     * Write TrestleObject
     *
     * @param individualIRI - {@link TrestleIRI} to add as key index/cache at a specific temporal interval
     * @param atTemporal - At temporal from Unix epoch (ms)
     * @param value - Value to write to the cache
     */
    void writeTrestleObject(TrestleIRI individualIRI, long atTemporal, @NonNull Object value);

    /**
     * Delete TrestleObject from cache
     *
     * @param trestleIRI - {@link TrestleIRI} to remove from index and cache
     */
    void deleteTrestleObject(TrestleIRI trestleIRI);

    /**
     * Get {@link TrestleIndividual} from cache
     *
     * @param individual - {@link org.semanticweb.owlapi.model.OWLNamedIndividual} key to retrieve
     * @return - {@link TrestleIndividual}, if it exists
     */
    @Nullable TrestleIndividual getTrestleIndividual(OWLNamedIndividual individual);

    /**
     * Write {@link TrestleIndividual} into cache
     *
     * @param key   - {@link OWLNamedIndividual} to use as key
     * @param value - {@link TrestleIndividual} value
     */
    void writeTrestleIndividual(OWLNamedIndividual key, TrestleIndividual value);

    /**
     * Delete {@link TrestleIndividual} from cache
     *
     * @param individual - {@link OWLNamedIndividual} key to delete
     */
    void deleteTrestleIndividual(OWLNamedIndividual individual);

    void shutdown(boolean drop);
}

package com.nickrobison.trestle.reasoner.caching;

import com.nickrobison.trestle.iri.TrestleIRI;
import com.nickrobison.trestle.types.TrestleIndividual;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.OffsetDateTime;
import java.time.ZoneId;

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
     * Write TrestleObject to cache with a specified validity interval, using using {@link OffsetDateTime#now(ZoneId)} at {@link java.time.ZoneOffset#UTC}
     *
     * @param individualIRI - {@link TrestleIRI} to add as key index/cache at a specific temporal interval
     * @param startTemporal - {@link OffsetDateTime} of start temporal
     * @param endTemporal   - {@link OffsetDateTime} of end temporal
     * @param value         - Value to write to cache
     */
    void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal, Object value);


    /**
     * Write TrestleObject to cache with a specified validity interval
     *
     * @param individualIRI   - {@link TrestleIRI} to add as key index/cache at a specific temporal interval
     * @param startTemporal   - {@link OffsetDateTime} of start temporal
     * @param endTemporal     - {@link OffsetDateTime} of end temporal
     * @param dbStartTemporal - {@link OffsetDateTime} of database start temporal
     * @param dbEndTemporal   - {@link OffsetDateTime} of database end temporal
     * @param value           - Value to write to cache
     */
    void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime startTemporal, @Nullable OffsetDateTime endTemporal, OffsetDateTime dbStartTemporal, @Nullable OffsetDateTime dbEndTemporal, Object value);

    /**
     * Write TrestleObject to cache with a specified validity point, using {@link OffsetDateTime#now(ZoneId)} at {@link java.time.ZoneOffset#UTC} as the database temporal
     *
     * @param individualIRI - {@link TrestleIRI} to add as key index/cache at a specific temporal interval
     * @param atTemporal    - {@link OffsetDateTime} of validity point temporal
     * @param value         - Value to write to cache
     */
    void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime atTemporal, Object value);

    /**
     * Write TrestleObject to cache with a specified validity point
     *
     * @param individualIRI   - {@link TrestleIRI} to add as key index/cache at a specific temporal interval
     * @param atTemporal      - {@link OffsetDateTime} of validity point temporal
     * @param dbStartTemporal - {@link OffsetDateTime} of database start temporal
     * @param dbEndTemporal   - {@link OffsetDateTime} of database end temporal
     * @param value           - Value to write to cache
     */
    void writeTrestleObject(TrestleIRI individualIRI, OffsetDateTime atTemporal, OffsetDateTime dbStartTemporal, @Nullable OffsetDateTime dbEndTemporal, Object value);

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

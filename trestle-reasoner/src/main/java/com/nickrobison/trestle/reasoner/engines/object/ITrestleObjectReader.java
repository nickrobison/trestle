package com.nickrobison.trestle.reasoner.engines.object;

import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

/**
 * Created by nickrobison on 2/13/18.
 */

/**
 * Base interface for providing methods for reading an object from the underlying database
 * All methods require that the given Java classes be previously registered with the reasoner by calling {@link com.nickrobison.trestle.reasoner.TrestleReasoner#registerClass(Class)}
 * If methods are called with a non-registered class a {@link TrestleClassException} will be thrown
 */
public interface ITrestleObjectReader {
    /**
     * Returns an object from the database, looking up the class definition from the registry
     * Returns the currently valid facts, at the current database time
     *
     * @param datasetClassID - {@link String} name of Java class to retrieve from the class registry
     * @param objectID       - {@link String} string ID of individual
     * @param <T>            - Java class to return
     * @return - Java object of type {@link T}
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException;

    /**
     * Returns an object from the database, looking up the class definition from the registry
     * Allows the user to specify a valid/database pair for the desired object state
     *
     * @param <T>              - Java {@link Class} to return
     * @param datasetClassID   - {@link String} of Java class name to retrieve from the class registry
     * @param objectID         - {@link String} string ID of individual
     * @param validTemporal    - {@link Temporal} to denote the ValidAt time
     * @param databaseTemporal - Optional {@link Temporal} to denote the DatabaseAt time
     * @return - Java object of type {@link T}
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws MissingOntologyEntity, TrestleClassException;

    /**
     * Returns an object from the database, using the provided class definition.
     * Returns the currently valid facts, at the current database time
     *
     * @param clazz    - Java {@link Class} of type {@link T} to return
     * @param objectID - {@link String} ID of individual
     * @param <T>      - Java class to return
     * @return - Java object of type {@link T}
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Returns an object, from the database, using the provided class definition.
     * Allows the user to specify a valid/database pair for the desired object state
     *
     * @param clazz            - Java {@link Class} of type {@link T} to return
     * @param objectID         - {@link String} ID  of individual
     * @param validTemporal    - {@link Temporal} to denote the ValidAt time
     * @param databaseTemporal - Optional {@link Temporal} to denote the DatabaseAt time
     * @param <T>              - Java class to return
     * @return - Java object of type {@link T}
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws TrestleClassException, MissingOntologyEntity;

    /**
     * ReadAsObject interface, builds the default database temporal, optionally returns the object from the cache
     * Returns the currently valid facts, at the current database time
     *
     * @param clazz         - Java {@link Class} of type {@link T} to return
     * @param individualIRI - {@link IRI} ID of individual
     * @param bypassCache   - {@code true} bypass object cache. {@code false} use cache if possible
     * @param <T>           - Java {@link Class} to return
     * @return - Java object of type {@link T}
     */
    <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache);

    /**
     * /**
     * ReadAsObject interface, (optionally) building the database temporal and retrieving from the cache
     * Returns the state of the object at the specified valid/database point
     * If no valid or database times are specified, returns the currently valid facts at the current database time
     *
     * @param clazz         - Java {@link Class} of type {@link T} to return
     * @param individualIRI - {@link IRI} ID of individual
     * @param bypassCache   - {@code true} bypass object cache. {@code false} use cache if possible
     * @param validAt       - Optional {@link Temporal} to specify a validAt time
     * @param databaseAt    - Optional {@link Temporal} to specify databaseAt time
     * @param <T>           - Java {@link Class} to return
     * @return - Java object of type {@link T}
     */
    <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, IRI individualIRI, boolean bypassCache, @Nullable Temporal validAt, @Nullable Temporal databaseAt);

    /**
     * Retrieve historical states of a given Fact
     * Returns an optional list of Java Objects that match the datatype of the given Fact
     * Allows for optional temporal filter to restrict results to only Fact states valid during the provided temporal window
     *
     * @param clazz            - Java {@link Class} to retrieve from the class registry
     * @param individual       - {@link String} ID of individual
     * @param factName         - {@link String} name of Fact
     * @param validStart       - Optional {@link Temporal} setting the start of the temporal filter
     * @param validEnd         - Optional {@link Temporal} setting the end of the temporal filter
     * @param databaseTemporal - Optional {@link Temporal} filtering results to only certain fact versions
     * @return - {@link Optional} {@link List} of Java {@link Object}
     * @throws IllegalArgumentException - Throws if the given Fact name does not exist on the dataset
     */
    List<Object> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal);

    /**
     * Retrieve historical states of a given Fact
     * Returns an optional list of Java Objects that match the datatype of the given Fact
     * Allows for optional temporal filter to restrict results to only Fact states valid during the provided temporal window
     *
     * @param clazz            - Java {@link Class} to retrieve from the class registry
     * @param individual       - {@link OWLNamedIndividual} of individual ID
     * @param factName         - {@link OWLDataProperty}
     * @param validStart       - Optional {@link Temporal} setting the start of the temporal filter
     * @param validEnd         - Optional {@link Temporal} setting the end of the temporal filter
     * @param databaseTemporal - Optional {@link Temporal} filtering results to only certain fact versions
     * @return - {@link Optional} {@link List} of Java {@link Object}
     * @throws IllegalArgumentException - Throws if the given Fact name does not exist on the dataset
     */
    List<Object> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal);


    /**
     * Returns a sampling of unique values for a given property in a Dataset
     *
     * @param clazz       - Java {@link Class} to retrieve from the class registry
     * @param factName    - {@link String} name of the Fact
     * @param sampleLimit - {@link Long} maximum number of unique values to return
     * @return - {@link Optional} {@link List} of Java {@link Object}
     */
    List<Object> sampleFactValues(Class<?> clazz, String factName, long sampleLimit);

    List<Object> sampleFactValues(Class<?> clazz, OWLDataProperty factName, long sampleLimit);
}

package com.nickrobison.trestle.reasoner.engines.object;

import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

/**
 * Created by nickrobison on 2/13/18.
 */
public interface ITrestleObjectReader {
    /**
     * Returns an object, from the database, looking up the class definition from the registry
     *
     * @param datasetClassID - String of class name to retrieve from the class registry
     * @param objectID       - IRI string of individual
     * @param <T>            - Java class to return
     * @return - Java object of type T
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID) throws MissingOntologyEntity, TrestleClassException;

    /**
     * Returns an object, from the database, looking up the class definition from the registry
     *
     * @param <T>              - Java class to return
     * @param datasetClassID   - String of class name to retrieve from the class registry
     * @param objectID         - IRI string of individual
     * @param validTemporal    - Temporal to denote the ValidAt point
     * @param databaseTemporal - Optional Temporal to denote the DatabaseAt point
     * @return - Java object of type T
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(String datasetClassID, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws MissingOntologyEntity, TrestleClassException;

    /**
     * Returns an object, from the database, using the provided class definition.
     * Returns the currently valid facts, at the current database time
     *
     * @param clazz    - Java class definition of return object
     * @param objectID - IRI string of individual
     * @param <T>      - Java class to return
     * @return - Java object of type T
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Returns an object, from the database, using the provided class definition.
     * Allows the user to specify a valid/database pair to specified desired object state
     *
     * @param clazz            - Java class definition of return object
     * @param objectID         - IRI string of individual
     * @param validTemporal    - Temporal to denote the ValidAt point
     * @param databaseTemporal - Optional Temporal to denote the DatabaseAt point
     * @param <T>              - Java class to return
     * @return - Java object of type T
     * @throws MissingOntologyEntity - throws if the given individual isn't in the Database
     * @throws TrestleClassException - throws if the class isn't registered with the Reasoner
     */
    <T extends @NonNull Object> T readTrestleObject(Class<T> clazz, String objectID, @Nullable Temporal validTemporal, @Nullable Temporal databaseTemporal) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Retrieve historical states of a given Fact
     * Returns an optional list of Java Objects that match the datatype of the given Fact
     * Allows for optional temporal filter to restrict results to only Fact states valid during the provided temporal window
     *
     * @param clazz            - Java class to parse
     * @param individual       - Individual ID
     * @param factName         - Name of Fact
     * @param validStart       - Optional Temporal setting the start of the temporal filter
     * @param validEnd         - Optional Temporal setting the end of the temporal filter
     * @param databaseTemporal - Optional temporal filtering results to only certain fact versions
     * @return - Optional list of Java Objects
     */
    Optional<List<Object>> getFactValues(Class<?> clazz, String individual, String factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal);

    /**
     * Retrieve historical states of a given Fact
     * Returns an optional list of Java Objects that match the datatype of the given Fact
     * Allows for optional temporal filter to restrict results to only Fact states valid during the provided temporal window
     *
     * @param clazz            - Java class to parse
     * @param individual       - {@link OWLNamedIndividual} of individual ID
     * @param factName         - {@link OWLDataProperty}
     * @param validStart       - Optional Temporal setting the start of the temporal filter
     * @param validEnd         - Optional Temporal setting the end of the temporal filter
     * @param databaseTemporal - Optional temporal filtering results to only certain fact versions
     * @return - Optional List of Java Objects
     */
    Optional<List<Object>> getFactValues(Class<?> clazz, OWLNamedIndividual individual, OWLDataProperty factName, @Nullable Temporal validStart, @Nullable Temporal validEnd, @Nullable Temporal databaseTemporal);
}

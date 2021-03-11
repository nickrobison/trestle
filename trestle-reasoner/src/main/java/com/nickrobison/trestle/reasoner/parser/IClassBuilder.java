package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.exceptions.MissingConstructorException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IClassBuilder {

    /**
     * Parses out the data properties for a given input class
     * Uses the provided {@link String} prefix for the properties
     * Filters out spatial members
     *
     * @param clazz - {@link Class} to parse
     * @return - {@link Optional} {@link List} of {@link OWLDataProperty} for given class
     */
    Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz);

    /**
     * Parses out the data properties for a given input class
     * Uses the provided {@link String} prefix for the properties
     *
     * @param clazz         - {@link Class} to parse
     * @param filterSpatial - filter out spatial properties?
     * @return - {@link Optional} {@link List} of {@link OWLDataProperty} for given class
     */
    Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz, boolean filterSpatial);

    /**
     * Parses out the object properties for a given input class
     *
     * @param clazz - {@link Class to parse}
     * @return - {@link Set} of {@link OWLObjectProperty} for given class
     */
    Set<OWLObjectProperty> getObjectPropertyMembers(Class<?> clazz);

    /**
     * /**
     * Creates object of type {@link T} from given class definition using the provided {@link ConstructorArguments}
     *
     * @param clazz     - {@link Class} clazz to build from
     * @param arguments - {@link ConstructorArguments} to use
     * @param <T>       - Type parameter
     * @return - {@link T} constructed object
     * @throws MissingConstructorException if unable to match constructor with input params
     */

    <T> T constructObject(Class<T> clazz, ConstructorArguments arguments) throws MissingConstructorException;

    /**
     * Creates a projected WKT string using either the provided srid, or the projection specified by the class
     *
     * @param clazz         - {@link Class} registered with reasoner
     * @param spatialObject - {@link Object} representing spatial value to parse
     * @param srid          - {@link Integer} optional SRID to manually specify projection
     * @return - {@link OWLLiteral} of projected WKT value
     */
    OWLLiteral getProjectedWKT(Class<?> clazz, Object spatialObject, @Nullable Integer srid);
}

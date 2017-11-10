package com.nickrobison.trestle.reasoner.parser;

import org.semanticweb.owlapi.model.OWLDataProperty;

import java.util.List;
import java.util.Optional;

public interface IClassBuilder {

    /**
     * Parses out the data properties for a given input class
     * Uses the provided {@link String} prefix for the properties
     * Filters out spatial members
     *
     * @param clazz  - {@link Class} to parse
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
     * Creates object of type {@link T} from given class definition using the provided {@link ConstructorArguments}
     *
     * @param clazz     - {@link Class} clazz to build from
     * @param arguments - {@link ConstructorArguments} to use
     * @param <T>       - Type parameter
     * @return - {@link T} constructed object
     */
    <T> T constructObject(Class<T> clazz, ConstructorArguments arguments);
}

package com.nickrobison.trestle.reasoner.parser;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.Optional;

public interface IClassParser {

    Object parseClass(Class<?> clazz);
    /**
     * Is the current {@link ClassParser} multi-language enabled?
     * @return - {@code true} multi-language support enabled. {@code false} multi-language support is disabled.
     */
    boolean isMultiLangEnabled();

    /**
     * Return the default language code used by the parser.
     * Returns null if no default is specified
     * @return - {@link String} representing the default ISO language code
     */
    @Nullable String getDefaultLanguageCode();

    /**
     * Get the {@link OWLClass} representation of the given input {@link Object}
     * Note: The method does not utilize the class cache
     * @param inputObject - {@link Object} to determine class of
     * @return - {@link OWLClass}
     */
    OWLClass getObjectClass(Object inputObject);

    /**
     * Get {@link OWLClass} from given Java {@link Class}
     * @param clazz - {@link Class} to parser
     * @return - {@link OWLClass}
     */
    OWLClass getObjectClass(Class<?> clazz);

    /**
     * Get {@link OWLNamedIndividual} for given input {@link Object}
     * @param inputObject - {@link Object} to parse
     * @return - {@link OWLNamedIndividual}
     */
    OWLNamedIndividual getIndividual(Object inputObject);

    /**
     * Extract the {@link OWLDataPropertyAssertionAxiom} from a given object
     * Return {@link Optional#empty()} if the input object cannot be parsed
     * @param inputObject - {@link Object} to parse
     * @return - {@link Optional} {@link List} of {@link OWLDataPropertyAssertionAxiom}s
     */
    Optional<List<OWLDataPropertyAssertionAxiom>> getFacts(Object inputObject);

    /**
     * Extract the {@link OWLDataPropertyAssertionAxiom} from a given object
     * Return {@link Optional#empty()} if the input object cannot be parsed
     * @param inputObject - {@link Object} to parse
     * @param filterSpatial - {@code true} filter out the spatial annotations from return set. {@code false} returns spatial annotations as well
     * @return - {@link Optional} {@link List} of {@link OWLDataPropertyAssertionAxiom}s
     */
    Optional<List<OWLDataPropertyAssertionAxiom>> getFacts(Object inputObject, boolean filterSpatial);

    /**
     * Extract the spatial property a given object
     * @param inputObject - {@link Object} to parse for spatial property
     * @return - {@link Optional} of {@link OWLDataPropertyAssertionAxiom} representing spatial property
     */
    Optional<OWLDataPropertyAssertionAxiom> getSpatialFact(Object inputObject);

    /**
     * Match a given String, representing the short-form of a {@link OWLDataProperty} {@link IRI} with the appropriate class member
     * If the languageCode param is not null, the method attempts to match the correct data property and language pair.
     * @param clazz - Java {@link Class} to parse
     * @param classMember - {@link IRI} short-form to match against class
     * @param languageTag - Nullable languageCode determining multi-lang String support required
     * @return - {@link String} name of matching class member
     */
    String matchWithClassMember(Class<?> clazz, String classMember, @Nullable String languageTag);

    /**
     * Match a given {@link String}, representing the short-form of a {@link OWLDataProperty} {@link IRI} with the appropriate class member
     * @param clazz - Java {@link Class} to parse
     * @param classMember - {@link IRI} short-form to match against class
     * @return - {@link String} name of matching class member
     */
    String matchWithClassMember(Class<?> clazz, String classMember);

    /**
     * Get the datatype of the fact represented in the given string as an Java {@link Class}
     * @param clazz - {@link Class} to parse
     * @param factName - {@link String} name of fact
     * @return - {@link Optional} {@link Class} of return datatype
     */
    Optional<Class<@NonNull ?>> getFactDatatype(Class<?> clazz, String factName);

    /**
     * Get the correct Fact {@link IRI} from a given string
     * Gets the correct {@link IRI} prefix to handle things like Spatial members and user defined types
     * @param clazz - Java {@link Class} to parse
     * @param factName - Name of fact to build IRI for
     * @return - {@link Optional} {@link IRI} of fact
     */
    Optional<IRI> getFactIRI(Class<?> clazz, String factName);
}

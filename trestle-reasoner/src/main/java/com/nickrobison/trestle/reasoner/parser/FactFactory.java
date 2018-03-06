package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.types.TrestleFact;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;

import javax.inject.Inject;

/**
 * Created by nickrobison on 3/5/18.
 */
public class FactFactory {

    private final ITypeConverter typeConverter;

    @Inject
    public FactFactory(ITypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }


    /**
     * Construct an {@link TrestleFact}
     *
     * @param identifier       - {@link String} ID of Fact
     * @param name             - {@link String} name of Fact
     * @param value            - Value of type {@link T}
     * @param validTemporal    - {@link TemporalObject} valid temporal
     * @param databaseTemporal - {@link TemporalObject} database temporal
     * @param <T>              - Generic type parameter
     * @return - {@link TrestleFact}
     */
    public <T extends @NonNull Object> TrestleFact<T> createFact(String identifier, String name, T value, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        return new TrestleFact<>(identifier, name, value, null, validTemporal, databaseTemporal);
    }

    public <T extends @NonNull Object> TrestleFact<T> createFact(@Nullable Class<?> clazz, OWLDataPropertyAssertionAxiom propertyAxiom, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        final Class<?> aClass = this.typeConverter.lookupJavaClassFromOWLDatatype(propertyAxiom, clazz);
        final OWLLiteral literal = propertyAxiom.getObject();
        //noinspection unchecked
        final T literalObject = this.typeConverter.extractOWLLiteral((Class<T>) aClass, literal);

        return new TrestleFact<>(propertyAxiom.getSubject().toStringID(),
                propertyAxiom.getProperty().asOWLDataProperty().getIRI().getShortForm(),
                literalObject,
                literal.hasLang() ? literal.getLang() : null,
                validTemporal,
                databaseTemporal);
    }
}

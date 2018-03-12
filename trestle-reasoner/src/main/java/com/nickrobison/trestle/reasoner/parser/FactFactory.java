package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.types.TrestleFact;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Created by nickrobison on 3/5/18.
 */
public class FactFactory {

    private final IClassParser parser;
    private final ITypeConverter typeConverter;

    @Inject
    public FactFactory(ITypeConverter typeConverter, IClassParser parser) {
        this.typeConverter = typeConverter;
        this.parser = parser;
    }

    /**
     * Construct a {@link TrestleFact}
     *
     * @param clazz            - {@link Class} type of Fact
     * @param propertyAxiom    - {@link OWLDataPropertyAssertionAxiom} of literal value and type
     * @param validTemporal    - {@link TemporalObject} valid temporal
     * @param databaseTemporal - {@link TemporalObject} of database temporal
     * @param <T>              - {@link T} generic type parameter
     * @return - {@link TrestleFact}
     */
    public <T extends @NonNull Object> TrestleFact<T> createFact(@Nullable Class<?> clazz, OWLDataPropertyAssertionAxiom propertyAxiom, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        final OWLDataProperty factName = propertyAxiom.getProperty().asOWLDataProperty();
        final Optional<Class<?>> factDatatype = this.parser.getFactDatatype(clazz, factName.toStringID());
        //noinspection unchecked
        final Class<T> factClass = (Class<T>) this.typeConverter.lookupJavaClassFromOWLDatatype(propertyAxiom, factDatatype.orElseThrow(() ->
                new IllegalStateException(String.format("Cannot have null datatype for fact %s on Individual %s",
                        factName, propertyAxiom.getSubject()))));
        final OWLLiteral literal = propertyAxiom.getObject();

//        Get the projection of the class and re-project, if necessary
        final Integer classProjection = this.parser.getClassProjection(clazz);
//        Guard against null projections
        final T literalObject;
        if (classProjection == null) {
            literalObject = this.typeConverter.extractOWLLiteral(factClass, literal);
        } else {
            literalObject = this.typeConverter.reprojectSpatial(this.typeConverter.extractOWLLiteral(factClass, literal), classProjection);
        }

        return new TrestleFact<>(propertyAxiom.getSubject().toStringID(),
                factName.getIRI().getShortForm(),
                literalObject,
                factClass,
                literal.hasLang() ? literal.getLang() : null,
                validTemporal,
                databaseTemporal);
    }
}

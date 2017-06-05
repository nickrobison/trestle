package com.nickrobison.trestle.reasoner.types;

import com.nickrobison.trestle.reasoner.parser.TypeConverter;
import com.nickrobison.trestle.reasoner.types.temporal.TemporalObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;

import java.io.Serializable;

/**
 * Created by nrobison on 10/16/16.
 */
public class TrestleFact<T> implements Serializable {
    private static final long serialVersionUID = 42L;

    private final String identifier;
    private final String name;
    private final T value;
    private final TemporalObject validTemporal;
    private final TemporalObject databaseTemporal;
    private final @Nullable String language;
    private final Class<?> javaClass;

    @SuppressWarnings({"dereference.of.nullable"})
    public TrestleFact(String identifier, String name, T value, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        this.identifier = identifier;
        this.name = name;
        this.value = value;
        this.validTemporal = validTemporal;
        this.databaseTemporal = databaseTemporal;
        this.language = null;
        this.javaClass = value.getClass();
    }

    public TrestleFact(@Nullable Class<?> clazz, OWLDataPropertyAssertionAxiom propertyAxiom, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        this.validTemporal = validTemporal;
        this.databaseTemporal = databaseTemporal;
        this.identifier = propertyAxiom.getSubject().toStringID();
        this.name = propertyAxiom.getProperty().asOWLDataProperty().getIRI().getShortForm();
        final Class<@NonNull ?> factClass = TypeConverter.lookupJavaClassFromOWLDatatype(propertyAxiom, clazz);
        final OWLLiteral literal = propertyAxiom.getObject();
        final Object literalObject = TypeConverter.extractOWLLiteral(factClass, literal);
//        TODO(nrobison): This feels terrible, what else should I do?
        this.value = (T) literalObject;
        if (literal.hasLang()) {
            this.language = literal.getLang();
        } else {
            this.language = null;
        }
        this.javaClass = factClass;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() { return name; }

    public T getValue() {
        return value;
    }

    /**
     * Returns the language of the Fact, if its a multi-language string
     * @return - Optional of Fact language
     */
    public @Nullable String getLanguage() {
        return this.language;
    }

    public Class<?> getJavaClass() {
        return this.javaClass;
    }

    public TemporalObject getValidTemporal() {
        return validTemporal;
    }

    public TemporalObject getDatabaseTemporal() {
        return databaseTemporal;
    }

}

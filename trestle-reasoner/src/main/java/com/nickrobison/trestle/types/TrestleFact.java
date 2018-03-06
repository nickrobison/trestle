package com.nickrobison.trestle.types;

import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.checkerframework.checker.nullness.qual.Nullable;

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
    public TrestleFact(String identifier, String name, T value, @Nullable String language, TemporalObject validTemporal, TemporalObject databaseTemporal) {
        this.identifier = identifier;
        this.name = name;
        this.value = value;
        this.validTemporal = validTemporal;
        this.databaseTemporal = databaseTemporal;
        this.language = language;
        this.javaClass = value.getClass();
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    /**
     * Returns the language of the Fact, if its a multi-language string
     *
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

    @Override
    public String toString() {
        return "TrestleFact{" +
                "identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", value=" + value +
                ", validTemporal=" + validTemporal +
                ", databaseTemporal=" + databaseTemporal +
                ", language='" + language + '\'' +
                ", javaClass=" + javaClass +
                '}';
    }
}

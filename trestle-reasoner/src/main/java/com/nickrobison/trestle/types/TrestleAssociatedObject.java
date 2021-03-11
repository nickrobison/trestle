package com.nickrobison.trestle.types;

import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.Objects;

/**
 * Created by nickrobison on 3/11/21
 * <p>
 * Thin wrapper associating an {@link org.semanticweb.owlapi.model.OWLObjectProperty} with the instantiated {@link Object} it points to
 */
public class TrestleAssociatedObject<T> {

    private final OWLObjectProperty property;
    private final T object;

    public TrestleAssociatedObject(OWLObjectProperty property, T object) {
        this.property = property;
        this.object = object;
    }

    public OWLObjectProperty getProperty() {
        return property;
    }

    public T getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrestleAssociatedObject<?> that = (TrestleAssociatedObject<?>) o;
        return property.equals(that.property) && object.equals(that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, object);
    }
}

package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 6/30/16.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class TemporalObject implements Serializable {
    private static final long serialVersionUID = 42L;

    private final String id;
    private final Set<OWLNamedIndividual> temporal_of;

    TemporalObject(String id, Optional<Set<OWLNamedIndividual>> relations) {
        this.id = id;
        this.temporal_of = relations.orElse(new HashSet<>());
    }

    public String getID() {
        return this.id;
    }

    public abstract boolean isInterval();

    public IntervalTemporal asInterval() {
        return (IntervalTemporal) this;
    }

    public abstract boolean isPoint();

    public PointTemporal asPoint() {
        return (PointTemporal) this;
    }

    public abstract TemporalType getType();

    public abstract TemporalScope getScope();

    public abstract boolean isValid();

    public abstract boolean isExists();

    public abstract boolean isDatabase();

    /**
     * Determines whether the temporal object represents a continuing (unbounded) interval.
     * For {@link PointTemporal} this will always return false
     *
     * @return - <code>true</code> temporal is continuing. <code>false</code> is not continuing
     */
    public abstract boolean isContinuing();

    /**
     * Returns the temporal used as an identifier
     * For an {@link IntervalTemporal}, this is the startTemporal
     * For a {@link PointTemporal}, it is the atTemporal
     *
     * @return - {@link Temporal} to use for identification
     */
    public abstract Temporal getIdTemporal();

    /**
     * Get the Base temporal type of the object
     *
     * @return - Temporal subclass of object
     */
    public abstract Class<? extends Temporal> getBaseTemporalType();

    /**
     * Cast TemporalObject to a different temporal scope
     * Always results in a new object even if the TemporalObject has the same temporal scope
     *
     * @param castScope - TemporalScope to cast object to
     * @return - new TemporalObject
     */
    public abstract TemporalObject castTo(TemporalScope castScope);

    /**
     * Compares a given {@link Temporal} with the {@link TemporalObject} to determine if the given {@link Temporal} is before, during, or after the {@link TemporalObject}
     *
     * @param comparingTemporal - {@link Temporal} to compare against the temporal object
     * @return - {@code -1} if this {@link TemporalObject} comes before the {@link Temporal}, {@code 0} if the {@link Temporal} occurs during (or is equal to), {@code 1} if the {@link TemporalObject} comes after
     */
    public abstract int compareTo(Temporal comparingTemporal);

    /**
     * Compares two temporal objects to determine if this object occurs during the given object
     *
     * @param comparingObject - {@link TemporalObject} object to compare against this one
     * @return - {@code true} if this object occurs entirely within the given object
     */
    public abstract boolean during(TemporalObject comparingObject);

    /**
     * Compares 2 {@link TemporalObject} to determine if this object starts the given object
     * Is only possible if this object occurs DURING the given object
     * <p>
     * Note: {@link PointTemporal} will always return {@code false}
     *
     * @param comparingObject - {@link TemporalObject} to determine is started by
     * @return - {@code true} this object starts the given object, {@code false} does not start the given object
     */
    public abstract boolean starts(TemporalObject comparingObject);

    /**
     * Compares 2 {@link TemporalObject} to determine if this object FINISHES the given object
     * Is only possible if this object occurs DURING the given object
     * <p>
     * Note: {@link PointTemporal} will always return {@code false}
     *
     * @param comparingObject - {@link TemporalObject} to determine is started by
     * @return - {@code true} this object starts the given object, {@code false} does not start the given object
     */
    public abstract boolean finishes(TemporalObject comparingObject);

    /**
     * Compares 2 {@link TemporalObject} to determine if this object MEETS the given object
     *
     * @param comparingObject - {@link TemporalObject} to determine meets
     * @return - {@code true} this object starts the given object, {@code false} does not start the given object
     */
    public abstract boolean meets(TemporalObject comparingObject);

    @Override
    public abstract boolean equals(@Nullable Object obj);

    @Override
    public abstract int hashCode();

}

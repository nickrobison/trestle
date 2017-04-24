package com.nickrobison.trestle.types.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 6/30/16.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class TemporalObject {

    private final String id;
    private final Optional<Set<OWLNamedIndividual>> temporal_of;

    TemporalObject(String id, Optional<Set<OWLNamedIndividual>> relations) {
        this.id = id;
        this.temporal_of = relations;
    }

    @Deprecated
    public String getID() {
        return this.id;
    }

    @Deprecated
    public Set<OWLNamedIndividual> getTemporalRelations() {
        if (this.temporal_of.isPresent()) {
            return this.temporal_of.get();
        } else {
            return new HashSet<>();
        }
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
     * Returns the temporal used as an identifier
     * For an {@link IntervalTemporal}, this is the startTemporal
     * For a {@link PointTemporal}, it is the atTemporal
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
     * @param castScope - TemporalScope to cast object to
     * @return - new TemporalObject
     */
    public abstract TemporalObject castTo(TemporalScope castScope);

    /**
     * Compares a temporal with the TemporalObject to determine if the given Temporal is before, during, or after the {@link TemporalObject}
     * @param comparingTemporal - {@link OffsetDateTime} to compare against the temporal object
     * @return - {@code -1} if the {@link OffsetDateTime} comes before the {@link TemporalObject}, {@code 0} is it occurs during (or at), {@code 1} if it comes after
     */
    public abstract int compareTo(OffsetDateTime comparingTemporal);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemporalObject that = (TemporalObject) o;

        return id.equals(that.id) && temporal_of.equals(that.temporal_of);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + temporal_of.hashCode();
        return result;
    }
}

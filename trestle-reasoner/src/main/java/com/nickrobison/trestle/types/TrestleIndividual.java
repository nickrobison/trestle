package com.nickrobison.trestle.types;

import com.nickrobison.trestle.types.events.TrestleEvent;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 10/16/16.
 */
public class TrestleIndividual implements Serializable {
    private static final long serialVersionUID = 42L;

    private final String individualID;
    private final List<TrestleFact> facts;
    private final TemporalObject existsTemporal;
    private final List<TrestleRelation> relations;
    private final List<TrestleEvent> events;

    public TrestleIndividual(String id, TemporalObject existsTemporal) {
        this.facts = new ArrayList<>();
        this.individualID = id;
        this.existsTemporal = existsTemporal;
        this.relations = new ArrayList<>();
        this.events = new ArrayList<>();
    }

    /**
     * Add a fact to the individual
     *
     * @param fact - TrestleFact to add
     */
    public void addFact(TrestleFact fact) {
        if (this.facts.contains(fact)) {
            return;
        }
        this.facts.add(fact);
    }

    /**
     * Add {@link TrestleRelation} to the individual
     *
     * @param relation - {@link TrestleRelation} to add
     */
    public void addRelation(TrestleRelation relation) {
        if (this.relations.contains(relation)) {
            return;
        }
        this.relations.add(relation);
    }

    /**
     * Add {@link TrestleEvent} to individual
     *
     * @param event - {@link TrestleEvent} to add
     */
    public void addEvent(TrestleEvent event) {
        if (this.events.contains(event)) {
            return;
        }
        this.events.add(event);
    }

    public List<TrestleEvent> getEvents() {
        return this.events;
    }

    public String getIndividualID() {
        return this.individualID;
    }

    public List<TrestleFact> getFacts() {
        return this.facts;
    }

    public TemporalObject getExistsTemporal() {
        return this.existsTemporal;
    }

    public Set<TemporalObject> getTemporals() {
        final Set<TemporalObject> attributeTemporals = facts
                .stream()
                .map(TrestleFact::getValidTemporal)
                .collect(Collectors.toSet());
        attributeTemporals.add(this.existsTemporal);

        return attributeTemporals;
    }

    public Set<TemporalObject> getDatabaseTemporals() {
        return facts
                .stream()
                .map(TrestleFact::getDatabaseTemporal)
                .collect(Collectors.toSet());
    }

    public List<TrestleRelation> getRelations() {
        return this.relations;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrestleIndividual that = (TrestleIndividual) o;

        if (!getIndividualID().equals(that.getIndividualID())) return false;
        if (!getFacts().equals(that.getFacts())) return false;
        if (!getExistsTemporal().equals(that.getExistsTemporal())) return false;
        if (!getRelations().equals(that.getRelations())) return false;
        return getEvents().equals(that.getEvents());
    }

    @Override
    public int hashCode() {
        int result = getIndividualID().hashCode();
        result = 31 * result + getFacts().hashCode();
        result = 31 * result + getExistsTemporal().hashCode();
        result = 31 * result + getRelations().hashCode();
        result = 31 * result + getEvents().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TrestleIndividual{" +
                "individualID='" + individualID + '\'' +
                ", facts=" + facts +
                ", existsTemporal=" + existsTemporal +
                ", relations=" + relations +
                ", events=" + events +
                '}';
    }
}

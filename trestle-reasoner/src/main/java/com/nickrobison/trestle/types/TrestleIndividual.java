package com.nickrobison.trestle.types;

import com.nickrobison.trestle.types.temporal.TemporalObject;

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

    public TrestleIndividual(String id, TemporalObject existsTemporal) {
        this.facts = new ArrayList<>();
        this.individualID = id;
        this.existsTemporal = existsTemporal;
        this.relations = new ArrayList<>();
    }

    /**
     * Add a fact to the individual
     * @param fact - TrestleFact to add
     */
    public void addFact(TrestleFact fact) {
        if (this.facts.contains(fact)) {
            return;
        }
        this.facts.add(fact);
    }

    /**
     * Add relation to the individual
     * @param relation - TrestleRelation to add
     */
    public void addRelation(TrestleRelation relation) {
        if (this.relations.contains(relation)) {
            return;
        }
        this.relations.add(relation);
    }

    public String getIndividualID() { return this.individualID;}

    public List<TrestleFact> getFacts() { return this.facts;}

    public TemporalObject getExistsTemporal() { return this.existsTemporal;}

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
}

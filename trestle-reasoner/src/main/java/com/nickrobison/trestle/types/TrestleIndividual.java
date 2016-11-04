package com.nickrobison.trestle.types;

import com.nickrobison.trestle.types.temporal.TemporalObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 10/16/16.
 */
public class TrestleIndividual {

    private final String individualID;
    private final List<TrestleFact> facts;
    private final TemporalObject validTemporal;

    public TrestleIndividual(String id, TemporalObject validTemporal) {
        this.facts = new ArrayList<>();
        this.individualID = id;
        this.validTemporal = validTemporal;
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

    public String getIndividualID() { return this.individualID;}

    public List<TrestleFact> getFacts() { return this.facts;}

    public TemporalObject getValidTemporal() { return this.validTemporal;}

    public Set<TemporalObject> getTemporals() {
        final Set<TemporalObject> attributeTemporals = facts
                .stream()
                .map(TrestleFact::getValidTemporal)
                .collect(Collectors.toSet());
        attributeTemporals.add(this.validTemporal);

        return attributeTemporals;
    }

    public Set<TemporalObject> getDatabaseTemporals() {
        return facts
                .stream()
                .map(TrestleFact::getDatabaseTemporal)
                .collect(Collectors.toSet());
    }
}

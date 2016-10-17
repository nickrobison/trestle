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
    private final List<TrestleAttribute> attributes;
    private final TemporalObject validTemporal;

    public TrestleIndividual(String id, TemporalObject validTemporal) {
        this.attributes = new ArrayList<>();
        this.individualID = id;
        this.validTemporal = validTemporal;
    }

    /**
     * Add an attribute to the individual
     * @param attribute - TrestleAttribute to add
     */
    public void addAttribute(TrestleAttribute attribute) {
        if (this.attributes.contains(attribute)) {
            return;
        }
        this.attributes.add(attribute);
    }

    public Set<TemporalObject> getTemporals() {
        final Set<TemporalObject> attributeTemporals = attributes
                .stream()
                .map(TrestleAttribute::getValidTemporal)
                .collect(Collectors.toSet());
        attributeTemporals.add(this.validTemporal);

        return attributeTemporals;
    }

    public Set<TemporalObject> getDatabaseTemporals() {
        return attributes
                .stream()
                .map(TrestleAttribute::getDatabaseTemporal)
                .collect(Collectors.toSet());
    }
}

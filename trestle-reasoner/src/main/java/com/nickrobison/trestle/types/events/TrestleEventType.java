package com.nickrobison.trestle.types.events;

import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;

import java.util.Arrays;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Exhaustive list of Trestle Events
 */
public enum TrestleEventType implements HasIRI {
    CREATED("created", "Created_Event"),
    DESTROYED("destroyed", "Destroyed_Event"),
    BECOMES("becomes", "Becomes_Event"),
    SPLIT("splits", "Split_Event"),
    MERGE("merges", "Merged_Event");

    private final IRI relationIRI;
    private final IRI classIRI;
    private final String shortName;

    TrestleEventType(String relationString, String classString) {
        this.relationIRI = IRI.create(TRESTLE_PREFIX, relationString);
        this.classIRI = IRI.create(TRESTLE_PREFIX, classString);
        this.shortName = relationString;
    }


    @Override
    public IRI getIRI() {
        return relationIRI;
    }

    public IRI getClassIRI() {
        return this.classIRI;
    }

    /**
     * Get the {@link IRI} of the event type, as a string
     *
     * @return - {@link String} of {@link IRI}
     */
    public String getIRIString() {
        return this.relationIRI.getIRIString();
    }

    /**
     * Get the short name of the event (owl:relation)
     *
     * @return {@link String} name
     */
    public String getShortName() {
        return this.shortName;
    }

    /**
     * Find the TrestleEventType (owl:Class) that matches the given IRI
     * throw {@link IllegalArgumentException} if the given IRI doesn't match any known relations
     *
     * @param eventIRI - Class IRI to match on
     * @return - ObjectRelation that matches the given IRI
     */
    public static TrestleEventType getEventClassFromIRI(IRI eventIRI) {
        return Arrays.stream(TrestleEventType.values())
                .filter(event -> event.getClassIRI().equals(eventIRI))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find EventType for IRI %s", eventIRI.getIRIString())));
    }
}

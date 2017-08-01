package com.nickrobison.trestle.types;

import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;

import java.util.Arrays;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Exhaustive list of Trestle Events
 */
public enum TrestleEvent implements HasIRI {
    CREATED     ("created"),
    DESTROYED   ("destroyed"),
    BECOMES     ("becomes"),
    SPLITS      ("splits"),
    MERGES      ("merges");

    private final IRI relationIRI;
    private final String shortName;

    TrestleEvent(String eventString) {
        this.relationIRI = IRI.create(TRESTLE_PREFIX, eventString);
        this.shortName = eventString;
    }


    @Override
    public IRI getIRI() {
        return relationIRI;
    }

    /**
     * Get the {@link IRI} of the event type, as a string
     * @return - {@link String} of {@link IRI}
     */
    public String getIRIString() {
        return this.relationIRI.getIRIString();
    }

    /**
     * Get the short name of the event
     * @return {@link String} name
     */
    public String getShortName() {
        return this.shortName;
    }

    public static TrestleEvent getEventFromIRI(IRI eventIRI) {
        return Arrays.stream(TrestleEvent.values())
                .filter(event -> event.getIRI().equals(eventIRI))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find EventType for IRI %s", eventIRI.getIRIString())));
    }
}

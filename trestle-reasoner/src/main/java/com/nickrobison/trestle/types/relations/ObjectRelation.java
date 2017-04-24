package com.nickrobison.trestle.types.relations;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import java.util.Arrays;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Created by nrobison on 12/19/16.
 */
public enum ObjectRelation {
//    Spatial
    CONTAINS            ("contains"),
    COVERS              ("covers"),
    DISJOINT            ("disjoint"),
    EQUALS              ("equal"),
    INSIDE              ("inside"),
    MEETS               ("meets"),
    SPATIAL_OVERLAPS    ("spatial_overlaps"),
//    Temporal
    AFTER               ("after"),
    BEFORE              ("before"),
    BEGINS              ("begins"),
    DURING              ("during"),
    ENDS                ("ends"),
    TEMPORAL_OVERLAPS   ("temporal_overlaps");

    private final IRI relationIRI;
    private final String relationShortName;

    ObjectRelation(String relationString) {
        this.relationShortName = relationString;
        this.relationIRI = IRI.create(TRESTLE_PREFIX, relationString);
    }

    /**
     * Get the IRI of the relation
     * @return - IRI
     */
    public IRI getIRI() {
        return relationIRI;
    }

    /**
     * Get the IRI of the relation, as a string
     * @return - String of IRI
     */
    public String getIRIString() {
        return relationIRI.getIRIString();
    }

    /**
     * Find the ObjectRelation that matches the given IRI
     * Throws a RuntimeException if the given IRI doesn't match any known relations
     * @param relationIRI - IRI to match on
     * @return - ObjectRelation that matches the given IRI
     */
    public static ObjectRelation getRelationFromIRI(IRI relationIRI) {
        return Arrays.stream(ObjectRelation.values())
                .filter(object -> object.getIRI().equals(relationIRI))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Cannot find ObjectRelation for IRI %s", relationIRI.getIRIString())));
    }
}

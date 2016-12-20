package com.nickrobison.trestle.types.relations;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;

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

    ObjectRelation(String relationString) {
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
}

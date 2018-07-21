package com.nickrobison.trestle.types.relations;

import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;

import java.io.Serializable;
import java.util.Arrays;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;

/**
 * Created by nrobison on 12/19/16.
 */
public enum ObjectRelation implements HasIRI, Serializable {
    //    Spatial
    /**
     * Exists if Object fully encompassed within the Subject
     */
    CONTAINS("contains"),
    /**
     * Exists if Subject contains Object and the exterior ring of Subject is at any point equal to the exterior ring of the Object.
     */
    COVERS("covers"),
    /**
     * Subject and Object share no spatial relationships
     */
    DISJOINT("disjoint"),
    /**
     * Exists if the Subject and Object correspond to the same spatial area
     */
    SPATIAL_EQUALS("spatial_equals"),
    /**
     * Inverse Relationship of {@link ObjectRelation#CONTAINS}.
     * Exists if Subject is fully encompassed within the Subject
     */
    INSIDE("inside"),
    /**
     * Inverse Relationship of {@link ObjectRelation#COVERS}
     * Exists if the Subject is inside the Object and the exterior ring of Subject is at any point equal to the exterior ring of the Object.
     */
    COVERED_BY("covered_by"),
    /**
     * Exists if the exterior ring of the Subject and the Object are at any point equal
     */
    SPATIAL_MEETS("meets"),
    /**
     * Exists if the Subject and Object share only a portion of the same spatial area.
     */
    SPATIAL_OVERLAPS("spatial_overlaps"),
    //    Temporal
    /**
     * exists if the subject occurs after the object.
     */
    AFTER("after"),
    /**
     * exists if the subject occurs before the object.
     */
    BEFORE("before"),
    /**
     * exists if subject and object share the same temporal start point
     * and the subject occurs {@link ObjectRelation#DURING} the object
     */
    STARTS("starts"),
    /**
     * Inverse relationship of {@link ObjectRelation#STARTS}
     */
    STARTED_BY("started_by"),
    /**
     * exists if subject temporal period exists entirely within the period of the object.
     * Note: Normal temporal equality uses standard [) annotation; however, for the purposes of this relationship,
     * the [] annotation is used. Otherwise we can never have a {@link ObjectRelation#FINISHES} relation
     */
    DURING("during"),
    /**
     * exists if subject and object share the same temporal end point
     * and the subject occurs {@link ObjectRelation#DURING} the object
     */
    FINISHES("finishes"),
    /**
     * Inverse relationship of {@link ObjectRelation#FINISHES}
     */
    FINISHED_BY("finished_by"),
    /**
     * Exists if the subject overlaps the object at any point.
     */
    TEMPORAL_OVERLAPS("temporal_overlaps"),
    /**
     * Exists if the end temporal of the subject equals the start temporal of object
     * and the the subject does not occur {@link ObjectRelation#DURING} the object
     */
    TEMPORAL_MEETS("temporal_meets"),
    /**
     * Inverse relationship of {@link ObjectRelation#TEMPORAL_MEETS}
     */
    MET_BY("met_by"),
    //    Event
    MERGED_INTO("merged_into"),
    MERGED_FROM("merged_from"),
    SPLIT_INTO("split_into"),
    SPLIT_FROM("split_from"),
    COMPONENT_OF("component_of"),
    COMPONENT_WITH("component_with"),
    //    Fuzzy equals relation
    EQUALS("equals");


    private final IRI relationIRI;

    ObjectRelation(String relationString) {
        this.relationIRI = IRI.create(TRESTLE_PREFIX, relationString);
    }

    /**
     * Get the IRI of the relation
     *
     * @return - IRI
     */
    @Override
    public IRI getIRI() {
        return relationIRI;
    }

    /**
     * Get the {@link IRI} of the relation, as a string
     *
     * @return - {@link String} of {@link IRI}
     */
    public String getIRIString() {
        return relationIRI.getIRIString();
    }

    /**
     * Find the ObjectRelation that matches the given IRI
     * throw {@link IllegalArgumentException} if the given IRI doesn't match any known relations
     *
     * @param relationIRI - IRI to match on
     * @return - ObjectRelation that matches the given IRI
     */
    public static ObjectRelation getRelationFromIRI(IRI relationIRI) {
        return Arrays.stream(ObjectRelation.values())
                .filter(object -> object.getIRI().equals(relationIRI))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot find ObjectRelation for IRI %s", relationIRI.getIRIString())));
    }
}

package com.nickrobison.trestle.reasoner.engines.concept;

import com.nickrobison.trestle.types.relations.ConceptRelationType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nickrobison on 2/19/18.
 */
public interface ITrestleConceptEngine {
    /**
     * For a given individual, get all related concepts and the {@link IRI} of all members of those concepts,
     * that have a relation strength above the given cutoff value
     *
     * @param individual       - {@link String} ID of individual to return relations for
     * @param conceptID        - Nullable {@link String} of concept ID to filter members of
     * @param relationStrength - {@link double} Cutoff value of minimum relation strength
     * @return - {@link Optional} {@link Map} of String IRI representations of related concepts
     */
    Optional<Map<String, List<String>>> getRelatedConcepts(String individual, @Nullable String conceptID, double relationStrength);

    /**
     * Return a set of Trestle_Concepts that intersect with the given WKT
     * The temporal parameters allow for additional specificity on the spatio-temporal intersection
     *
     * @param wkt      - {@link String} of WKT to intersect with
     * @param buffer   - {@link double} buffer to draw around WKT
     * @param strength - {@link double} strength parameter to filter weak associations
     * @param validAt  - {@link Temporal} of validAt time
     * @param dbAt     - Optional {@link Temporal} of dbAt time
     * @return - {@link Optional} {@link Set} of {@link String} Concept IDs
     */
    Optional<Set<String>> STIntersectConcept(String wkt, double buffer, double strength, Temporal validAt, @Nullable Temporal dbAt);

    /**
     * Retrieve all members of a specified concept that match a given class
     * If the {@code spatialIntersection} parameter occurs outside of the exists range of the target TrestleObjects, the intersection point is adjusted, in order to return a valid object
     * If the intersection point occurs before the TrestleObject, the earliest version of that object is returned
     * If the intersection point occurs after the TrestleObject, the latest version of the object is returned
     *
     * @param <T>                  - Generic type {@link T} of returned object
     * @param clazz                - Input {@link Class} to retrieve from concept
     * @param conceptID            - {@link String} ID of concept to retrieve
     * @param strength             - {@link double} Strength parameter to filter weak associations
     * @param spatialIntersection  - Optional spatial intersection to restrict results
     * @param temporalIntersection - Optional temporal intersection to restrict results
     * @return - {@link Optional} {@link List} of Objects
     */
    <T> Optional<List<T>> getConceptMembers(Class<T> clazz, String conceptID, double strength, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection);

    /**
     * Write an object into the database, as a member of a given concept
     *
     * @param conceptIRI   - {@link String} ID of concept to add object to
     * @param inputObject  - {@link Object} to write into database
     * @param relationType - {@link ConceptRelationType}
     * @param strength     - {@link double} Strength parameter of relation
     */
    void addObjectToConcept(String conceptIRI, Object inputObject, ConceptRelationType relationType, double strength);
}

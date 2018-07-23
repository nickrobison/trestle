package com.nickrobison.trestle.reasoner.engines.collection;

import com.nickrobison.trestle.types.relations.CollectionRelationType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.IRI;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nickrobison on 2/19/18.
 */
public interface ITrestleCollectionEngine {
    /**
     * Get all Trestle_Collections currently in the repository
     *
     * @return - {@link List} of {@link String} Collection IDs
     */
    List<String> getCollections();

    /**
     * For a given individual, get all related collections and the {@link IRI} of all members of those collections,
     * that have a relation strength above the given cutoff value
     *
     * @param individual       - {@link String} ID of individual to return relations for
     * @param collectionID        - Nullable {@link String} of collection ID to filter members of
     * @param relationStrength - {@link double} Cutoff value of minimum relation strength
     * @return - {@link Optional} {@link Map} of String IRI representations of related collections
     */
    Optional<Map<String, List<String>>> getRelatedCollections(String individual, @Nullable String collectionID, double relationStrength);

    /**
     * Return a set of Trestle_Collections that intersect with the given WKT
     * The temporal parameters allow for additional specificity on the spatio-temporal intersection
     *
     * @param wkt      - {@link String} of WKT to intersect with
     * @param buffer   - {@link double} buffer to draw around WKT. 0 is no buffer (defaults to {@link javax.measure.unit.SI#METER}
     * @param strength - {@link double} strength parameter to filter weak associations
     * @param validAt  - {@link Temporal} of validAt time
     * @param dbAt     - Optional {@link Temporal} of dbAt time
     * @return - {@link Optional} {@link Set} of {@link String} Collection IDs
     */
    Optional<Set<String>> STIntersectCollection(String wkt, double buffer, double strength, Temporal validAt, @Nullable Temporal dbAt);

    /**
     * Return a set of Trestle_Collections that intersect with the given WKT
     * The temporal parameters allow for additional specificity on the spatio-temporal intersection
     *
     * @param wkt        - {@link String} of WKT to intersect with
     * @param buffer     - {@link Double} buffer to draw around WKT
     * @param bufferUnit - {@link Unit} of {@link Length} buffer units
     * @param strength   - {@link Double} strength parameter to filter weak associations
     * @param validAt    - {@link Temporal} of validAt time
     * @param dbAt       - Optional {@link Temporal} of dbAt time
     * @return - {@link Optional} {@link Set} of {@link String} Collection IDs
     */
    Optional<Set<String>> STIntersectCollection(String wkt, double buffer, Unit<Length> bufferUnit, double strength, Temporal validAt, @Nullable Temporal dbAt);

    /**
     * Retrieve all members of a specified collection that match a given class
     * If the {@code spatialIntersection} parameter occurs outside of the exists range of the target TrestleObjects, the intersection point is adjusted, in order to return a valid object
     * If the intersection point occurs before the TrestleObject, the earliest version of that object is returned
     * If the intersection point occurs after the TrestleObject, the latest version of the object is returned
     *
     * @param <T>                  - Generic type {@link T} of returned object
     * @param clazz                - Input {@link Class} to retrieve from collection
     * @param collectionID            - {@link String} ID of collection to retrieve
     * @param strength             - {@link Double} Strength parameter to filter weak associations
     * @param spatialIntersection  - Optional spatial intersection to restrict results
     * @param temporalIntersection - Optional temporal intersection to restrict results
     * @return - {@link Optional} {@link List} of Objects
     */
    <T> Optional<List<T>> getCollectionMembers(Class<T> clazz, String collectionID, double strength, @Nullable String spatialIntersection, @Nullable Temporal temporalIntersection);

    /**
     * Write an object into the database, as a member of a given collection
     *
     * @param collectionIRI   - {@link String} ID of collection to add object to
     * @param inputObject  - {@link Object} to write into database
     * @param relationType - {@link CollectionRelationType}
     * @param strength     - {@link Double} Strength parameter of relation
     */
    void addObjectToCollection(String collectionIRI, Object inputObject, CollectionRelationType relationType, double strength);

    /**
     * Remove the specified Trestle_Collection
     *
     * @param collectionIRI - {@link String} Collection ID
     */
    void removeCollection(String collectionIRI);

    /**
     * Remove a given Trestle_Object from the Trestle_Collection
     * Optionally, if removing the object causes the Collection to be empty, remove the collection.
     *  @param collectionIRI - {@link String} Collection ID
     * @param inputObject - {@link Object} Java object to add to collection
     * @param removeEmptyCollection - {@code true} Remove Collection if it's empty. {@code false} Leave empty collection
     */
    void removeObjectFromCollection(String collectionIRI, Object inputObject, boolean removeEmptyCollection);

    /**
     * Determines whether or not two Collections are spatially adjacent to each other.
     * Requires that Spatial and Temporal relationships be generated for the underlying Trestle_Objects.
     * Specifically, looks for {@link com.nickrobison.trestle.types.relations.ObjectRelation#SPATIAL_MEETS} on any of the collection members.
     * If relationships have not been generated, consider {@link ITrestleCollectionEngine#STIntersectCollection(String, double, double, Temporal, Temporal)}
     *
     * The associated strength parameter is applied symmetrically across both collections.
     *
     * @param subjectCollectionID - {@link String} ID of collection to query
     * @param objectCollectionID - {@link String} ID collection to determine intersection with
     * @param strength - {@link Double} strength parameter to filter weak associations
     * @return - {@code true} Collections are adjacent. {@code false} Collections are not adjacent.
     */
    boolean collectionsAreAdjacent(String subjectCollectionID, String objectCollectionID, double strength);
}

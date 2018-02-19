package com.nickrobison.trestle.reasoner.engines.object;

import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.temporal.Temporal;
import java.util.List;

/**
 * Created by nickrobison on 2/13/18.
 */

/**
 * Base interface for providing methods for writing an object to the underlying database
 * All methods require that the given Java classes be previously registered with the reasoner by calling {@link com.nickrobison.trestle.reasoner.TrestleReasoner#registerClass(Class)}
 * If methods are called with a non-registered class a {@link TrestleClassException} will be thrown
 */
public interface ITrestleObjectWriter {
    /**
     * Write a Java {@link Object} as a Trestle_Object
     *
     * @param inputObject - Input {@link Object} to write to the database
     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
     * @throws MissingOntologyEntity - Throws if the individual doesn't exist in the database
     */
    void writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Write a Java {@link Object} as a Trestle_Object
     * Use the provided temporals to manually set the database time
     *
     * @param inputObject   - {@link Object} to write to the database
     * @param startTemporal - Start {@link Temporal} of database time interval
     * @param endTemporal   - Nullable {@link Temporal} of ending interval time
     * @throws MissingOntologyEntity      - Throws if the individual doesn't exist in the database
     * @throws UnregisteredClassException - Throws if the object class isn't registered with the reasoner
     */
    void writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException;

    /**
     * Manually add a Fact to a TrestleObject, along with a specified validity point
     *
     * @param clazz        - Java {@link Class} to retrieve from the class registry
     * @param individual   - {@link String} ID of individual
     * @param factName     - {@link String} name of fact
     * @param value        - {@link Object} value of fact
     * @param validAt      - {@link Temporal} to denote the ValidAt time
     * @param databaseFrom - Optional {@link Temporal} to denote the DatabaseAt time
     */
    void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom);

    /**
     * Manually add a Fact to a TrestleObject, along with a specified validity interval
     *
     * @param clazz        - Java {@link Class} to retrieve from the class registry
     * @param individual   - {@link String} ID of individual
     * @param factName     - {@link String} name of Fact
     * @param value        - {@link Object} value of Fact
     * @param validFrom    - {@link Temporal} to denote validFrom time
     * @param validTo      - {@link Temporal} to denote validTo time
     * @param databaseFrom - Optional {@link Temporal} to denote databaseFrom time
     */
    void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom);

    /**
     * Add a {@link TrestleEventType} between the given <i>Subject</i> object and the collection of <i>Object</i> objects
     *
     * @param type     - {@link TrestleEventType} to add to object collection
     * @param subject  - {@link Object} of type {@link T} as event Subject
     * @param objects  - {@link List} of {@link Object} of type {@link T} as event Objects
     * @param strength - {@link double} strength of event association
     * @param <T>      - Java {@link Class} of underlying objects, registered with the reasoner
     */
    <T extends @NonNull Object> void addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength);

    /**
     * Write a relationship between two objects.
     * If one or both of those objects do not exist, create them.
     *
     * @param subject  - Java {@link Object} to write as subject of relationship
     * @param object   - Java {@link Object} to write as object of relationship
     * @param relation - {@link ObjectRelation} between the two object
     */
    void writeObjectRelationship(Object subject, Object object, ObjectRelation relation);

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject - Java {@link Object} to write as subject of relationship
     * @param object  - Java {@link Object} to write as object of relationship
     * @param wkt     - {@link String} of wkt boundary of spatial overlap
     */
    void writeSpatialOverlap(Object subject, Object object, String wkt);

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject         - Java {@link Object} to write as subject of relationship
     * @param object          - Java {@link Object} to write as object of relationship
     * @param temporalOverlap - {@link String} of temporal overlap between two objects (Not implemented yet)
     */
    //    TODO(nrobison): Correctly implement this
    void writeTemporalOverlap(Object subject, Object object, String temporalOverlap);
}

package com.nickrobison.trestle.reasoner.engines.object;

import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.events.TrestleEventType;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import io.reactivex.rxjava3.core.Completable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Set;

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
     * @return - {@link Completable} when finished
     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
     * @throws MissingOntologyEntity - Throws if the individual doesn't exist in the database
     * @throws TrestleClassException - if the class isn't registered
     * @throws MissingOntologyEntity - Should never throw this
     */
    Completable writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Write a Java {@link Object} as a Trestle_Object
     * Use the provided temporals to manually set the database time
     *
     * @param inputObject   - {@link Object} to write to the database
     * @param startTemporal - Start {@link Temporal} of database time interval
     * @param endTemporal   - Nullable {@link Temporal} of ending interval time
     * @return - {@link Completable} when finished
     * @throws UnregisteredClassException - if the class isn't registered
     * @throws MissingOntologyEntity - Should never throw this
     */
    Completable writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException;

    /**
     * Manually add a Fact to a TrestleObject, along with a specified validity point
     *
     * @param clazz        - Java {@link Class} to retrieve from the class registry
     * @param individual   - {@link String} ID of individual
     * @param factName     - {@link String} name of fact
     * @param value        - {@link Object} value of fact
     * @param validAt      - {@link Temporal} to denote the ValidAt time
     * @param databaseFrom - Optional {@link Temporal} to denote the DatabaseAt time
     * @return - {@link Completable} when finished
     */
    Completable addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom);

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
     * @return - {@link Completable} when finished
     */
    Completable addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom);

    /**
     * Add a SPLIT or MERGE {@link TrestleEventType} to a given {@link OWLNamedIndividual}
     * Events are oriented subject to object, so A splits_into [B,C,D] and H merged_from [E,F,G]
     * Individuals are not created if they don't already exist
     * throws {@link IllegalArgumentException} if something other than {@link TrestleEventType#MERGED} or {@link TrestleEventType#SPLIT} is passed
     *
     * @param <T>      - Generic type parameter of Trestle Object
     * @param type     {@link TrestleEventType} to add
     * @param subject  - {@link OWLNamedIndividual} subject of Event
     * @param objects  - {@link Set} of {@link OWLNamedIndividual} that are the objects of the event
     * @param strength - {@link Double} Strength of union association
     * @return - {@link Completable} when finished
     */
    <T extends @NonNull Object> Completable addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength);

    /**
     * Write a relationship between two objects.
     * If one or both of those objects do not exist, create them.
     *
     * @param subject     - Java {@link Object} to write as subject of relationship
     * @param object      - Java {@link Object} to write as object of relationship
     * @param relation    - {@link ObjectRelation} between the two object
     * @param transaction - {@link TrestleTransaction} to continue with
     * @return - {@link Completable} when finished
     */
    Completable writeObjectRelationship(Object subject, Object object, ObjectRelation relation, @Nullable TrestleTransaction transaction);

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject - Java {@link Object} to write as subject of relationship
     * @param object  - Java {@link Object} to write as object of relationship
     * @param wkt     - {@link String} of wkt boundary of spatial overlap
     * @return - {@link Completable} when finished
     */
    Completable writeSpatialOverlap(Object subject, Object object, String wkt);

    /**
     * Create a spatial overlap association between two objects.
     * If one or both of the object do not exist, create them.
     *
     * @param subject         - Java {@link Object} to write as subject of relationship
     * @param object          - Java {@link Object} to write as object of relationship
     * @param temporalOverlap - {@link String} of temporal overlap between two objects (Not implemented yet)
     * @return - {@link Completable} when finished
     */
    //    TODO(nrobison): Correctly implement this
    Completable writeTemporalOverlap(Object subject, Object object, String temporalOverlap);
}

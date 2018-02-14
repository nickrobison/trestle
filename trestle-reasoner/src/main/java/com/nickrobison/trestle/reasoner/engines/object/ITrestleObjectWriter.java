package com.nickrobison.trestle.reasoner.engines.object;

import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.types.events.TrestleEventType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.temporal.Temporal;
import java.util.List;

/**
 * Created by nickrobison on 2/13/18.
 */
public interface ITrestleObjectWriter {
    /**
     * Write a Java object as a Trestle_Object
     *
     * @param inputObject - Input object to write as fact
     * @throws TrestleClassException - Throws an exception if the class doesn't exist or is invalid
     * @throws MissingOntologyEntity - Throws if the individual doesn't exist in the ontology
     */
    void writeTrestleObject(Object inputObject) throws TrestleClassException, MissingOntologyEntity;

    /**
     * Write a Java object as a Trestle_Object
     * Use the provided temporals to setup the database time
     *
     * @param inputObject   - Object to write into the ontology
     * @param startTemporal - Start {@link Temporal} of database time interval
     * @param endTemporal   - Nullable {@link Temporal} of ending interval time
     * @throws MissingOntologyEntity      - Throws if the individual doesn't exist in the ontology
     * @throws UnregisteredClassException - Throws if the object class isn't registered with the reasoner
     */
    void writeTrestleObject(Object inputObject, Temporal startTemporal, @Nullable Temporal endTemporal) throws MissingOntologyEntity, UnregisteredClassException;

    /**
     * Manually add a Fact to a TrestleObject, along with a specified validity point
     *
     * @param clazz        - Java class to parse
     * @param individual   - Individual ID
     * @param factName     - Fact name
     * @param value        - Fact value
     * @param validAt      - validAt Temporal
     * @param databaseFrom - Optional databaseFrom Temporal
     */
    void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validAt, @Nullable Temporal databaseFrom);

    /**
     * Manually add a Fact to a TrestleObject, along with a specified validity interval
     *
     * @param clazz        - Java class to parse
     * @param individual   - Individual ID
     * @param factName     - Fact name
     * @param value        - Fact value
     * @param validFrom    - validFrom Temporal
     * @param validTo      - validTo Temporal
     * @param databaseFrom - Optional databaseFrom Temporal
     */
    void addFactToTrestleObject(Class<?> clazz, String individual, String factName, Object value, Temporal validFrom, @Nullable Temporal validTo, @Nullable Temporal databaseFrom);

    <T extends @NonNull Object> void addTrestleObjectSplitMerge(TrestleEventType type, T subject, List<T> objects, double strength);
}

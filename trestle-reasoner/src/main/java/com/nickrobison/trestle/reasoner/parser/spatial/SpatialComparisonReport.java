package com.nickrobison.trestle.reasoner.parser.spatial;

import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.vividsolutions.jts.geom.Geometry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public class SpatialComparisonReport<A, B> implements Serializable {
    public static final long serialVersionUID = 42L;

    private final A objectA;
    private final B objectB;
    private final Set<ObjectRelation> relations;
    private double equality;
    private @MonotonicNonNull Geometry overlap;

    public SpatialComparisonReport(A objectA, B objectB) {
        this.objectA = objectA;
        this.objectB = objectB;
        this.relations = new HashSet<>();
    }

    public A getObjectA() {
        return objectA;
    }

    public B getObjectB() {
        return objectB;
    }

    /**
     * Add {@link ObjectRelation#EQUALS} relationship between the two objects
     *
     * @param equalityPercentage - {@link Double} percent equality between the objects
     */
    public void addApproximateEquality(double equalityPercentage) {
        this.relations.add(ObjectRelation.EQUALS);
        this.equality = equalityPercentage;
    }

    /**
     * Returns {@link ObjectRelation#EQUALS} strength, if the relation exists
     *
     * @return - {@link Optional} of {@link Double} if the relation exists. {@link Optional#empty()} if not
     */
    public Optional<Double> getEquality() {
        if (this.relations.contains(ObjectRelation.EQUALS)) {
            return Optional.of(equality);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Add {@link ObjectRelation#SPATIAL_OVERLAPS} relation along with {@link Geometry} representing overlapping area
     *
     * @param overlap - {@link Geometry} overlapping geometry
     */
    public void addSpatialOverlap(Geometry overlap) {
        this.addRelation(ObjectRelation.SPATIAL_OVERLAPS);
        this.overlap = overlap;
    }

    /**
     * Get the {@link Geometry} of overlapping area, if it exists
     *
     * @return - {@link Optional} of {@link Geometry}, {@link OptionalLong#empty()} if it doesn't exist
     */
    public Optional<Geometry> getSpatialOverlap() {
        return Optional.ofNullable(this.overlap);
    }

    /**
     * Add the specified {@link ObjectRelation} to the relation set
     *
     * @param relation - {@link ObjectRelation} to add
     */
    public void addRelation(ObjectRelation relation) {
        this.relations.add(relation);
    }
}

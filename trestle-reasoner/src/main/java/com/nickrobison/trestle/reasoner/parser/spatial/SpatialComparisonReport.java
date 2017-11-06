package com.nickrobison.trestle.reasoner.parser.spatial;

import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.vividsolutions.jts.geom.Geometry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public class SpatialComparisonReport implements Serializable {
    public static final long serialVersionUID = 42L;

    private final String objectAID;
    private final String objectBID;
    private final Set<ObjectRelation> relations;
    private @MonotonicNonNull Double equality;
    private @MonotonicNonNull String overlap;
    private @MonotonicNonNull Double overlapPercentage;

    public SpatialComparisonReport(OWLNamedIndividual objectAID, OWLNamedIndividual objectBID) {
        this.objectAID = objectAID.toStringID();
        this.objectBID = objectBID.toStringID();
        this.relations = new HashSet<>();
    }

    public String getObjectAID() {
        return objectAID;
    }

    public String getObjectBID() {
        return objectBID;
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
     * Add {@link ObjectRelation#SPATIAL_OVERLAPS} relation along with {@link String} WKT representation overlapping area
     *
     * @param overlap           - {@link String} overlapping geometry
     * @param overlapPercentage - {@link Double} percentage overlap of the two objects
     */
    public void addSpatialOverlap(String overlap, Double overlapPercentage) {
        this.addRelation(ObjectRelation.SPATIAL_OVERLAPS);
        this.overlap = overlap;
        this.overlapPercentage = overlapPercentage;
    }

    /**
     * Get the {@link Geometry} of overlapping area, if it exists
     *
     * @return - {@link Optional} of {@link String}, {@link OptionalLong#empty()} if it doesn't exist
     */
    public Optional<String> getSpatialOverlap() {
        return Optional.ofNullable(this.overlap);
    }


    /**
     * Get the {@link Double} percentage of the overlapping area, if it exists
     *
     * @return - {@link Optional} of {@link Double}, {@link Optional#empty()} if it doesn't exist
     */
    public Optional<Double> getSpatialOverlapPercentage() {
        return Optional.ofNullable(this.overlapPercentage);
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

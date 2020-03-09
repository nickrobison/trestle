package com.nickrobison.trestle.reasoner.engines.spatial;

import com.nickrobison.trestle.reasoner.engines.AbstractComparisonReport;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import org.locationtech.jts.geom.Geometry;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Optional;
import java.util.OptionalLong;

public class SpatialComparisonReport extends AbstractComparisonReport {
    public static final long serialVersionUID = 42L;

    private @MonotonicNonNull Double equality;
    private @MonotonicNonNull String overlap;
    private @MonotonicNonNull Double overlapPercentage;

    public SpatialComparisonReport(OWLNamedIndividual objectA, OWLNamedIndividual objectB) {
        super(objectA, objectB);
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
        return Optional.ofNullable(this.equality);
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
}

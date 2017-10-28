package com.nickrobison.trestle.reasoner.spatial;

import com.nickrobison.trestle.types.TrestleIndividual;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLClass;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

public interface SpatialEngine {


    /**
     * Performs a spatial intersection on a given dataset with a specified spatio-temporal restriction
     * Returns an optional list of {@link TrestleIndividual}s
     * If no valid temporal is specified, performs a spatial intersection with no temporal constraints
     *
     * @param clazz - {@link Class} of dataset {@link OWLClass}
     * @param wkt - {@link String} WKT boundary
     * @param buffer - {@link Double} buffer to extend around buffer. 0 is no buffer
     * @param validAt - {@link Temporal} valid at restriction
     * @param dbAt - {@link Temporal} database at restriction
     * @return - {@link Optional} {@link List} of {@link TrestleIndividual}
     */
    Optional<List<TrestleIndividual>> spatialIntersectIndividuals(Class<@NonNull ?> clazz, String wkt, double buffer, @Nullable Temporal validAt, @Nullable Temporal dbAt);
}

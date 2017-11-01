package com.nickrobison.trestle.reasoner.engines.spatial.equality;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.SpatialUnionBuilder;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.SpatialUnionTraverser;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionEqualityResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

public class EqualityEngineImpl implements EqualityEngine {
    private static final Logger logger = LoggerFactory.getLogger(EqualityEngineImpl.class);

    private final SpatialUnionBuilder unionBuilder;
    private final SpatialUnionTraverser unionWalker;

    @Inject
    public EqualityEngineImpl(SpatialUnionBuilder builder, SpatialUnionTraverser walker) {
        logger.info("Instantiating Equality Engine");
        this.unionBuilder = builder;
        this.unionWalker = walker;
    }

    @Override
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
        return this.unionBuilder.getApproximateEqualUnion(inputObjects, inputSR, matchThreshold);
    }

    @Override
    public <T> UnionContributionResult<T> calculateUnionContribution(UnionEqualityResult<T> result, SpatialReference inputSR) {
        return this.unionBuilder.calculateContribution(result, inputSR);
    }

    @Override
    public <T extends @NonNull Object> boolean isApproximatelyEqual(T inputObject, T matchObject, SpatialReference inputSR, double threshold) {
        final double percentEquals = this.unionBuilder.calculateSpatialEquals(inputObject, matchObject, inputSR);
        logger.debug("{} and {} have equality of {}", inputObject, matchObject, percentEquals);
        return percentEquals >= threshold;
    }

    @Override
    public <T> double calculateSpatialEquals(T inputObject, T matchObject, SpatialReference inputSR) {
        return this.unionBuilder.calculateSpatialEquals(inputObject, matchObject, inputSR);
    }

    @Override
    public <T extends @NonNull Object> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, OWLNamedIndividual individual, Temporal queryTemporal) {
        return this.unionWalker.traverseUnion(clazz, individual, queryTemporal);
    }

    @Override
    public <T extends @NonNull Object> List<OWLNamedIndividual> getEquivalentIndividuals(Class<T> clazz, List<OWLNamedIndividual> individual, Temporal queryTemporal) {
        return this.unionWalker.traverseUnion(clazz, individual, queryTemporal);
    }
}

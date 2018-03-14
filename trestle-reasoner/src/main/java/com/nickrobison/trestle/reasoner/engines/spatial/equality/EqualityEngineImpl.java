package com.nickrobison.trestle.reasoner.engines.spatial.equality;

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
    public <T extends @NonNull Object> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, int inputSRID, double matchThreshold) {
        return this.unionBuilder.getApproximateEqualUnion(inputObjects, inputSRID, matchThreshold);
    }

    @Override
    public <T extends @NonNull Object> UnionContributionResult calculateUnionContribution(UnionEqualityResult<T> result, int inputSRID) {
        return this.unionBuilder.calculateContribution(result, inputSRID);
    }

    @Override
    public <A extends @NonNull Object, B extends @NonNull Object> boolean isApproximatelyEqual(A inputObject, B matchObject, double threshold) {
        final double percentEquals = this.unionBuilder.calculateSpatialEquals(inputObject, matchObject);
        logger.debug("{} and {} have equality of {}", inputObject, matchObject, percentEquals);
        return percentEquals >= threshold;
    }

    @Override
    public <A extends @NonNull Object, B extends @NonNull Object> double calculateSpatialEquals(A inputObject, B matchObject) {
        return this.unionBuilder.calculateSpatialEquals(inputObject, matchObject);
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

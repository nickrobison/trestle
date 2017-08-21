package com.nickrobison.trestle.reasoner.equality;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.reasoner.equality.union.SpatialUnionBuilder;
import com.nickrobison.trestle.reasoner.equality.union.SpatialUnionTraverser;
import com.nickrobison.trestle.reasoner.equality.union.UnionEqualityResult;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class EqualityEngineImpl implements EqualityEngine {
    private static final Logger logger = LoggerFactory.getLogger(EqualityEngineImpl.class);

    private final SpatialUnionBuilder unionBuilder;
    private final SpatialUnionTraverser unionWalker;

    @Inject
    public EqualityEngineImpl(TrestleParser tp, ITrestleOntology ontology) {
        logger.info("Instantiating Equality Engine");
        this.unionBuilder = new SpatialUnionBuilder(tp);
        this.unionWalker = new SpatialUnionTraverser(ontology);
    }

    @Override
    public <T> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
        return this.unionBuilder.getApproximateEqualUnion(inputObjects, inputSR, matchThreshold);
    }

    @Override
    public <T> boolean isApproximatelyEqual(T inputObject, T matchObject, SpatialReference inputSR, double threshold) {
        final double percentEquals = this.unionBuilder.calculateSpatialEquals(inputObject, matchObject, inputSR);
        return percentEquals >= threshold;
    }
}

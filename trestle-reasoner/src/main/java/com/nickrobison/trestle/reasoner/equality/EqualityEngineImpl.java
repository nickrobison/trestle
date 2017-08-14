package com.nickrobison.trestle.reasoner.equality;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.equality.union.SpatialUnionEngine;
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

    private final SpatialUnionEngine unionEngine;

    @Inject
    public EqualityEngineImpl(TrestleParser tp) {
        logger.info("Instantiating Equality Engine");
        this.unionEngine = new SpatialUnionEngine(tp);
    }

    @Override
    public <T> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
        return this.unionEngine.getApproximateEqualUnion(inputObjects, inputSR, matchThreshold);
    }
}

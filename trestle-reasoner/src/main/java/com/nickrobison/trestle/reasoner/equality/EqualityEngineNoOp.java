package com.nickrobison.trestle.reasoner.equality;

import com.esri.core.geometry.SpatialReference;
import com.nickrobison.trestle.reasoner.equality.union.UnionEqualityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class EqualityEngineNoOp implements EqualityEngine {
    private static final Logger logger = LoggerFactory.getLogger(EqualityEngineNoOp.class);

    public EqualityEngineNoOp() {
        logger.info("Equality disabled, instantiating No-Op");
    }

    @Override
    public <T> Optional<UnionEqualityResult<T>> calculateSpatialUnion(List<T> inputObjects, SpatialReference inputSR, double matchThreshold) {
        return Optional.empty();
    }
}

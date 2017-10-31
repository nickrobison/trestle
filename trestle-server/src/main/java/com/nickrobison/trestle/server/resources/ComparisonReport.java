package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;

import java.io.Serializable;

public class ComparisonReport implements Serializable {
    public static final long serialVersionUID = 42L;

    private UnionContributionResult<Object> union;

    public ComparisonReport() {
//        Not needed
    }

    public UnionContributionResult<Object> getUnion() {
        return union;
    }

    public void setUnion(UnionContributionResult<Object> union) {
        this.union = union;
    }
}

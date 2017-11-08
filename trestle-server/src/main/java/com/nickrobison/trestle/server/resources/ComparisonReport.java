package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.parser.spatial.SpatialComparisonReport;

import java.io.Serializable;
import java.util.*;

public class ComparisonReport implements Serializable {
    public static final long serialVersionUID = 42L;

    private UnionContributionResult union;
    private final Set<SpatialComparisonReport> reports;

    public ComparisonReport() {
        this.reports = new HashSet<>();
    }

    public UnionContributionResult getUnion() {
        return union;
    }

    public void setUnion(UnionContributionResult union) {
        this.union = union;
    }

    public Set<SpatialComparisonReport> getReports() {
        return reports;
    }

    public void addReport(SpatialComparisonReport report) {
        this.reports.add(report);
    }

    public void addAllReports(Collection<SpatialComparisonReport> reports) {
        this.reports.addAll(reports);
    }
}

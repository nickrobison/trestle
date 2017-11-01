package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.reasoner.engines.spatial.equality.union.UnionContributionResult;
import com.nickrobison.trestle.reasoner.parser.spatial.SpatialComparisonReport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComparisonReport implements Serializable {
    public static final long serialVersionUID = 42L;

    private UnionContributionResult<Object> union;
    private final List<SpatialComparisonReport> reports;

    public ComparisonReport() {
        this.reports = new ArrayList<>();
    }

    public UnionContributionResult<Object> getUnion() {
        return union;
    }

    public void setUnion(UnionContributionResult<Object> union) {
        this.union = union;
    }

    public List<SpatialComparisonReport> getReports() {
        return reports;
    }

    public void addReport(SpatialComparisonReport report) {
        this.reports.add(report);
    }

    public void addAllReports(Collection<SpatialComparisonReport> reports) {
        this.reports.addAll(reports);
    }
}

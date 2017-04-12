package com.nickrobison.trestle.server.resources;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by nrobison on 4/10/17.
 */
public class MetricsQueryRequest {

    private List<String> metrics;
    @NotNull
    private Long start;
    private Long end;

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    MetricsQueryRequest() {}
}

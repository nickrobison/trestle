package com.nickrobison.trestle.server.resources.requests;

import java.util.List;

public class ComparisonRequest {

    private String compare;
    private List<String> compareAgainst;

    public ComparisonRequest() {
//        Not needed
    }


    public String getCompare() {
        return compare;
    }

    public void setCompare(String compare) {
        this.compare = compare;
    }

    public List<String> getCompareAgainst() {
        return compareAgainst;
    }

    public void setCompareAgainst(List<String> compareAgainst) {
        this.compareAgainst = compareAgainst;
    }
}

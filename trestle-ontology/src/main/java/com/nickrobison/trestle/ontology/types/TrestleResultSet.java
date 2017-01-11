package com.nickrobison.trestle.ontology.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nrobison on 1/10/17.
 */
public class TrestleResultSet {

    private final int rows;
    private final List<TrestleResult> results;

    public TrestleResultSet(int rows) {
        this.rows = rows;
        this.results = new ArrayList<>();
    }

    /**
     * Add TrestleResult to TrestleResultSet
     * @param result - TrestleResult to add
     */
    public void addResult(TrestleResult result) {
        this.results.add(result);
    }

    public List<TrestleResult> getResults() {
        return this.results;
    }
}

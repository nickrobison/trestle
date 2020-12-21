package com.nickrobison.trestle.ontology.types;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nrobison on 1/10/17.
 * ResultSet returned from {@link com.nickrobison.trestle.ontology.ITrestleOntology}
 * Thread-safe and fully expanded, it allows us to
 */
public class TrestleResultSet {

    private int rows;
    private final List<String> bindingNames;
    private final List<TrestleResult> results;

    public TrestleResultSet(int rows, List<String> bindingNames) {
        this.rows = rows;
        this.results = new ArrayList<>();
        this.bindingNames = bindingNames;
    }

    public TrestleResultSet(int rows, List<String> bindingNames, List<TrestleResult> results) {
        this.rows = rows;
        this.results = results;
        this.bindingNames = bindingNames;
    }

    /**
     * Get a {@link List} of result bindings
     * @return - {@link List} of String result bindings
     */
    public List<String> getBindingNames() {
        return this.bindingNames;
    }

    /**
     * Add {@link TrestleResult} to {@link TrestleResultSet}
     * @param result - {@link TrestleResult} to add
     */
    public void addResult(TrestleResult result) {
        this.results.add(result);
    }

    public List<TrestleResult> getResults() {
        return this.results;
    }

    public int getRows() {
        return this.rows;
    }

    public void updateRowCount() {
        this.rows = this.results.size();
    }
}

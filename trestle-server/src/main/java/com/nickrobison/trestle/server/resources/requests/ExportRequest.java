package com.nickrobison.trestle.server.resources.requests;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

public class ExportRequest {

    @NotEmpty
    private String dataset;
    @NotNull
    private List<String> individuals;
    @NotEmpty
    private String type;
    public ExportRequest() {

    }

    public ExportRequest(String dataset, List<String> individuals, String type) {
        this.dataset = dataset;
        this.individuals = individuals;
        this.type = type;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public List<String> getIndividuals() {
        return individuals;
    }

    public void setIndividuals(List<String> individuals) {
        this.individuals = individuals;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

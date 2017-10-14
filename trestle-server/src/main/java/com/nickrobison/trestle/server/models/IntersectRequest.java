package com.nickrobison.trestle.server.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.geojson.GeoJsonObject;
import org.hibernate.validator.constraints.NotEmpty;

import java.time.OffsetDateTime;

/**
 * Created by nrobison on 6/30/17.
 */
public class IntersectRequest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @NotEmpty
    private String dataset;
    @NotEmpty
    private OffsetDateTime validAt;
    @NotEmpty
    private OffsetDateTime databaseAt;
    private GeoJsonObject bbox;

    public IntersectRequest() {}

    public IntersectRequest(String dataset, String validAt, String databaseAt, GeoJsonObject bbox) {
        this.dataset = dataset;
        this.validAt = OffsetDateTime.parse(validAt);
        this.databaseAt = OffsetDateTime.parse(databaseAt);
        this.bbox = bbox;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public OffsetDateTime getValidAt() {
        return validAt;
    }

    public void setValidAt(String validAt) {
        this.validAt = OffsetDateTime.parse(validAt);
    }

    public OffsetDateTime getDatabaseAt() {
        return databaseAt;
    }

    public void setDatabaseAt(String databaseAt) {
        this.databaseAt = OffsetDateTime.parse(databaseAt);
    }

    public GeoJsonObject getBbox() {
        return bbox;
    }

    public void setBbox(GeoJsonObject bbox) {
        this.bbox = bbox;
//        this.bbox = mapper.convertValue(bbox, GeoJsonObject.class);
    }
}

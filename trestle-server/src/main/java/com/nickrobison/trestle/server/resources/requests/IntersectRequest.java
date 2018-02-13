package com.nickrobison.trestle.server.resources.requests;

import org.geojson.GeoJsonObject;
import org.hibernate.validator.constraints.NotEmpty;

import java.time.OffsetDateTime;

/**
 * Created by nrobison on 6/30/17.
 */
public class IntersectRequest {
    @NotEmpty
    private String dataset;
    private OffsetDateTime validAt;
    @NotEmpty
    private OffsetDateTime databaseAt;
    private GeoJsonObject geojson;
    private Double buffer;

    public IntersectRequest() {
    }

    public IntersectRequest(String dataset, String validAt, String databaseAt, GeoJsonObject geojson, Double buffer) {
        this.dataset = dataset;
        this.validAt = OffsetDateTime.parse(validAt);
        this.databaseAt = OffsetDateTime.parse(databaseAt);
        this.geojson = geojson;
        this.buffer = buffer;
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

    public GeoJsonObject getGeojson() {
        return geojson;
    }

    public void setGeojson(GeoJsonObject geojson) {
        this.geojson = geojson;
    }

    public Double getBuffer() {
        return buffer;
    }

    public void setBuffer(Double buffer) {
        this.buffer = buffer;
    }
}

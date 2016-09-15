package com.nickrobison.trestle.exporter;

import com.vividsolutions.jts.geom.Geometry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by nrobison on 9/15/16.
 */
public class ShapefileSchema {

    private final String geomName;
    private final Class<? extends Geometry> geomType;
    private final Map<String, Class<?>> schema = new LinkedHashMap<>();

    public ShapefileSchema(String geomName, Class<? extends Geometry> geomType) {
        this.geomName = geomName;
        this.geomType = geomType;
    }

    public void addProperty(String name, Class<?> type) {
        schema.put(name, type);
    }

    public Map<String, Class<?>> getSchema() {
        return this.schema;
    }

    public String getGeomName() {
        return this.geomName;
    }

    public Class<? extends Geometry> getGeomType() {
        return this.geomType;
    }
}

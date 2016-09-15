package com.nickrobison.trestle.exporter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by nrobison on 9/14/16.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TSIndividual {

    private final String geom;
    private final Map<String, String> properties = new LinkedHashMap<>();
    private Optional<ShapefileSchema> schema = Optional.empty();

    public TSIndividual(String geom){
        this.geom = geom;
    }

    public TSIndividual(String geom, ShapefileSchema schema) {
        this.geom = geom;
        this.schema = Optional.of(schema);
        schema.getSchema().keySet().forEach(key -> properties.put(key, ""));
    }

    public void addProperty(String property, String value) {
        this.properties.put(property, value);
    }

    /**
     * Returns the properties for the individual
     * If a schema is present it returns a sorted map
     * @return
     */
    //    TODO(nrobison): Validate types
    public Map<String, String> getProperties() {
        return this.properties;
    }

    public String getGeom() {
        return this.geom;
    }

    public String getGeomName() {
        if (this.schema.isPresent()) {
            return this.schema.get().getGeomName();
        }
        return "geom";
    }
}

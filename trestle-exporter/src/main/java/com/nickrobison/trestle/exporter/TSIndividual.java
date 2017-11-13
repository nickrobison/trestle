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
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private Optional<ShapefileSchema> schema = Optional.empty();

    public TSIndividual(String geom) {
        this.geom = geom;
    }

    public TSIndividual(String geom, ShapefileSchema schema) {
        this.geom = geom;
        this.schema = Optional.of(schema);
    }

    public void addProperty(String property, Object value) {
        this.properties.put(property, value);
    }

    public void addAllProperties(Map<String, Object> properties) {
        this.properties.putAll(properties);
    }

    /**
     * Returns the properties for the individual
     * If a schema is present it returns a sorted map
     * Since DBFs don't support primitives, we need to manually box them.
     *
     * @return - Map of property names and values
     */
    @SuppressWarnings({"argument.type.incompatible"})
    public Map<String, Object> getProperties() {
        if (this.schema.isPresent()) {
            Map<String, Object> sortedProperties = new LinkedHashMap<>();
            schema.get().getSchema().entrySet().forEach(entry -> sortedProperties.put(entry.getKey(), entry.getValue().cast(this.properties.get(entry.getKey()))));
            return sortedProperties;
        }
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

package com.nickrobison.trestle.exporter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 9/14/16.
 */
public class TSIndividual {

    private final String wkt = "";
    private final Map<String, Class<?>> properties = new HashMap<>();

    public TSIndividual(){}

    public void addProperty(String property, Class<?> type) {
        properties.put(property, type);
    }

//    TODO(nrobison): Validate types
    public Map<String, Class<?>> getProperties() {
        return this.properties;
    }

    public String getWkt() {
        return this.wkt;
    }
}

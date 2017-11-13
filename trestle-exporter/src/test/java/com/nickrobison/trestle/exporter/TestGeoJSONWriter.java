package com.nickrobison.trestle.exporter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestGeoJSONWriter {

    @Test
    public void testGeoExport() throws IOException {
//        Try to create an individual
        Map<String, Object> properties = new HashMap<>();
        properties.put("test1", "test1-val");
        properties.put("test2", 2);
        properties.put("test3", LocalDateTime.of(LocalDate.of(1989, 3, 26), LocalTime.NOON));
        final TSIndividual tsIndividual = new TSIndividual("POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))");
        tsIndividual.addAllProperties(properties);

        new GeoJsonExporter().writePropertiesToByteBuffer(Collections.singletonList(tsIndividual), "test-out.json");
    }
}

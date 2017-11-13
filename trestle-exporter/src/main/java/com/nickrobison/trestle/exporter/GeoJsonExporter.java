package com.nickrobison.trestle.exporter;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class GeoJsonExporter implements ITrestleExporter {

    private final String prefix;
    private final ObjectMapper mapper;


    public GeoJsonExporter() {
        this.prefix = "Trestle";
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true);
    }


    @Override
    public DataType exporterType() {
        return DataType.GEOJSON;
    }

    @Override
    public File writePropertiesToByteBuffer(List<TSIndividual> individuals, @Nullable String fileName) throws IOException {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final GeoJsonWriter geoWriter = new GeoJsonWriter();

//        Create the collection node
        final ObjectNode collectionNode = this.mapper.createObjectNode();
        final ArrayNode featuresNode = this.mapper.createArrayNode();
        collectionNode.put("type", "FeatureCollection");

        final String exportName;
        if (fileName != null) {
            exportName = String.format("%s_%s", this.prefix, fileName);
        } else {
            exportName = String.format("%s_Export_%s", this.prefix, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        for (final TSIndividual individual : individuals) {
            final ObjectNode featureNode = this.mapper.createObjectNode();
            featureNode.put("type", "Feature");
            try {
                final Geometry geometry = wktReader.read(individual.getGeom());
                final String coordinateString = geoWriter.write(geometry);
                final JsonNode coordianteNode = mapper.readTree(coordinateString);
                featureNode.set("geometry", coordianteNode);
                //                    Add all the properties
                final ObjectNode propertiesNode = this.mapper.createObjectNode();
                for (final Map.Entry<String, Object> entry : individual.getProperties().entrySet()) {
                    final ObjectWriter objectWriter = this.mapper.writerFor(entry.getValue().getClass());
                    final String propertyValue = objectWriter.writeValueAsString(entry.getValue());
                    propertiesNode.set(entry.getKey(), this.mapper.readTree(propertyValue));
                }
//                        Add the properties
                featureNode.set("properties", propertiesNode);
                featuresNode.add(featureNode);
            } catch (ParseException | IOException e) {
                throw new TrestleInvalidDataException("Cannot read wkt", individual.getGeom());
            }
        }

//        Add them and return the value
        collectionNode.set("features", featuresNode);

        final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

        final File file = new File(exportName);
        writer.writeValue(file, collectionNode);
        return file;
    }
}

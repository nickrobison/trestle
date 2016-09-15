package com.nickrobison.trestle.exporter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.ParseException;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by nrobison on 9/14/16.
 */
public class ShapefileExporter<T extends Geometry> implements ITrestleExporter {

    private static final Logger logger = LoggerFactory.getLogger(ShapefileExporter.class);
    private final SimpleFeatureBuilder simpleFeatureBuilder;
    private final GeometryFactory geometryFactory;
    private final DefaultFeatureCollection featureCollection;
    private final WKTReader2 wktReader;
    private final Class<T> type;

    private ShapefileExporter(Builder builder) {
        this.type = builder.type;
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(builder.typeName);
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);

//        Add the geometry type first
        typeBuilder.add(builder.typeName, builder.type);

//        Now the rest of the properties
        BiConsumer<String, Class<?>> addToBuilder = (key, value) -> builder.addProperty(key, value);
        builder.properties.forEach(addToBuilder);

        final SimpleFeatureType simpleFeatureType = typeBuilder.buildFeatureType();
        simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureType);
        featureCollection = new DefaultFeatureCollection();
        geometryFactory = JTSFactoryFinder.getGeometryFactory();
        wktReader = new WKTReader2();
    }

    @Override
    public DataType exporterType() {
        return DataType.SHAPEFILE;
    }

    @Override
    public ByteBuffer writePropertiesToByteBuffer(List<TSIndividual> individuals) {
        individuals.forEach(individual -> {

//            Build the geometry
            try {
                final T geometry = type.cast(wktReader.read(individual.getWkt()));
                simpleFeatureBuilder.add(geometry);
            } catch (ParseException e) {
                logger.error("Cannot parse wkt {}", e);
            }

//            Now the properties
            individual.getProperties().entrySet().forEach(entry -> {
                simpleFeatureBuilder.add(entry.getKey());
            });

            final SimpleFeature simpleFeature = simpleFeatureBuilder.buildFeature(null);
            featureCollection.add(simpleFeature);
        });

//        Now, write it out
        return null;
    }

    public static class Builder<T extends Geometry> {

        private final String typeName;
        private final Class<T> type;
        private final Map<String, Class<?>> properties = new HashMap<>();

        public Builder(String typeName, Class<T> type) {
            this.typeName = typeName;
            this.type = type;
        }

        public Builder addProperty(String key, Class<?> value) {
            properties.put(key, value);
        }

        public ShapefileExporter build() {
            return new ShapefileExporter(this);
        }
    }

}

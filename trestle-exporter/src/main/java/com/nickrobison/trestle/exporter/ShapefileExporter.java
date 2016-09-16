package com.nickrobison.trestle.exporter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileException;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
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

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private final SimpleFeatureType simpleFeatureType;

    private ShapefileExporter(Builder builder) {
        this.type = builder.type;
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(builder.typeName);
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);

//        Add the geometry type first
        typeBuilder.add(builder.typeName, builder.type);

//        Now the rest of the properties
        builder.schema.getSchema().forEach(typeBuilder::add);

        simpleFeatureType = typeBuilder.buildFeatureType();
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
    public File writePropertiesToByteBuffer(List<TSIndividual> individuals) throws IOException {
        individuals.forEach(individual -> {

//            Build the geometry
            try {
                final T geometry = type.cast(wktReader.read(individual.getGeom()));
                simpleFeatureBuilder.add(geometry);
            } catch (ParseException e) {
                logger.error("Cannot parse wkt {}", e);
            }

//            Now the properties
            individual.getProperties().entrySet().forEach(entry -> simpleFeatureBuilder.add(entry.getValue()));

            final SimpleFeature simpleFeature = simpleFeatureBuilder.buildFeature(null);
            featureCollection.add(simpleFeature);
        });

//        Now, write it out
        final File shpFile = new File("./target/test.shp");
        final File dbf = new File("./target/test.dbf");

        shpFile.createNewFile();
        dbf.createNewFile();
        final ShapefileDataStoreFactory shapefileDataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        try {
            params.put("url", shpFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        params.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore dataStore = null;
        try {
            dataStore = (ShapefileDataStore) shapefileDataStoreFactory.createDataStore(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dataStore != null) {
            dataStore.createSchema(simpleFeatureType);
//            Write it out
            Transaction transaction = new DefaultTransaction("create");
            final String typeName = dataStore.getTypeNames()[0];
            final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(featureCollection);
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }

            }


        }

//        Now, zip it
        final File zipFile = new File("./target/shp.zip");
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            final ZipOutputStream zos = new ZipOutputStream(fos);
            addToZipArchive(zos, "./target/test.shp", "./target/test.dbf", "./target/test.fix", "./target/test.prj", "./target/test.shx");
            zos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return zipFile;
    }

    private static void addToZipArchive(ZipOutputStream zos, String... fileName) {
        Arrays.stream(fileName).forEach(fn -> {
            File file = new File(fn);
            final FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(file);
                final ZipEntry zipEntry = new ZipEntry(fn);
                try {
                    zos.putNextEntry(zipEntry);
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fileInputStream.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }
                    zos.closeEntry();
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    public static class Builder<T extends Geometry> {

        private final String typeName;
        private final Class<T> type;
        private final ShapefileSchema schema;
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public Builder(String typeName, Class<T> type, ShapefileSchema schema) {
            this.typeName = typeName;
            this.type = type;
            this.schema = schema;
            schema.getSchema().keySet().forEach(key -> properties.put(key, ""));
        }

        public Builder addProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }

        public Builder addAllProperties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public ShapefileExporter build() {
            return new ShapefileExporter(this);
        }
    }

}

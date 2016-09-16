package com.nickrobison.trestle.exporter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final File directory;
    private final String prefix;

    private ShapefileExporter(Builder builder) {

//        Setup the export directory first
        final File directory = (File) builder.path.orElse(new File("./target/shapefiles/"));
        if (!directory.exists()) {
            logger.debug("Creating dirctory {}", directory);
            directory.mkdirs();
        }
        this.directory = directory;
        this.prefix = (String) builder.prefix.orElse("");


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
    public File writePropertiesToByteBuffer(List<TSIndividual> individuals, String fileName) throws IOException {
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
        final String exportName;
        if (fileName != null) {
            exportName = String.format("%s_%s", this.prefix, fileName);
        } else {
            exportName = String.format("%s_Export_%s", this.prefix, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        final File shpFile = new File(directory, String.format("%s.shp", exportName));

//        shpFile.createNewFile();
//        dbf.createNewFile();
        final ShapefileDataStoreFactory shapefileDataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        try {
            params.put("url", shpFile.toURI().toURL());
        } catch (MalformedURLException e) {
            logger.error("{} is not a valid URL", shpFile.toURI());
        }
        params.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore dataStore = (ShapefileDataStore) shapefileDataStoreFactory.createDataStore(params);

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

//        Now, zip it
        final File zipFile = new File(directory, String.format("%s.zip", exportName));
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            final ZipOutputStream zos = new ZipOutputStream(fos);
            addToZipArchive(zos,
                    String.format("%s.shp", exportName),
                    String.format("%s.dbf", exportName),
                    String.format("%s.fix", exportName),
                    String.format("%s.prj", exportName),
                    String.format("%s.shx", exportName));
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder<T extends Geometry> {

        private final String typeName;
        private final Class<T> type;
        private final ShapefileSchema schema;
        private final Map<String, Object> properties = new LinkedHashMap<>();
        private Optional<File> path = Optional.empty();
        private Optional<String> prefix = Optional.empty();

        public Builder(String typeName, Class<T> type, ShapefileSchema schema) {
            this.typeName = typeName;
            this.type = type;
            this.schema = schema;
            schema.getSchema().keySet().forEach(key -> properties.put(key, ""));
        }

        public Builder<T> setExportDirectory(File path) {
            this.path = Optional.of(path);
            return this;
        }

        public Builder<T> setExportPrefix(String prefix) {
            this.prefix = Optional.of(prefix);
            return this;
        }

        public ShapefileExporter<Geometry> build() {
            return new ShapefileExporter<>(this);
        }
    }

}

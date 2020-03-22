package com.nickrobison.trestle.exporter;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
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
    private final DefaultFeatureCollection featureCollection;
    private final Class<T> type;
    private final SimpleFeatureType simpleFeatureType;
    private final File directory;
    private final String prefix;
    private final WKTReader reader;

    private ShapefileExporter(ShapefileExporterBuilder builder) {

//        Setup the export directory first
        final File fileDirectory = (File) builder.path.orElse(new File("./target/shapefiles/"));
        if (!fileDirectory.exists()) {
            logger.debug("Creating directory {}", fileDirectory);
            fileDirectory.mkdirs();
        }
        this.directory = fileDirectory;
        this.prefix = (String) builder.getPrefix().orElse("Trestle");


        this.type = builder.type;
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(builder.typeName);

        final Integer srid = (Integer) builder.getSRID().orElse(4326);
        final CoordinateReferenceSystem crs;
        try {

            crs = CRS.decode("EPSG:" + srid);
        } catch (FactoryException e) {
            final String error = String.format("Cannot get CRS for SRID %s", srid);
            logger.error(error, e);
            throw new IllegalArgumentException(error);
        }
        typeBuilder.setCRS(crs);

//        Add the geometry type first
        typeBuilder.add(builder.typeName, builder.type);
        typeBuilder.setDefaultGeometry(builder.typeName);

//        Now the rest of the properties
        builder.schema.getSchema().forEach(typeBuilder::add);

        simpleFeatureType = typeBuilder.buildFeatureType();
        simpleFeatureType.getGeometryDescriptor();
        simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureType);
        featureCollection = new DefaultFeatureCollection();
        this.reader = new WKTReader(new GeometryFactory(new PrecisionModel(), srid));
    }

    @Override
    public DataType exporterType() {
        return DataType.SHAPEFILE;
    }

    @Override
    @SuppressWarnings({"argument.type.incompatible"})
    public File writePropertiesToByteBuffer(List<TSIndividual> individuals, @Nullable String fileName) throws IOException {
        individuals.forEach(individual -> {

//            Build the geometry
            try {
                final String individualGeom = individual.getGeom();

                final T geometry = type.cast(this.reader.read(individualGeom));
                simpleFeatureBuilder.add(geometry);
            } catch (ParseException e) {
                logger.error("Cannot parse wkt {}", e);
            }

//            Now the properties
            individual.getProperties().forEach((key, value) -> simpleFeatureBuilder.add(value));

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

        final ShapefileDataStoreFactory shapefileDataStoreFactory = new ShapefileDataStoreFactory();
        final Map<String, Serializable> params = new HashMap<>();
        try {
            params.put("url", shpFile.toURI().toURL());
        } catch (MalformedURLException e) {
            logger.error("{} is not a valid URL", shpFile.toURI());
        }
        params.put("create spatial index", Boolean.TRUE);
        final ShapefileDataStore dataStore = (ShapefileDataStore) shapefileDataStoreFactory.createDataStore(params);

        dataStore.createSchema(simpleFeatureType);
//            Write it out
        final Transaction transaction = new DefaultTransaction("create");
        try {
            final String typeName = dataStore.getTypeNames()[0];
            final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

                featureStore.setTransaction(transaction);
                featureStore.addFeatures(featureCollection);
                transaction.commit();
            }
        } catch (Exception e) {
            logger.error("Error writing Shapefile properties", e);
            transaction.rollback();
        } finally {
            transaction.close();
        }

//        Now, zip it
        final File zipFile = new File(directory, String.format("%s.zip", exportName));
        final OutputStream fos = Files.newOutputStream(zipFile.toPath());
        final ZipOutputStream zos = new ZipOutputStream(fos);
        try {
            addToZipArchive(zos,
                    String.format("%s.shp", new File(this.directory, exportName).toString()),
                    String.format("%s.dbf", new File(this.directory, exportName).toString()),
                    String.format("%s.fix", new File(this.directory, exportName).toString()),
                    String.format("%s.prj", new File(this.directory, exportName).toString()),
                    String.format("%s.shx", new File(this.directory, exportName).toString()));
            zos.close();
            fos.close();
        } catch (IOException e) {
            logger.error("Error creating zip file", e);
        } finally {
            zos.close();
            fos.close();
        }


        return zipFile;
    }

    private static void addToZipArchive(ZipOutputStream zos, String... fileName) {
        Arrays.stream(fileName).forEach(fn -> {
            final File file = new File(fn);
            final InputStream fileInputStream;
            final ZipEntry zipEntry = new ZipEntry(fn);
            try {
                fileInputStream = Files.newInputStream(file.toPath());
                try {
                    zos.putNextEntry(zipEntry);
                    final byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fileInputStream.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }
                    zos.closeEntry();
                    fileInputStream.close();
                } catch (IOException e) {
                    logger.error("Error closing zip entry", e);
                }
            } catch (IOException e) {
                logger.error("Cannot find file", e);
            }
        });
    }


    public static class ShapefileExporterBuilder<T extends Geometry> {


        private final String typeName;
        private final Class<T> type;
        private final ShapefileSchema schema;
        private final Map<String, Object> properties = new LinkedHashMap<>();
        private Optional<File> path = Optional.empty();
        private Optional<String> prefix = Optional.empty();
        private Optional<Integer> srid = Optional.empty();

        public ShapefileExporterBuilder(String typeName, Class<T> type, ShapefileSchema schema) {
            this.typeName = typeName;
            this.type = type;
            this.schema = schema;
            schema.getSchema().keySet().forEach(key -> properties.put(key, ""));
        }

        public ShapefileExporterBuilder<T> setExportDirectory(File path) {
            this.path = Optional.of(path);
            return this;
        }

        public ShapefileExporterBuilder<T> setExportPrefix(String prefix) {
            this.prefix = Optional.of(prefix);
            return this;
        }

        public ShapefileExporterBuilder<T> setSRID(Integer srid) {
            this.srid = Optional.of(srid);
            return this;
        }

        public String getTypeName() {
            return typeName;
        }

        public Class<T> getType() {
            return type;
        }

        public ShapefileSchema getSchema() {
            return schema;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public Optional<File> getPath() {
            return path;
        }

        public Optional<String> getPrefix() {
            return prefix;
        }

        public Optional<Integer> getSRID() {
            return this.srid;
        }

        public ShapefileExporter<T> build() {
            return new ShapefileExporter<>(this);
        }
    }

}

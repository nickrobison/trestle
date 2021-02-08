package com.nickrobison.trestle.reasoner;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.SharedTestUtils;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 9/15/16.
 */
@SuppressWarnings("Duplicates")
@Tag("integration")
public class DataExporterTests extends AbstractReasonerTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private static List<String> ids = new ArrayList<>();
    private static List<SimpleGAULObject> gaulObjects = new ArrayList<>();

    @BeforeEach
    public void setupData() throws IOException {
        loadData();
    }

    @SuppressWarnings({"dereference.of.nullable", "argument.type.incompatible"})
    private static void loadData() throws IOException {

        SharedTestUtils.ITestClassConstructor<SimpleGAULObject, String> simpleConstructor = (line -> {


            final String[] splitLine = line.split(";");
            final long code;
            try {
                code = Long.parseLong(splitLine[0]);
            } catch (NumberFormatException e) {
                return null;
            }
            LocalDate date = LocalDate.parse(splitLine[2].replace("\"", ""), formatter);
            return new SimpleGAULObject(code, splitLine[1].replace("\"", ""), date, date.plusYears(5), splitLine[4].replace("\"", ""));
        });

        final List<SimpleGAULObject> loadedGAULS = SharedTestUtils.readFromCSV("objects.csv", simpleConstructor);

        loadedGAULS
                .forEach(object -> {
                    if (object != null) {
                        gaulObjects.add(object);
                        ids.add(object.getObjectID());
                    }
                });
    }

    @Test
    @Disabled // FIXME: Re-enable once we have the DataExportEngine migrated
    public void testExport() throws IOException {

        gaulObjects
                .parallelStream()
                .forEach(object -> {
                    try {
                        reasoner.writeTrestleObject(object).blockingAwait();
                    } catch (TrestleClassException | MissingOntologyEntity e) {
                        fail(e);
                    }
                });

//        Verify GeoJSON
        final File file = reasoner.exportDataSetObjects(SimpleGAULObject.class, ids, LocalDate.of(1993, 1, 1), null, ITrestleExporter.DataType.GEOJSON);
        assertTrue(file.length() > 0, "Should have non-zero length");

//        Verify that we actually have something approaching the correct number of values
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonNode = mapper.readTree(file);
        final JsonNode featuresNode = jsonNode.get("features");
//        Should have 197 features, because some don't exist
        assertEquals(197, featuresNode.size(), "Should have 197 features");

//        Remove the file
        assertTrue(file.delete(), "Should be able to delete file");


//        Check for Shapefile
        final File shapeZip = reasoner.exportDataSetObjects(SimpleGAULObject.class, ids, LocalDate.of(1993, 1, 1), null, ITrestleExporter.DataType.SHAPEFILE);

//        We don't actually have to unzip anything, because the files still exist in the directory
//        So that's nice.
        ZipInputStream zis = new ZipInputStream(new FileInputStream(shapeZip));
        ZipEntry zipEntry = zis.getNextEntry();
        String shapeName = "nothing";
//        Go through them and find the shp file
        while (zipEntry != null) {
//            If it's the shp file, try to read it
            final String name = zipEntry.getName();
            if (name.endsWith("shp")) {
                shapeName = name;
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();

        assertNotEquals("nothing", shapeName, "Should have found the shapefile");
        Map<String, Object> map = new HashMap<>();
        map.put("url", new File(shapeName).toURI().toURL());
        final DataStore dataStore = DataStoreFinder.getDataStore(map);
        final String typeName = dataStore.getTypeNames()[0];
        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        assertEquals(197, featureSource.getFeatures().size(), "Should have 197 features");
    }

    @Override
    protected String getTestName() {
        return "hadoop_gaul_exporter";
    }

    @Override
    protected ImmutableList<Class<?>> registerClasses() {
        return ImmutableList.of(SimpleGAULObject.class);
    }

    @DatasetClass(name = "gaul-test")
    public static class SimpleGAULObject implements Serializable {
        private static final long serialVersionUID = 42L;

        @Fact(name = "gaulCode")
        public long gaulCode;
        @Fact(name = "objectName")
        public String objectName;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @StartTemporal(name = "startDate")
        public LocalDate startDate;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @EndTemporal(name = "endDate")
        public LocalDate endDate;
        @Spatial(name = "wkt")
        public String wkt;
        @Ignore
        public double edgeWeight;


        @TrestleCreator
        public SimpleGAULObject(long gaulCode, String objectName, LocalDate startDate, LocalDate endDate, String wkt) {
            this.gaulCode = gaulCode;
            this.objectName = objectName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.wkt = wkt;
            this.edgeWeight = 1.0;
        }

        @IndividualIdentifier
        @Ignore
        public String getObjectID() {
            return Long.toString(this.gaulCode);
        }
    }
}

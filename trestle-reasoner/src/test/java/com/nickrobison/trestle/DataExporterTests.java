package com.nickrobison.trestle;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 9/15/16.
 */
@SuppressWarnings("Duplicates")
public class DataExporterTests {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private static TrestleReasoner reasoner;
    private static List<String> ids = new ArrayList<>();
    private static List<SimpleGAULObject> gaulObjects = new ArrayList<>();

    @BeforeAll
    public static void setup() throws IOException {
        final Config config = ConfigFactory.parseResources("test.configuration.conf").getConfig("trestle.ontology");
        reasoner = new TrestleBuilder()
                .withDBConnection(config.getString("connectionString"),
                        config.getString("username"),
                        config.getString("password"))
                .withName("hadoop_gaul_exporter")
                .withInputClasses(SimpleGAULObject.class)
                .withoutCaching()
                .initialize()
                .build();

        loadData();
    }

    private static void loadData() throws IOException {
        final InputStream is = TrestleAPITest.class.getClassLoader().getResourceAsStream("objects.csv");

        final BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;

        while ((line = br.readLine()) != null) {


            final String[] splitLine = line.split(";");
            final long code;
            try {
                code = Long.parseLong(splitLine[0]);
            } catch (NumberFormatException e) {
                continue;
            }


            LocalDate date = LocalDate.parse(splitLine[2].replace("\"", ""), formatter);
            final String id = UUID.randomUUID().toString();
            gaulObjects.add(new SimpleGAULObject(id, code, splitLine[1].replace("\"", ""), date, date.plusYears(5), splitLine[4].replace("\"", "")));
            ids.add(id);
        }
    }

    @Test
    public void testExport() throws IOException {

        gaulObjects
                .parallelStream()
                .forEach(object -> {
                    try {
                        reasoner.WriteAsTSObject(object);
                    } catch (TrestleClassException | MissingOntologyEntity e) {
                        e.printStackTrace();
                    }
                });

        reasoner.getUnderlyingOntology().runInference();
        final File file = reasoner.exportDataSetObjects(SimpleGAULObject.class, ids, ITrestleExporter.DataType.SHAPEFILE);
        assertTrue(file.length() > 0, "Should have non-zero length");
    }

    @AfterAll
    public static void shutdown() {
        reasoner.shutdown(true);
    }


    @OWLClassName(className = "gaul-test")
    public static class SimpleGAULObject {
        @Ignore
        public UUID objectid;
        @DataProperty(name = "gaulCode")
        public long gaulcode;
        @DataProperty(name = "objectName")
        public String objectname;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @StartTemporalProperty(name = "startDate")
        public LocalDate startdate;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @EndTemporalProperty(name = "endDate")
        public LocalDate enddate;
        @Spatial(name = "wkt")
        public String geom;
        @Ignore
        public double edgeWeight;


        @TrestleCreator
        public SimpleGAULObject(String id, long gaulCode, String objectname, LocalDate startDate, LocalDate endDate, String wkt) {
            this.objectid = UUID.fromString(id);
            this.gaulcode = gaulCode;
            this.objectname = objectname;
            this.startdate = startDate;
            this.enddate = endDate;
            this.geom = wkt;
            this.edgeWeight = 1.0;
        }

        @IndividualIdentifier
        @DataProperty(name = "id")
        @NoMultiLanguage
        public String getObjectID() {
            return this.objectid.toString();
        }
    }
}

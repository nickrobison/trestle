package com.nickrobison.trestle;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.*;
import org.semanticweb.owlapi.model.IRI;

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
@Tag("integration")
@Disabled
public class DataExporterTests {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private static TrestleReasoner reasoner;
    private static List<String> ids = new ArrayList<>();
    private static List<SimpleGAULObject> gaulObjects = new ArrayList<>();

    @BeforeAll
    public static void setup() throws IOException {
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf")).getConfig("trestle.ontology");
        reasoner = new TrestleBuilder()
                .withDBConnection(config.getString("connectionString"),
                        config.getString("username"),
                        config.getString("password"))
                .withName("hadoop_gaul_exporter")
                .withInputClasses(SimpleGAULObject.class)
                .withOntology(IRI.create(config.getString("trestle.ontology.location")))
                .withoutMetrics()
                .withoutCaching()
                .withoutMetrics()
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
            gaulObjects.add(new SimpleGAULObject(code, splitLine[1].replace("\"", ""), date, date.plusYears(5), splitLine[4].replace("\"", "")));
            ids.add(Long.toString(code));
        }
    }

    @Test
    public void testExport() throws IOException {

        gaulObjects
                .stream()
                .forEach(object -> {
                    try {
                        reasoner.writeTrestleObject(object);
                    } catch (TrestleClassException | MissingOntologyEntity e) {
                        e.printStackTrace();
                    }
                });

        reasoner.getUnderlyingOntology().runInference();
        final File file = reasoner.exportDataSetObjects(SimpleGAULObject.class, ids, LocalDate.of(1993, 1, 1), null, ITrestleExporter.DataType.SHAPEFILE);
        assertTrue(file.length() > 0, "Should have non-zero length");
    }

    @AfterAll
    public static void shutdown() {
        reasoner.shutdown(false);
    }


    @DatasetClass(name = "gaul-test")
    public static class SimpleGAULObject implements Serializable {
        private static final long serialVersionUID = 42L;

        @Fact(name = "gaulCode")
        public long gaulcode;
        @Fact(name = "objectName")
        public String objectname;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @StartTemporal(name = "startDate")
        public LocalDate startdate;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @EndTemporal(name = "endDate")
        public LocalDate enddate;
        @Spatial(name = "wkt")
        public String geom;
        @Ignore
        public double edgeWeight;


        @TrestleCreator
        public SimpleGAULObject(long gaulCode, String objectname, LocalDate startDate, LocalDate endDate, String wkt) {
            this.gaulcode = gaulCode;
            this.objectname = objectname;
            this.startdate = startDate;
            this.enddate = endDate;
            this.geom = wkt;
            this.edgeWeight = 1.0;
        }

        @IndividualIdentifier
        @Ignore
        public String getObjectID() {
            return Long.toString(this.gaulcode);
        }
    }
}

package com.nickrobison.trestle.reasoner;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 9/15/16.
 */
@SuppressWarnings("Duplicates")
@Tag("integration")
@Disabled
public class DataExporterTests extends AbstractReasonerTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private static List<String> ids = new ArrayList<>();
    private static List<SimpleGAULObject> gaulObjects = new ArrayList<>();

    @BeforeEach
    public void setupData() throws IOException {
        loadData();
    }

    private static void loadData() throws IOException {
        final InputStream is = TrestleAPITest.class.getClassLoader().getResourceAsStream("objects.csv");

        final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

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

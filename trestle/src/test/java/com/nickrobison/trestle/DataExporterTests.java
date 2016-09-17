package com.nickrobison.trestle;

import afu.edu.emory.mathcs.backport.java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.nickrobison.trestle.TestClasses;
import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.temporal.EndTemporalProperty;
import com.nickrobison.trestle.annotations.temporal.StartTemporalProperty;
import com.nickrobison.trestle.exporter.ITrestleExporter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Created by nrobison on 9/15/16.
 */
public class DataExporterTests {

    private static TrestleReasoner reasoner;
    private static OWLDataFactory df;
    private static String[] ids;

    @BeforeAll
    public static void setup() {
        reasoner = new TrestleBuilder()
                .withDBConnection(
                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
                        "spatialUser",
                        "spatial1")
                .withName("hadoop_gaul5")
                .withInputClasses(SimpleGAULObject.class)
                .withoutCaching()
                .build();

        df = OWLManager.getOWLDataFactory();

        ids = new String[]{"bd0ad9b3-df19-4321-af90-4de510de10eb", "a8cc8e68-676f-4eac-946b-8072cec908ef", "3ff89813-6427-44b9-81eb-53fb1a830de6", "9c93841b-2df6-41c8-a529-9e8fa87e5a77", "edc30fba-01e7-4bcb-95bf-4f382d96be0b", "96e07e83-ea8b-4019-98d1-42da5eec5744", "aedd5ca1-ee67-4733-ae46-0d495fc39792", "31db17dc-21e3-4431-abd0-0a42b4fd715a", "be745ece-acf4-4efa-b98c-96b400b89369", "d986b8c5-b716-42c4-9842-026b4fc88f3c"};
    }

    @Test
    public void testExport() throws IOException {
        final File file = reasoner.exportDataSetObjects(SimpleGAULObject.class, Arrays.asList(ids), ITrestleExporter.DataType.SHAPEFILE);
    }

    @AfterAll
    public static void shutdown() {
        reasoner.shutdown(false);
    }


    @OWLClassName(className = "gaul-test")
    public static class SimpleGAULObject {
        @Ignore
        public UUID objectid;
        @DataProperty(name = "gaulCode")
        public long gaulcode;
        @DataProperty(name = "objectName")
        public String objectname;
        //    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
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
        public SimpleGAULObject(String id, long gaulCode, String objectName, LocalDate startDate, LocalDate endDate, String wkt) {
            this.objectid = UUID.fromString(id);
            this.gaulcode = gaulCode;
            this.objectname = objectName;
            this.startdate = startDate;
            this.enddate = endDate;
            this.geom = wkt;
            this.edgeWeight = 1.0;
        }

        @IndividualIdentifier
        @DataProperty(name = "id")
        public String getObjectID() {
            return this.objectid.toString();
        }
    }
}

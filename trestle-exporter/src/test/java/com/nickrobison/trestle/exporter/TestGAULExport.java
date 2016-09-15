package com.nickrobison.trestle.exporter;

import com.vividsolutions.jts.geom.MultiPolygon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by nrobison on 9/15/16.
 */
@SuppressWarnings("unchecked")
public class TestGAULExport {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private static ShapefileSchema shapefileSchema;
    private static List<TSIndividual> tsIndividuals;


    @BeforeAll
    private static void setup() {
        shapefileSchema = new ShapefileSchema("geom", MultiPolygon.class);
        shapefileSchema.addProperty("gaulcode", String.class);
        shapefileSchema.addProperty("objectname", String.class);
        shapefileSchema.addProperty("startdate", String.class);
        shapefileSchema.addProperty("enddate", String.class);

        tsIndividuals = readCSV();
    }

    @Test
    public void ShapeFileExport() {

        final ShapefileExporter exporter = new ShapefileExporter.Builder(shapefileSchema.getGeomName(), shapefileSchema.getGeomType(), shapefileSchema).build();
        exporter.writePropertiesToByteBuffer(tsIndividuals);
    }

    private static List<TSIndividual> readCSV() {
        final InputStream is = TestGAULExport.class.getClassLoader().getResourceAsStream("objects.csv");
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

        return bufferedReader.lines()
                .skip(1)
                .map(line -> {
                    final String[] splitLine = line.split(";");
                    final int code = Integer.parseInt(splitLine[0]);
                    LocalDate startdate = LocalDate.parse(splitLine[2].replace("\"", ""), formatter);
                    LocalDate enddate = LocalDate.parse(splitLine[3].replace("\"", ""), formatter);

                    final TSIndividual tsIndividual = new TSIndividual(splitLine[4].replace("\"", ""), shapefileSchema);
                    tsIndividual.addProperty("startdate", startdate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    tsIndividual.addProperty("objectname", splitLine[1].replace("\"", ""));
                    tsIndividual.addProperty("enddate", enddate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    tsIndividual.addProperty("gaulcode", Integer.toString(code));
                    return tsIndividual;
                })
                .collect(Collectors.toList());

    }
}

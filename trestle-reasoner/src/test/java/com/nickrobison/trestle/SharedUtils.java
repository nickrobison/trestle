package com.nickrobison.trestle;

import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.TestClasses.GAULTestClass;
import com.nickrobison.trestle.reasoner.TrestleAPITest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SharedUtils {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");


    public static List<GAULTestClass> readGAULObjects() throws IOException {
        //        Parse the CSV
        List<TestClasses.GAULTestClass> gaulObjects = new ArrayList<>();

        final InputStream is = TrestleAPITest.class.getClassLoader().getResourceAsStream("objects.csv");

        final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;

        while ((line = br.readLine()) != null) {


            final String[] splitLine = line.split(";");
            final int code;
            try {
                code = Integer.parseInt(splitLine[0]);
            } catch (NumberFormatException e) {
                continue;
            }


            LocalDate date = LocalDate.parse(splitLine[2].replace("\"", ""), formatter);
            LocalDate endDate = LocalDate.parse(splitLine[3].replace("\"", ""), formatter);
//            final Instant instant = Instant.from(date);
//            final LocalDateTime startTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

//            Need to add a second to get it to format correctly.
            gaulObjects.add(new TestClasses.GAULTestClass(code,
                    splitLine[1].replace("\"", ""),
                    date.atStartOfDay(),
                    endDate.atStartOfDay(),
                    splitLine[4].replace("\"", "")));
        }

        return gaulObjects;
    }
}

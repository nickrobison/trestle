package com.nickrobison.trestle;

import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.TestClasses.GAULTestClass;
import com.nickrobison.trestle.reasoner.TrestleAPITest;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    public static List<TestClasses.KCProjectionTestClass> readKCProjectionClass(String fileName, String idField) throws IOException {

        List<TestClasses.KCProjectionTestClass> countyObjects = new ArrayList<>();
        final URL resource = TestClasses.class.getClassLoader().getResource(fileName);

        Map<String, Object> map = new HashMap<>();
        map.put("url", resource);

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
                .getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                final Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
                countyObjects.add(new TestClasses.KCProjectionTestClass(
                        Long.parseLong(feature.getAttribute(idField).toString()),
                        feature.getAttribute("NAMELSAD10").toString(),
                        defaultGeometry));
            }
        }
        return countyObjects;
    }

    public static List<TestClasses.CensusProjectionTestClass> readCensusProjectionClass(String fileName, String idField) throws IOException {

        List<TestClasses.CensusProjectionTestClass> countyObjects = new ArrayList<>();
        final URL resource = TestClasses.class.getClassLoader().getResource(fileName);

        Map<String, Object> map = new HashMap<>();
        map.put("url", resource);

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
                .getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                final Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
                countyObjects.add(new TestClasses.CensusProjectionTestClass(
                        Long.parseLong(feature.getAttribute(idField).toString()),
                        feature.getAttribute("NAMELSAD10").toString(),
                        defaultGeometry));
            }
        }
        return countyObjects;
    }
}

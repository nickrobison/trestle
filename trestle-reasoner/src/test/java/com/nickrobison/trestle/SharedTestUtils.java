package com.nickrobison.trestle;

import com.nickrobison.trestle.reasoner.TestClasses;
import com.nickrobison.trestle.reasoner.TestClasses.GAULTestClass;
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
import java.util.*;
import java.util.stream.Collectors;

public class SharedTestUtils {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");


    public interface ICensusTract {
        String getName();
    }

    @FunctionalInterface
    public interface ITestClassConstructor<C, R> {
        C construct(R record);
    }


    public static List<GAULTestClass> readGAULObjects() throws IOException {
        ITestClassConstructor<GAULTestClass, String> gaulConstructor = (line -> {


            final String[] splitLine = line.split(";");
            final int code;
            try {
                code = Integer.parseInt(splitLine[0]);
            } catch (NumberFormatException e) {
                return null;
            }


            LocalDate date = LocalDate.parse(splitLine[2].replace("\"", ""), formatter);
            LocalDate endDate = LocalDate.parse(splitLine[3].replace("\"", ""), formatter);

//            Need to add a second to get it to format correctly.
            return new TestClasses.GAULTestClass(code,
                    splitLine[1].replace("\"", ""),
                    date.atStartOfDay(),
                    endDate.atStartOfDay(),
                    splitLine[4].replace("\"", ""));
        });
//        Filter out null objects
        return readFromCSV("objects.csv", gaulConstructor)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static <T> List<T> readFromCSV(String fileName, ITestClassConstructor<T, String> constructor) throws IOException {
        //        Parse the CSV
        List<T> csvObjects = new ArrayList<>();

        final InputStream is = SharedTestUtils.class.getClassLoader().getResourceAsStream(fileName);
        final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                csvObjects.add(constructor.construct(line));
            }

            return csvObjects;
        } finally {
            br.close();
            is.close();
        }
    }

    public static <T extends ICensusTract> List<T> readFromShapeFiles(String fileName, ITestClassConstructor<T, SimpleFeature> constructor) throws IOException {

        List<T> shapefileObjects = new ArrayList<>();
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
                shapefileObjects.add(constructor.construct(feature));
            }
        }
        return shapefileObjects;
    }
}

package com.nickrobison.trestle.covidintegrator;

import com.nickrobison.trestle.datasets.CensusCounty;
import com.nickrobison.trestle.datasets.CovidCounty;
import com.nickrobison.trestle.datasets.CensusState;
import com.nickrobison.trestle.datasets.CovidState;
import com.nickrobison.trestle.ontology.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.reasoner.exceptions.TrestleClassException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nickrobison on 3/22/20.
 */
public class CovidDataLoader {

    private static final LocalDate START_DATE = LocalDate.of(1990, 1, 1);

    private final TrestleReasoner reasoner;
    private final File inputDirectory;
    private final FileDataStoreFactorySpi dsFactory;

    CovidDataLoader(TrestleReasoner reasoner, String inputDirectory) {
        this.reasoner = reasoner;
        this.inputDirectory = new File(inputDirectory);

        dsFactory = FileDataStoreFinder.getDataStoreFactory("shp");
    }

    public void load() throws IOException, TrestleClassException, MissingOntologyEntity {
        this.loadStates();
        this.loadCounties();
        this.loadCases();
    }

    private void loadStates() throws IOException, TrestleClassException, MissingOntologyEntity {
        System.out.println("Loading state file");
        // Filter for only shapefiles, and ignore special files from MacOS
        final IOFileFilter fileFilter = FileFilterUtils.and(FileFilterUtils.suffixFileFilter("shp"),
                FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")),
                FileFilterUtils.prefixFileFilter("tl_2019_us_state"));

        final Collection<File> files = FileUtils.listFiles(this.inputDirectory,
                fileFilter, TrueFileFilter.INSTANCE);

        for (final File file : files) {
            SimpleFeatureIterator features = readShapefile(file).features();

            while (features.hasNext()) {
                final SimpleFeature feature = features.next();
                this.reasoner.writeTrestleObject(stateSupplier(feature));
            }
        }
        System.out.println("Finished loading state file");
    }

    private void loadCounties() throws IOException, TrestleClassException, MissingOntologyEntity {
        System.out.println("Loading counties file");
        // Filter for only shapefiles, and ignore special files from MacOS
        final IOFileFilter fileFilter = FileFilterUtils.and(FileFilterUtils.suffixFileFilter("shp"),
                FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")),
                FileFilterUtils.prefixFileFilter("tl_2019_us_county"));

        final Collection<File> files = FileUtils.listFiles(this.inputDirectory,
                fileFilter, TrueFileFilter.INSTANCE);

        for (final File file : files) {
            SimpleFeatureIterator features = readShapefile(file).features();

            while (features.hasNext()) {
                final SimpleFeature feature = features.next();
                this.reasoner.writeTrestleObject(countySupplier(feature));
            }
        }
    }

    private void loadCases() throws IOException, TrestleClassException, MissingOntologyEntity {
        System.out.println("Loading counties file");
        // Filter for only shapefiles, and ignore special files from MacOS
        final IOFileFilter fileFilter = FileFilterUtils.and(FileFilterUtils.suffixFileFilter("csv"),
                FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")),
                FileFilterUtils.prefixFileFilter("covid19_county"));

        final Collection<File> files = FileUtils.listFiles(this.inputDirectory,
                fileFilter, TrueFileFilter.INSTANCE);

        for (final File file : files) {
            importCSV(file);
        }
        System.out.println("Finished loading cases");
    }

    private void importCSV(File file) throws IOException, TrestleClassException, MissingOntologyEntity {
        System.out.println(String.format("Loading: %s", file.toString()));
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] row = line.split(",");
                if (row[0].equals("County Name")) {
                    continue;
                }
                try {
                    final CovidCounty covidCounty = ccSupplier(row);
                    reasoner.writeTrestleObject(covidCounty);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println(e.getMessage());
                    System.err.println("Assigning values to state");
                    reasoner.writeTrestleObject(csSupplier(row));
                }
            }
        }
    }

    private SimpleFeatureCollection readShapefile(File file) throws IOException {
        final Map<String, Serializable> params = new HashMap<>();
        params.put("url", file.toURI().toURL());

        final DataStore dataStore = dsFactory.createDataStore(params);


        final String typeName = dataStore.getTypeNames()[0];

        final SimpleFeatureSource featureSource = dataStore
                .getFeatureSource(typeName);

        return featureSource.getFeatures();
    }

    public static void main(String[] args) throws IOException, TrestleClassException, MissingOntologyEntity {
        System.out.println("Loading Census data");
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf"));

        TrestleReasoner reasoner = new TrestleBuilder()
                .withDBConnection("http://localhost:7200", "", "")
                .withName("covid")
//                .withOntology(IRI.create(config.getString("trestle.ontology.location")))
                .withPrefix("http://trestle.nickrobison.com/covid/")
                .withInputClasses(CensusState.class, CensusCounty.class, CovidCounty.class, CovidState.class)
                .withoutMetrics()
                .withoutCaching()
                .initialize()
                .build();

        final CovidDataLoader loader = new CovidDataLoader(reasoner, "data");
        loader.load();

    }

    private static CensusState stateSupplier(SimpleFeature feature) {
        final MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometry();
        final String geoid = (String) feature.getAttribute("GEOID");
        final String name = (String) feature.getAttribute("NAME");
        final String abbreviation = (String) feature.getAttribute("STUSPS");

        return new CensusState(geom, geoid, name, abbreviation, START_DATE);
    }

    private static CensusCounty countySupplier(SimpleFeature feature) {
        final MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometry();
        final String geoid = (String) feature.getAttribute("GEOID");
        final String name = (String) feature.getAttribute("NAME");
        final String statefp = (String) feature.getAttribute("STATEFP");

        return new CensusCounty(geom, geoid, name, statefp, START_DATE);
    }

    private static CovidCounty ccSupplier(String[] row) {
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
        final String geoid = String.format("%s%s", StringUtils.leftPad(row[9], 2, "0"), StringUtils.leftPad(row[10], 3, "0"));
        final int confirmed = Integer.parseInt(row[2]);
        final LocalDate of = LocalDate.parse(row[8], fmt);
        return new CovidCounty(geoid, confirmed, of);
    }

    private static CovidState csSupplier(String[] row) {
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
        final int confirmed = Integer.parseInt(row[2]);
        final LocalDate of = LocalDate.parse(row[8], fmt);
        return new CovidState(StringUtils.leftPad(row[9], 2, "0"), confirmed, of);
    }
}

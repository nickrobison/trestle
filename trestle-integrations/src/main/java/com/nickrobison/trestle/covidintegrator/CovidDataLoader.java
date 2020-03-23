package com.nickrobison.trestle.covidintegrator;

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
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    }

    private void loadStates() throws IOException, TrestleClassException, MissingOntologyEntity {
        // Filter for only shapefiles, and ignore special files from MacOS
        final IOFileFilter fileFilter = FileFilterUtils.and(FileFilterUtils.suffixFileFilter("shp"),
                FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")),
                        FileFilterUtils.prefixFileFilter("tl_2019_us_state"));

        final Collection<File> files = FileUtils.listFiles(this.inputDirectory,
                fileFilter, TrueFileFilter.INSTANCE);

        for (final File file : files) {
            final SimpleFeatureIterator features = readShapefile(file).features();

            while (features.hasNext()) {
                final SimpleFeature feature = features.next();
                this.reasoner.writeTrestleObject(stateSupplier(feature));
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
                .withPrefix("https://nickrobison.com/covid")
                .withInputClasses(CovidState.class)
                .withoutMetrics()
                .withoutCaching()
                .initialize()
                .build();

        final CovidDataLoader loader = new CovidDataLoader(reasoner, "data/census");
        loader.load();

    }

    private static CovidState stateSupplier(SimpleFeature feature) {
        final MultiPolygon geom = (MultiPolygon) feature.getDefaultGeometry();
        final String geoid = (String) feature.getAttribute("GEOID");
        final String name = (String) feature.getAttribute("NAME");
        final String abbreviation = (String) feature.getAttribute("STUSPS");

        return new CovidState(geom, geoid, name, abbreviation, START_DATE);
    }
}

package com.nickrobison.gaulintegrator;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Takes the GAUL datasets in the resources/ folder and creates a subset based on a set of ADM0 codes
 */
public class GAULSubsetEngine {

    private final File inputDirectory;
    private final File outputDirectory;
    private final String codes;
    private final FileDataStoreFactorySpi dsFactory;

    private GAULSubsetEngine(String inputDirectory, String outputDirectory, String... codes) {
        this.inputDirectory = new File(inputDirectory);
        this.outputDirectory = new File(outputDirectory);
        this.codes = String.join(",", codes);

        dsFactory = FileDataStoreFinder.getDataStoreFactory("shp");
    }

    public void subsetData() throws IOException, CQLException {

//        Empty the output directory
        FileUtils.cleanDirectory(this.outputDirectory);
//        Filter for only shapefiles, and ignore special files from MacOS
        final IOFileFilter fileFilter = FileFilterUtils.and(FileFilterUtils.suffixFileFilter("shp"),
                FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));
        //        List all files, recursively
        final Collection<File> files = FileUtils.listFiles(this.inputDirectory,
                fileFilter, TrueFileFilter.INSTANCE);

//        Create a new progress bar
        final ProgressBar pb = new ProgressBar("Filtering data:", files.size());
        pb.start();

        for (final File file : files) {
            final SimpleFeatureCollection filteredFeatures = this.readShapefile(file);
            this.writeShapefile(filteredFeatures, file.getName());
            pb.step();
        }
        pb.stop();
    }

    private SimpleFeatureCollection readShapefile(File file) throws IOException, CQLException {
        final Map<String, Serializable> params = new HashMap<>();
        params.put("url", file.toURI().toURL());

        final DataStore dataStore = dsFactory.createDataStore(params);


        final String typeName = dataStore.getTypeNames()[0];

        final SimpleFeatureSource featureSource = dataStore
                .getFeatureSource(typeName);

//        Filter based on ADM0 codes
        final Filter filter = ECQL.toFilter(String.format("ADM0_CODE IN (%s)", this.codes));

        return featureSource.getFeatures(filter);
    }

    private void writeShapefile(SimpleFeatureCollection features, String fileName) throws IOException {
//        File to write to output directory, adding the file name
        final File outputFile = new File(this.outputDirectory, fileName);
        Map<String, java.io.Serializable> creationParams = new HashMap<>();
        creationParams.put("url", outputFile.toURI().toURL());

        final DataStore dataStore = this.dsFactory.createDataStore(creationParams);
        final String typeName = dataStore.getTypeNames()[0];

        dataStore.createSchema(features.getSchema());

        final SimpleFeatureStore featureStore = (SimpleFeatureStore) dataStore.getFeatureSource(typeName);

        try (DefaultTransaction dt = new DefaultTransaction()) {
            featureStore.addFeatures(features);
            dt.commit();
        }
    }

    public static void main(String[] args) throws IOException, CQLException {
        new GAULSubsetEngine(args[0], args[1], Arrays.copyOfRange(args, 2, args.length)).subsetData();
    }
}

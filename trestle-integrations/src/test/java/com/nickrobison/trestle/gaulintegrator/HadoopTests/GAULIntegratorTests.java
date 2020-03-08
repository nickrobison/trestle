package com.nickrobison.trestle.gaulintegrator.HadoopTests;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.trestle.gaulintegrator.*;
import com.nickrobison.trestle.gaulintegrator.sorting.GAULMapperADM2CodeComparator;
import com.nickrobison.trestle.gaulintegrator.sorting.GAULPartitioner;
import com.nickrobison.trestle.gaulintegrator.sorting.NaturalKeyGroupingComparator;
import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.junit.jupiter.api.*;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by nrobison on 5/5/16.
 */
@SuppressWarnings({"argument.type.incompatible", "initialization.fields.uninitialized", "OptionalGetWithoutIsPresent"})
@Tags({@Tag("integration"), @Tag("load")})
public class GAULIntegratorTests {

    private static FileSystem fileSystem;
    private static HdfsConfiguration conf;
    private static MiniDFSCluster cluster;
    private static TrestleReasoner reasoner;
    private static String connectionString;
    private static String userName;
    private static String password;
    private static String ontologyName;
    private static String ontologyPath;
    private static String ontologyPrefix;

    @BeforeAll
    public static void setup() throws IOException {


        final File baseDir = new File("./target/hdfs/gaul-test").getAbsoluteFile();
        FileUtil.fullyDelete(baseDir);
        conf = new HdfsConfiguration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());

        final Properties userProperties = new Properties();
        final InputStream is = IntegrationRunner.class.getClassLoader().getResourceAsStream("sd.properties");
        userProperties.load(is);

        for (String name : userProperties.stringPropertyNames()) {
            conf.set(name, userProperties.getProperty(name));
        }

        fileSystem = FileSystem.get(conf);
//        Uncomment for running aginst the output of GAUL Subsetter
//        fileSystem = FileSystem.getLocal(conf);
        final YarnConfiguration clusterConf = new YarnConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();

        connectionString = "http://localhost:7200";
        userName = "";
        password = "";
        ontologyPath = "file:///Users/nickrobison/Developer/git/dissertation/trestle-ontology/trestle.owl";
        ontologyPrefix = "http://trestle.nickrobison.com/demonstration/";
        ontologyName = "trestle_regionalization";
        conf.set("reasoner.db.connection", connectionString);
        conf.set("reasoner.db.username", userName);
        conf.set("reasoner.db.password", password);
        conf.set("reasoner.ontology.name", ontologyName);

        conf.set("reasoner.ontology.path", ontologyPath);
        conf.set("reasoner.ontology.prefix", ontologyPrefix);
        conf.set("reasoner.ontology.location", ontologyPath);

//        Uncomment to restrict Mapper to a subset of GAUL codes
//        conf.set("gaulcode.restriction", "59");

//        Setup reasoner
        reasoner = new TrestleBuilder()
                .withDBConnection(connectionString, userName, password)
                .withInputClasses(GAULObject.class)
                .withOntology(IRI.create(ontologyPath))
                .withPrefix(ontologyPrefix)
                .initialize()
                .withName(ontologyName)
                .withoutCaching()
                .withoutMetrics()
                .build();

        reasoner.shutdown(false);
    }

    @Test
    public void testReducer() throws IOException, ClassNotFoundException, InterruptedException, SQLException, URISyntaxException {

        URL IN_DIR = GAULIntegratorTests.class.getClassLoader().getResource("gates_test/");

        Path inDir = new Path(IN_DIR.toString());
//        Uncomment to use GAUL subsetter
//        Path inDir = fileSystem.makeQualified(new Path("/Volumes/LaCie/gaul/"));
//        Path inDir = fileSystem.makeQualified(new Path("/Volumes/Macintosh HD/gaul/"));
        //        URL IN_DIR = GAULIntegratorTests.class.getClassLoader().getResource("shapefiles/sudan/");
        Path outDir = new Path("./target/out/");

        fileSystem.delete(outDir, true);

        Job job = Job.getInstance(conf, "GAUL Integrator");
        job.setJarByClass(IntegrationRunner.class);
        job.setMapperClass(GAULMapper.class);
        job.setMapOutputKeyClass(GAULMapperKey.class);
        job.setMapOutputValueClass(MapperOutput.class);
//        Deterministic sorting and partitioning, very course grained, we'll just do country code
//        We also need both the grouping and the sort comparator, not entirely sure why
        job.setGroupingComparatorClass(NaturalKeyGroupingComparator.class);
        job.setSortComparatorClass(GAULMapperADM2CodeComparator.class);
        job.setPartitionerClass(GAULPartitioner.class);
        job.setReducerClass(GAULReducer.class);

//        Add ontology to cache
        job.addCacheFile(new URI(String.format("%s", ontologyPath)));

        job.setInputFormatClass(PolygonFeatureInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        FileInputFormat.setInputDirRecursive(job, false);
        FileInputFormat.setInputPaths(job, inDir);
        FileOutputFormat.setOutputPath(job, outDir);
        job.waitForCompletion(true);
        assertTrue(job.isSuccessful());
//
//        Try to find some individuals
        reasoner = new TrestleBuilder()
                .withDBConnection(connectionString, userName, password)
                .withInputClasses(GAULObject.class)
                .withOntology(IRI.create(ontologyPath))
                .withPrefix(ontologyPrefix)
                .withName(ontologyName)
                .withoutCaching()
                .withoutMetrics()
                .build();

        final Optional<List<GAULObject>> manhicaMembers = reasoner.getCollectionMembers(GAULObject.class, "21884-Manhica:collection", 0.01, null, null);
        assertAll(() -> assertTrue(manhicaMembers.isPresent(), "Should have Manhica collection members"),
                () -> assertEquals(3, manhicaMembers.get().size(), "Wrong number of members for Manhica"));

//        Try for Cidade
        final Optional<List<GAULObject>> cidadeMembers = reasoner.getCollectionMembers(GAULObject.class, "41374-Cidade_de_Maputo:collection", 0.01, null, null);
        assertAll(() -> assertTrue(manhicaMembers.isPresent(), "Should have Cidade collection members"),
                () -> assertEquals(7, cidadeMembers.get().size(), "Wrong number of members for Cidade"));

    }

    @AfterAll
    public static void close() {
        cluster.shutdown();
        reasoner.shutdown(false);
    }
}

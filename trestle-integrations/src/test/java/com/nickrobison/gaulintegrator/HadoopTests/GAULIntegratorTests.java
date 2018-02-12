package com.nickrobison.gaulintegrator.HadoopTests;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.gaulintegrator.*;
import com.nickrobison.gaulintegrator.sorting.GAULMapperADM2CodeComparator;
import com.nickrobison.gaulintegrator.sorting.GAULPartitioner;
import com.nickrobison.gaulintegrator.sorting.NaturalKeyGroupingComparator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(GAULIntegratorTests.class);
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
//        fileSystem = FileSystem.getLocal(conf);
        final YarnConfiguration clusterConf = new YarnConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();

//        connectionString = "jdbc:virtuoso://localhost:1111";
//        userName = "dba";
//        password = "dba";
//        connectionString = "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial";
//        userName = "spatialUser";
//        password = "spatial1";
        connectionString = "http://localhost:7200";
        userName = "";
        password = "";
        ontologyPath = "file:///Users/nickrobison/Developer/git/dissertation/trestle-ontology/trestle.owl";
        ontologyPrefix = "http://nickrobison.com/test/gaul/";
        ontologyName = "hadoop_gaul_expanded_equality";
        conf.set("reasoner.db.connection", connectionString);
        conf.set("reasoner.db.username", userName);
        conf.set("reasoner.db.password", password);
        conf.set("reasoner.ontology.name", ontologyName);

        conf.set("reasoner.ontology.path", ontologyPath);
        conf.set("reasoner.ontology.prefix", ontologyPrefix);
        conf.set("reasoner.ontology.location", ontologyPath);

//        conf.set("gaulcode.restriction", "59");

//        Setup reasoner
        reasoner = new TrestleBuilder()
                .withDBConnection(connectionString, userName, password)
                .withInputClasses(GAULObject.class)
                .withOntology(IRI.create(ontologyPath))
                .withPrefix(ontologyPrefix)
                .initialize()
                .withName(ontologyName)
//                FIXME(nrobison): Caching just doesn't work, so we should disable it until we merge TRESTLE-206
                .withoutCaching()
                .withoutMetrics()
                .build();

        File outputFile = new File("/Users/nrobison/Desktop/hadoop.owl");
        reasoner.writeOntology(outputFile.toURI(), true);
        reasoner.shutdown(false);
    }

    @Test
    public void testReducer() throws IOException, ClassNotFoundException, InterruptedException, SQLException, URISyntaxException {

//        URL IN_DIR = GAULIntegratorTests.class.getClassLoader().getResource("shapefiles/sudan/");
                URL IN_DIR = GAULIntegratorTests.class.getClassLoader().getResource("out/");
        URL OUT_DIR = GAULIntegratorTests.class.getClassLoader().getResource("out/");

        Path inDir = new Path(IN_DIR.toString());
//        Path inDir = fileSystem.makeQualified(new Path("/Volumes/LaCie/gaul/"));
//        Path inDir = fileSystem.makeQualified(new Path("/Volumes/Macintosh HD/gaul/"));
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
//                FIXME(nrobison): Caching just doesn't work, so we should disable it until we merge TRESTLE-206
                .withoutCaching()
                .withoutMetrics()
                .build();

        final Optional<List<GAULObject>> manhicaMembers = reasoner.getConceptMembers(GAULObject.class, "Manhica:concept", 0.01, null, null);
        assertAll(() -> assertTrue(manhicaMembers.isPresent(), "Should have Manhica concept members"),
                () -> assertEquals(3, manhicaMembers.get().size(), "Wrong number of members for Manhica"));

//        Try for Cidade
        final Optional<List<GAULObject>> cidadeMembers = reasoner.getConceptMembers(GAULObject.class, "Cidade_de_Maputo:concept", 0.01, null, null);
        assertAll(() -> assertTrue(manhicaMembers.isPresent(), "Should have Cidade concept members"),
                () -> assertEquals(7, cidadeMembers.get().size(), "Wrong number of members for Cidade"));

    }

    @AfterAll
    public static void close() throws IOException {
        cluster.shutdown();
        reasoner.shutdown(false);
    }
}
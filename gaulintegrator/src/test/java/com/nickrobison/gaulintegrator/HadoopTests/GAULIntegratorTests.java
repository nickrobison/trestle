package com.nickrobison.gaulintegrator.HadoopTests;

import com.esri.mapreduce.PolygonFeatureInputFormat;
import com.nickrobison.gaulintegrator.*;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 5/5/16.
 */
public class GAULIntegratorTests {

    private static FileSystem fileSystem;
    private static HdfsConfiguration conf;
    private static MiniDFSCluster cluster;

    private static final Logger logger = LoggerFactory.getLogger(GAULIntegratorTests.class);
    private static TrestleReasoner reasoner;

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
        final YarnConfiguration clusterConf = new YarnConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).build();

        String connectionString = "jdbc:virtuoso://localhost:1111";
        String userName = "dba";
        String password = "dba";
        conf.set("reasoner.db.connection", connectionString);
        conf.set("reasoner.db.username", userName);
        conf.set("reasoner.db.password", password);

//        Setup reasoner
        reasoner = new TrestleReasoner.TrestleBuilder()
                .withDBConnection(connectionString, userName, password)
                .withInputClasses(GAULObject.class)
                .initialize()
                .withName("hadoop_test")
                .build();
//        reasoner.shutdown(false);
    }

    @Test
    public void testReducer() throws IOException, ClassNotFoundException, InterruptedException {

        URL IN_DIR = GAULIntegratorTests.class.getClassLoader().getResource("shapefiles/gates-test/");
        URL OUT_DIR = GAULIntegratorTests.class.getClassLoader().getResource("out/");

        Path inDir = new Path(IN_DIR.toString());
        Path outDir = new Path("./target/out/");

        fileSystem.delete(outDir, true);

        Job job = Job.getInstance(conf, "GAUL Integrator");
        job.setJarByClass(IntegrationRunner.class);
        job.setMapperClass(GAULMapper.class);
        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(MapperOutput.class);
        job.setReducerClass(GAULReducer.class);

        job.setInputFormatClass(PolygonFeatureInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        FileInputFormat.setInputDirRecursive(job, false);
        FileInputFormat.setInputPaths(job, inDir);
        FileOutputFormat.setOutputPath(job, outDir);
        job.waitForCompletion(true);
        assertTrue(job.isSuccessful());

//        Try to find some individuals
        final ITrestleOntology ontology = reasoner.getUnderlyingOntology();

        final String maputo = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
                "SELECT *" +
                " WHERE { ?m rdf:type :gaul-test . ?m :objectName ?n FILTER(?n = \"Cidade de Maputo\"^^<http://www.w3.org/2001/XMLSchema#string>)}";

        final ResultSet resultSet1 = ontology.executeSPARQL(maputo);

        String id = "http://nickrobison.com/dissertation/trestle.owl#7ceb69d2-3a88-4e57-a706-bdacc4a9402b";
        String retrievedValue = "";
        while (resultSet1.hasNext()) {
            final QuerySolution querySolution = resultSet1.nextSolution();
            retrievedValue = querySolution.getResource("m").getURI();
        }

        assertEquals(id, retrievedValue, "Should have the correct id from the database");


        final String relatedObjects = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
                "SELECT ?f WHERE { ?m rdf:type :gaul-test .?m :has_relation ?r .?r rdf:type :Concept_Relation .?r :Relation_Strength ?s .?r :has_relation ?f .?f rdf:type :gaul-test FILTER(?m = :7ceb69d2-3a88-4e57-a706-bdacc4a9402b && ?s >= \"0.6\"^^xsd:double)}";

        final ResultSet resultSet = ontology.executeSPARQL(relatedObjects);

    }

    @AfterAll
    public static void close() throws IOException {
        cluster.shutdown();

        File outputFile = new File("/Users/nrobison/Desktop/hadoop.owl");
        reasoner.writeOntology(outputFile.toURI(), true);
        reasoner.shutdown(false);
    }
}
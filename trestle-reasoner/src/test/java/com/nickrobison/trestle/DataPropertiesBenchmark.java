package com.nickrobison.trestle;

import com.nickrobison.trestle.common.JenaUtils;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.ontology.JenaOntology;
import com.nickrobison.trestle.parser.ClassParser;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.nickrobison.trestle.common.StaticIRI.hasFactIRI;

/**
 * Created by nrobison on 10/23/16.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@State(Scope.Thread)
@BenchmarkMode(Mode.SingleShotTime)
public class DataPropertiesBenchmark {

    private JenaOntology ontology;
    private TrestleReasoner reasoner;
    private OWLNamedIndividual owlNamedIndividual;
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static QueryBuilder qb;

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(DataPropertiesBenchmark.class.getName() + ".*")
                .forks(1)
//        .warmupTime(TimeValue.seconds(5))
                .warmupIterations(3)
//        .measurementTime(TimeValue.seconds(3))
                .measurementIterations(5)
                .build()).run();
    }

    @Setup
    public void init() throws OWLOntologyCreationException, TrestleClassException, MissingOntologyEntity {
        reasoner = new TrestleBuilder()
                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
//                .withDBConnection(
//                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
//                        "spatialUser",
//                        "spatial1")
                .withName("api_test")
                .withIRI(IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl"))
                .withInputClasses(TestClasses.GAULTestClass.class,
                        TestClasses.GAULComplexClassTest.class,
                        TestClasses.JTSGeometryTest.class,
                        TestClasses.ESRIPolygonTest.class,
                        TestClasses.GeotoolsPolygonTest.class,
                        TestClasses.OffsetDateTimeTest.class)
                .withoutCaching()
                .initialize()
                .build();

        ontology = (JenaOntology) reasoner.getUnderlyingOntology();

//        Write a test class
        final TestClasses.GAULComplexClassTest gaulComplexClassTest = new TestClasses.GAULComplexClassTest();
        reasoner.writeObjectAsFact(gaulComplexClassTest);
        owlNamedIndividual = ClassParser.GetIndividual(gaulComplexClassTest);
        qb = new QueryBuilder(ontology.getUnderlyingPrefixManager());
    }

    @Benchmark
    public Object standardMethod() {
        final Set<OWLObjectPropertyAssertionAxiom> factIndividuals = new HashSet<>();
        final CopyOnWriteArraySet<OWLDataPropertyAssertionAxiom> retrievedDataProperties = new CopyOnWriteArraySet<>();
        final String objectPropertySPARQL = qb.buildObjectPropertyRetrievalQuery(owlNamedIndividual, null, null);
        final ResultSet resultSet = ontology.executeSPARQL(objectPropertySPARQL);
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            final Resource f = next.getResource("f");
            factIndividuals.add(df.getOWLObjectPropertyAssertionAxiom(
                    df.getOWLObjectProperty(hasFactIRI),
                    owlNamedIndividual,
                    df.getOWLNamedIndividual(IRI.create(f.getURI()))));

        }
        factIndividuals.parallelStream()
                .map(individual -> {
//                    final TrestleTransaction tt = ontology.createandOpenNewTransaction(trestleTransaction, false);
                    final Set<OWLDataPropertyAssertionAxiom> allDataPropertiesForIndividual = ontology.getAllDataPropertiesForIndividual(individual.getObject().asOWLNamedIndividual());
//                    ontology.returnAndCommitTransaction(tt);
                    return allDataPropertiesForIndividual;
                })
                .forEach(propertySet -> propertySet.forEach(retrievedDataProperties::add));
        return retrievedDataProperties;

    }

//    @Benchmark
//    public Object optimizedMethod() {
//
//    }

    @Benchmark
    public Object sparqlMethod() {
        final Set<OWLDataPropertyAssertionAxiom> retrievedDataProperties = new HashSet<>();
        final String objectPropertySPARQL = qb.buildObjectPropertyRetrievalQueryOptimized(owlNamedIndividual, null, null);
        final ResultSet resultSet = ontology.executeSPARQL(objectPropertySPARQL);
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            final Optional<OWLLiteral> owlLiteral = JenaUtils.parseLiteral(df, next.getLiteral("o"));
            if (owlLiteral.isPresent()) {
                retrievedDataProperties.add(df.getOWLDataPropertyAssertionAxiom(
                        df.getOWLDataProperty(next.getResource("p").getURI()),
                        owlNamedIndividual,
                        owlLiteral.get()
                ));
            }
        }
        return retrievedDataProperties;
    }

    @Benchmark
    public Object sparqlOptimized() {
        final Set<OWLDataPropertyAssertionAxiom> retrievedDataProperties = new HashSet<>();
        final Map<String, Literal> values = new HashMap<>();
        final String objectPropertySPARQL = qb.buildObjectPropertyRetrievalQueryOptimized(owlNamedIndividual, null, null);
        final ResultSet resultSet = ontology.executeSPARQL(objectPropertySPARQL);
        while (resultSet.hasNext()) {
            final QuerySolution next = resultSet.next();
            values.put(next.getResource("p").getURI(), next.getLiteral("o"));
        }

        values.entrySet().forEach(entry -> {
            final Optional<OWLLiteral> owlLiteral = JenaUtils.parseLiteral(df, entry.getValue());
            if (owlLiteral.isPresent()) {
                retrievedDataProperties.add(df.getOWLDataPropertyAssertionAxiom(
                        df.getOWLDataProperty(entry.getKey()),
                        owlNamedIndividual,
                        owlLiteral.get()));
            }
        });
        return retrievedDataProperties;
    }

    @TearDown
    public void shutdown() {
        reasoner.shutdown(true);
    }


}

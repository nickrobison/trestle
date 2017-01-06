package com.nickrobison.trestle;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.Lock;
import org.apache.jena.tdb.transaction.TDBTransactionException;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import virtuoso.jena.driver.VirtModel;

/**
 * Created by nrobison on 12/21/16.
 */
@SuppressWarnings("Duplicates")
@State(Scope.Thread)
@BenchmarkMode({Mode.Throughput, Mode.SingleShotTime})
public class DataWriterBenchmark {

    private static final String PREFIX = "http://test.owl#";
    private InfModel model;
    private VirtModel virtModel;
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private OWLClassAssertionAxiom owlClassAssertionAxiom;
    private OWLNamedIndividual individual;
    private OWLClass aClass;
    private OWLDataPropertyAssertionAxiom dataProperty;

    @Setup
    public void setupTests() {
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf")).getConfig("trestle.ontology");
        virtModel = VirtModel.openDatabaseModel("create-bench",
                config.getString("connectionString"),
                config.getString("username"),
                config.getString("password"));
        model = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), virtModel);

//        Create the individual
        individual = df.getOWLNamedIndividual(IRI.create(PREFIX, "individual"));
        aClass = df.getOWLClass(IRI.create(PREFIX, "class"));
        owlClassAssertionAxiom = df.getOWLClassAssertionAxiom(aClass,
                individual);

        dataProperty = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(IRI.create(PREFIX, "testProperty")),
                individual, df.getOWLLiteral("test property value"));
    }

    @TearDown
    public void tearDown() {
        virtModel.removeAll();
        model.close();
        virtModel.close();
    }


    @Benchmark
    public void createIndividual() {
        this.model.enterCriticalSection(Lock.WRITE);
        try {
            final Resource modelResource = model.getResource(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual().toStringID());
            final Resource modelClass = model.getResource(owlClassAssertionAxiom.getClassExpression().asOWLClass().toStringID());
            if (model.contains(modelResource, RDF.type, modelClass)) {
                return;
            }
            modelResource.addProperty(RDF.type, modelClass);

        } catch (TDBTransactionException e) {
        } finally {
            this.model.leaveCriticalSection();
        }
    }

    @Benchmark
    public void optimizedIndividual() {
        this.model.enterCriticalSection(Lock.WRITE);
        try {
            final Resource modelResource = model.getResource(owlClassAssertionAxiom.getIndividual().asOWLNamedIndividual().toStringID());
            final Resource modelClass = model.getResource(owlClassAssertionAxiom.getClassExpression().asOWLClass().toStringID());

            modelResource.addProperty(RDF.type, modelClass);

        } catch (TDBTransactionException e) {
        } finally {
            this.model.leaveCriticalSection();
        }
    }

    public static void main(String [] args) throws RunnerException {
        final Options options = new OptionsBuilder()
                .include(DataWriterBenchmark.class.getSimpleName())
                .threads(1)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        new Runner(options).run();
    }
}

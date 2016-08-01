package com.nickrobison.trestle.ontology;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import org.openjdk.jmh.annotations.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

/**
 * Created by nrobison on 8/1/16.
 */
@SuppressWarnings({"initialization"})
public class OntologyBenchmark {

    private enum testImpl {
        TDB,
        TDB_TRANS,
        VIRTUOSO,
        VIRTUOSO_TRANS,
        ORACLE,
        SESAME
    }

    private JenaOntology ontology;
    private OWLDataFactory df;
    private OWLNamedIndividual maputo;
    private OWLDataProperty wkt;
    private OWLDataProperty code;
    private OWLDataPropertyAssertionAxiom assertion;
    @Param({"TDB", "TDB_TRANS", "VIRTUOSO", "VIRTUOSO_TRANS"})
    public testImpl impl;

    @Setup(Level.Trial)
    public void setup() throws OWLOntologyCreationException {
        final IRI iri = IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl");
        df = OWLManager.getOWLDataFactory();
        switch (impl) {

            case TDB: {
                ontology = (JenaOntology) new OntologyBuilder()
                        .fromIRI(iri)
                        .name("trestle_jmh")
                        .build().get();
                break;
            }

            case TDB_TRANS: {
                ontology = (JenaOntology) new OntologyBuilder()
                        .fromIRI(iri)
                        .name("trestle_jmh")
                        .build().get();
                ontology.openAndLock(true);
                break;
            }

            case VIRTUOSO: {
                ontology = (JenaOntology) new OntologyBuilder()
                        .fromIRI(iri)
                        .name("trestle_jmh")
                        .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
                        .build().get();
                break;
            }

            case VIRTUOSO_TRANS: {
                ontology = (JenaOntology) new OntologyBuilder()
                        .fromIRI(iri)
                        .name("trestle_jmh")
                        .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
                        .build().get();
                ontology.openAndLock(true);
                break;
            }
        }

        maputo = df.getOWLNamedIndividual(IRI.create("trestle:", "test_maputo"));
        wkt = df.getOWLDataProperty(IRI.create("geosparql:", "asWKT"));
        code = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_CODE"));
        assertion = df.getOWLDataPropertyAssertionAxiom(code, maputo, 4326);
    }

    @Benchmark
    public void measureNameWrite() throws MissingOntologyEntity {

        ontology.writeIndividualDataProperty(assertion);
    }


    @TearDown(Level.Trial)
    public void close() {
        ontology.unlockAndClose();
        ontology.close(true);
    }
}

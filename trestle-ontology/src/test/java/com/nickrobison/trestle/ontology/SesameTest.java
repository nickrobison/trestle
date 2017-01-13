package com.nickrobison.trestle.ontology;

import afu.org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.nickrobison.trestle.common.StaticIRI.hasFactIRI;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 1/11/17.
 */
@Tag("integration")
@Tag("GraphDB")
public class SesameTest {

    private static final Logger logger = LoggerFactory.getLogger(SesameTest.class);
    public static final String TARGET_DATA = "./target/data";
    private RepositoryConnection connection;

    @BeforeEach
    public void setupOntology() throws IOException {
        RepositoryManager repositoryManager = new LocalRepositoryManager(new File(TARGET_DATA));
        repositoryManager.initialize();

        final TreeModel graph = new TreeModel();

        final InputStream is = SesameTest.class.getClassLoader().getResourceAsStream("./ontology/graphdb-defaults.ttl");
        final RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
        parser.setRDFHandler(new StatementCollector(graph));
        parser.parse(is, RepositoryConfigSchema.NAMESPACE);
        is.close();

        final Resource repositoryNode = Models.subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElse(null);
        final RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);
        repositoryConfig.setID("sesame-test");
        repositoryConfig.setTitle("Sesame test application");

        repositoryManager.addRepositoryConfig(repositoryConfig);

        final Repository repository = repositoryManager.getRepository("sesame-test");

        connection = repository.getConnection();

        //        Enable?
        final String enableGEOSPARQL = "PREFIX : <http://www.ontotext.com/plugins/geosparql#> INSERT DATA { _:s :enabled 'true' .}";
        final Update update = connection.prepareUpdate(QueryLanguage.SPARQL, enableGEOSPARQL);
        update.execute();


        connection.begin();
        connection.add(new File("/Users/nrobison/Desktop/trestle.xml"), "urn:base", RDFFormat.RDFXML);
        connection.commit();
    }

    @Test
    public void getSomething() {
        final IRI gaulIRI = SimpleValueFactory.getInstance().createIRI("http://nickrobison.com/dissertation/trestle.owl#", "GAUL");
        final RepositoryResult<Statement> gaul = connection.getStatements((IRI) null, RDF.TYPE, gaulIRI);
        while (gaul.hasNext()) {
            final Statement next = gaul.next();
            logger.info("{}", next.getSubject().toString());
        }

//        Try for inferred
        final IRI factIRI = SimpleValueFactory.getInstance().createIRI(hasFactIRI.getIRIString());
        final IRI individualIRI = SimpleValueFactory.getInstance().createIRI("http://nickrobison.com/dissertation/trestle.owl#", "maputo:2013:3000");
        final RepositoryResult<Statement> factStatements = connection.getStatements(individualIRI, factIRI, (IRI) null);
        while (factStatements.hasNext()) {
            final Statement next = factStatements.next();
            logger.info("{} has fact {}", next.getSubject(), next.getObject());
        }

//        After everything?
        final IRI afterIRI = SimpleValueFactory.getInstance().createIRI("http://nickrobison.com/dissertation/trestle.owl#", "after");
        final IRI earlyIRI = SimpleValueFactory.getInstance().createIRI("http://nickrobison.com/dissertation/trestle.owl#", "earlyTime:1900:1989");
        final RepositoryResult<Statement> statements = connection.getStatements(individualIRI, afterIRI, (IRI) null);
        int afterCount = 0;
        while (statements.hasNext()) {
            final Statement next = statements.next();
            logger.info("{} is after {}", next.getSubject(), next.getObject());
            afterCount++;
        }
        assertEquals(5, afterCount, "Wrong number of afters");

//        SPARQL?
        final String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "SELECT DISTINCT ?m ?p ?o WHERE { ?m rdf:type :GAUL . ?m ?p ?o. ?p rdfs:subPropertyOf :Temporal_Relation . " +
                "VALUES ?m {<http://nickrobison.com/dissertation/trestle.owl#municipal1:1990:2013>} }";

        TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
        int sparqlCount = 0;
        while (tupleQueryResult.hasNext()) {
            final BindingSet next = tupleQueryResult.next();
            logger.info("{} is after {}", next.getBinding("m").toString(), next.getBinding("o").toString());
            sparqlCount++;
        }

        assertEquals(12, sparqlCount, "SPARQL should return all temporal relations");


//        Try for intersection

        final String spatialString = "BASE <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX : <http://nickrobison.com/test/trestle.owl#>\n" +
                "PREFIX trestle: <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n" +
                "SELECT DISTINCT ?m WHERE { ?m rdf:type trestle:GAUL .?m trestle:has_fact ?f .?f geo:asWKT ?wkt .FILTER(geof:sfIntersects(?wkt, 'MULTIPOLYGON(((32.9915311 -25.9701193999999,32.9553558000001 -26.0791321,32.9391123 -26.0327745,32.9136869000001 -26.0241227999999,32.9257746000001 -26.0450888,32.9044608040001 -26.0701784999999,32.8960634000001 -26.0516855999999,32.9143349 -26.0033473,32.9427438000001 -25.9861478999999,32.9573777000001 -25.9982697,32.9915311 -25.9701193999999)),((32.5096456000001 -25.967816,32.5549668 -25.9741010999999,32.5695855000001 -26.0133028,32.6053314210001 -26.0416069029999,32.5701370240001 -26.0840835569999,32.53560257 -26.075172424,32.4560050960001 -26.091545105,32.4411926270001 -26.0584659579999,32.4727706910001 -26.0565986629999,32.4908142090001 -26.035755157,32.4928904000001 -25.9757911,32.5096456000001 -25.967816)),((32.9103849900001 -25.9692587509999,32.9221983 -25.9703081,32.9139718000001 -25.982851,32.9009066 -25.973668,32.9103849900001 -25.9692587509999)),((32.611003876 -25.9445438389999,32.5932554 -25.9838586999999,32.5529831420001 -25.96678773,32.611003876 -25.9445438389999)))'^^geo:wktLiteral)) }";

        tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, spatialString);
        tupleQueryResult = tupleQuery.evaluate();
        int intersectCount = 0;
        while (tupleQueryResult.hasNext()) {
            final BindingSet next = tupleQueryResult.next();
            logger.info("{} intersects with {}", next.getBinding("m").toString(), next.getBinding("o").toString());
            intersectCount++;
        }
        assertEquals(1, intersectCount, "Should intersect with at least 1 object");

    }



    @AfterEach
    public void shutdown() throws IOException {
//        connection.close();
        FileUtils.deleteDirectory(new File(TARGET_DATA));
    }
}

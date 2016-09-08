package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.TestClasses;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 9/6/16.
 */
@SuppressWarnings({"Duplicates", "unchecked"})
public class ConcurrentOntologyGAULLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentOntologyGAULLoader.class);
    private List<TestClasses.GAULTestClass> gaulObjects = new ArrayList<>();
    private OWLDataFactory df;
    private ITrestleOntology ontology;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");

    @BeforeEach
    public void setup() throws IOException, OWLOntologyCreationException {

        final InputStream is = OracleOntologyGAULoader.class.getClassLoader().getResourceAsStream("objects.csv");

        final BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line;

        while ((line = br.readLine()) != null) {


            final String[] splitLine = line.split(";");
            final int code;
            try {
                code = Integer.parseInt(splitLine[0]);
            } catch (NumberFormatException e) {
                continue;
            }


            LocalDate date = LocalDate.parse(splitLine[2].replace("\"", ""), formatter);
//            final Instant instant = Instant.from(date);
//            final LocalDateTime startTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

//            Need to add a second to get it to format correctly.
            gaulObjects.add(new TestClasses.GAULTestClass(code, splitLine[1].replace("\"", ""), date.atStartOfDay().plusSeconds(1), splitLine[4].replace("\"", "")));
        }

        final IRI iri = IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl");
        df = OWLManager.getOWLDataFactory();

        ontology = new OntologyBuilder()
                .fromIRI(iri)
//                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
//                .withDBConnection(
//                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
//                        "spatialUser",
//                        "spatial1")
                .name("trestle_concurrent_1")
                .build().get();

        ontology.initializeOntology();
    }

    @Test
    public void testRAWDataLoader() throws MissingOntologyEntity, OWLOntologyStorageException, SQLException, InterruptedException, ExecutionException {

        OWLClass datasetClass = df.getOWLClass(IRI.create("trestle:", "GAUL_Test"));

        final OWLClass temporalClass = df.getOWLClass(IRI.create("trestle:", "Temporal_Object"));
        final OWLObjectProperty temporal_of = df.getOWLObjectProperty(IRI.create("trestle:", "temporal_of"));
        final OWLDataProperty valid_from = df.getOWLDataProperty(IRI.create("trestle:", "valid_from"));
        final OWLDataProperty valid_to = df.getOWLDataProperty(IRI.create("trestle:", "valid_to"));

//        final ExecutorCompletionService<> completionService = new ExecutorCompletionService<>(Executors.newFixedThreadPool(5));
//        final ExecutorService executorService = Executors.newFixedThreadPool(2);
//        final ExecutorService executorService = Executors.newCachedThreadPool();
        final ExecutorService executorService = Executors.newWorkStealingPool();
//        for (TestClasses.GAULTestClass gaul : gaulObjects) {
//            completionService.submit(CompletableFuture.runAsync(() -> {


//                datasetClass = ClassParser.GetObjectClass(gaul);

        final List<CompletableFuture<Void>> completableFutures = gaulObjects
                .stream()
                .map(gaul -> CompletableFuture.runAsync(() -> {
                    final OWLNamedIndividual gaulIndividual = ClassParser.GetIndividual(gaul);
                    final OWLClassAssertionAxiom testClass = df.getOWLClassAssertionAxiom(datasetClass, gaulIndividual);
                    logger.debug("Writing main individual");
                    ontology.createIndividual(testClass);

                    //        Write the data properties
                    final Optional<List<OWLDataPropertyAssertionAxiom>> gaulDataProperties = ClassParser.GetDataProperties(gaul);
                    gaulDataProperties.orElseThrow(() -> new RuntimeException("missing data properties")).forEach(dataAxiom -> {
                        try {
                            ontology.writeIndividualDataProperty(dataAxiom);
                        } catch (MissingOntologyEntity missingOntologyEntity) {
                            logger.error("Missing entity {}", gaul.adm0_name, missingOntologyEntity);
                        }
                    });
//                    final List<CompletableFuture<Void>> propertiesFuture = gaulDataProperties.orElseThrow(() -> new RuntimeException("missing data properties"))
//                            .stream()
//                            .map(dataAxiom -> CompletableFuture.runAsync(() -> {
//                                try {
//                                    logger.debug("Writing property {} for individual {}", dataAxiom.getProperty(), dataAxiom.getSubject());
//                                    ontology.writeIndividualDataProperty(dataAxiom);
//                                } catch (MissingOntologyEntity missingOntologyEntity) {
//                                    logger.error("Missing entity {}", gaul.adm0_name, missingOntologyEntity);
//                                }
//                            }, executorService))
//                            .collect(Collectors.toList());
//                    final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(propertiesFuture.toArray(new CompletableFuture[propertiesFuture.size()]));

                    final Optional<List<TemporalObject>> temporalObjects = TemporalParser.GetTemporalObjects(gaul);
                    for (TemporalObject temporal : temporalObjects.orElseThrow(() -> new RuntimeException("Missing temporals"))) {
                        try {

//                Write the temporal
                            final OWLNamedIndividual temporalIndividual = df.getOWLNamedIndividual(IRI.create("trestle:", temporal.getID()));
                            final OWLClassAssertionAxiom temporalAssertion = df.getOWLClassAssertionAxiom(temporalClass, temporalIndividual);
                            logger.debug("Writing temporal for {}", gaul.adm0_name);
                            ontology.createIndividual(temporalAssertion);

//                Set the object properties to point back to the individual
                            final OWLObjectPropertyAssertionAxiom temporalPropertyAssertion = df.getOWLObjectPropertyAssertionAxiom(temporal_of, temporalIndividual, gaulIndividual);
                            logger.debug("Writing temporal assertion for {}", gaul.adm0_name);
                            ontology.writeIndividualObjectProperty(temporalPropertyAssertion);

//                Write the data properties. I know these are closed intervals
                            final OWLLiteral fromLiteral = df.getOWLLiteral(temporal.asInterval().getFromTime().toString(), OWL2Datatype.XSD_DATE_TIME);
                            final OWLLiteral toLiteral = df.getOWLLiteral(temporal.asInterval().getToTime().get().toString(), OWL2Datatype.XSD_DATE_TIME);
                            final OWLDataPropertyAssertionAxiom fromAssertionAxiom = df.getOWLDataPropertyAssertionAxiom(valid_from, temporalIndividual, fromLiteral);
                            final OWLDataPropertyAssertionAxiom toAssertionAxiom = df.getOWLDataPropertyAssertionAxiom(valid_to, temporalIndividual, toLiteral);
                            logger.debug("Writing from assertion for {}", gaul.adm0_name);
                            ontology.writeIndividualDataProperty(fromAssertionAxiom);
                            logger.debug("Writing to assertion for {}", gaul.adm0_name);
                            ontology.writeIndividualDataProperty(toAssertionAxiom);
                        } catch (Exception e) {
                            logger.error("Catching an exception from {}", gaul.adm0_name, e);
                        }
                    }
                    logger.debug("Waiting for properties completion");
//                    voidCompletableFuture.thenAccept(v -> logger.debug("properties written"));
                }, executorService))
                .collect(Collectors.toList());
        logger.info("Waiting to complete");
        Instant start = Instant.now();
        final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]));
        voidCompletableFuture.join();
        Instant end = Instant.now();
        logger.info("Async took {} ms", Duration.between(start, end).toMillis());

//        Check to see if it worked.
        ontology.openAndLock(false);
        final Set<OWLNamedIndividual> gaulInstances = ontology.getInstances(datasetClass, true);
        assertEquals(191, gaulInstances.size(), "Wrong number of GAUL records from instances method");

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#> " +
                "SELECT * WHERE {?m rdf:type :GAUL_Test}";

        List<QuerySolution> resultSet = ResultSetFormatter.toList(ontology.executeSPARQL(queryString));
        assertEquals(191, resultSet.size(), "Wrong number of GAUL records from sparql method");

////        SPARQL Query of spatial intersections.
//        queryString = "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
//                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
//                "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
//                "PREFIX spatial: <http://jena.apache.org/spatial#>\n" +
//                "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
//                "SELECT * { ?s spatial:north (-12.0671005249999 39.5398864750001).}";
////                "SELECT ?m ?wkt WHERE { ?m rdf:type :GAUL_Test . ?m ogc:asWKT ?wkt\n" +
////                "    FILTER ( ?wkt spatial:nearby (-12.0671005249999 39.5398864750001 1000.0 'km')) }";
//
//        resultSet = ontology.executeSPARQL(queryString);
//        assertEquals(2, resultSet.getRowNumber(), "Wrong number of intersected results");


        final OWLNamedIndividual ndorwa = df.getOWLNamedIndividual(IRI.create("trestle:", "Ndorwa"));
        final OWLDataProperty adm0_code = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));
        final Optional<Set<OWLLiteral>> individualDataProperty = ontology.getIndividualDataProperty(ndorwa, adm0_code);
        assertTrue(individualDataProperty.isPresent(), "Should have ADM0_Code");
        assertEquals(28419, individualDataProperty.get().stream().findFirst().get().parseInteger(), "Codes don't match");
        //        Try some inference

//        final OWLObjectProperty has_temporal = df.getOWLObjectProperty(IRI.create("trestle:", "has_temporal"));
//        final Optional<Set<OWLObjectPropertyAssertionAxiom>> has_temporalProperty = ontology.getIndividualObjectProperty(ndorwa, has_temporal);
//        assertTrue(has_temporalProperty.isPresent(), "Should have inferred temporal");
//        assertEquals(1, has_temporalProperty.get().size(), "Should only have 1 temporal");
        ontology.unlockAndCommit();
    }

    @AfterEach
    public void cleanup() {
        ontology.close(true);
    }
}

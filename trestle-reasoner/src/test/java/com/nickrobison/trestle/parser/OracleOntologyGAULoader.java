package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.TestClasses;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.ontology.OracleOntology;
import com.nickrobison.trestle.querybuilder.QueryBuilder;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.common.StaticIRI.TRESTLE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by nrobison on 7/1/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization", "Duplicates"})
@Disabled
public class OracleOntologyGAULoader {

    private List<TestClasses.GAULTestClass> gaulObjects = new ArrayList<>();
    private OWLDataFactory df;
    private OracleOntology ontology;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
    private TrestleParser tp;

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
        final Config config = ConfigFactory.parseResources("test.configuration.conf");
        final IRI iri = IRI.create(config.getString("trestle.ontology.location"));
        df = OWLManager.getOWLDataFactory();

        ontology = (OracleOntology) new OntologyBuilder()
                .withDBConnection(
                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
                        "spatialUser",
                        "spatial1")
                .fromIRI(iri)
                .name("trestle_gaul_loader1")
                .build().get();

        ontology.initializeOntology();

        tp = new TrestleParser(df, TRESTLE_PREFIX);
    }

    @Test
    public void testRAWDataLoader() throws MissingOntologyEntity, OWLOntologyStorageException, SQLException, UnsupportedFeatureException {

        OWLClass datasetClass = df.getOWLClass(IRI.create("trestle:", "GAUL_Test"));

        final OWLClass temporalClass = df.getOWLClass(IRI.create("trestle:", "Temporal_Object"));
        final OWLObjectProperty temporal_of = df.getOWLObjectProperty(IRI.create("trestle:", "temporal_of"));
        final OWLDataProperty valid_from = df.getOWLDataProperty(IRI.create("trestle:", "valid_from"));
        final OWLDataProperty valid_to = df.getOWLDataProperty(IRI.create("trestle:", "valid_to"));

        ontology.openTransaction(false);
//        Create the subclass association
        final OWLClass datasetSuperClass = df.getOWLClass(IRI.create("trestle:", "Dataset"));
        final OWLSubClassOfAxiom owlSubClassOfAxiom = df.getOWLSubClassOfAxiom(datasetClass, datasetSuperClass);
        ontology.associateOWLClass(owlSubClassOfAxiom);

        for (TestClasses.GAULTestClass gaul : gaulObjects) {
            datasetClass = tp.classParser.GetObjectClass(gaul);
            final OWLNamedIndividual gaulIndividual = tp.classParser.GetIndividual(gaul);
            final OWLClassAssertionAxiom testClass = df.getOWLClassAssertionAxiom(datasetClass, gaulIndividual);
            ontology.createIndividual(testClass);


            final Optional<List<TemporalObject>> temporalObjects = tp.temporalParser.GetTemporalObjects(gaul);
            for (TemporalObject temporal : temporalObjects.orElseThrow(() -> new RuntimeException("Missing temporals"))) {

//                Write the temporal
                final OWLNamedIndividual temporalIndividual = df.getOWLNamedIndividual(IRI.create("trestle:", temporal.getID()));
                final OWLClassAssertionAxiom temporalAssertion = df.getOWLClassAssertionAxiom(temporalClass, temporalIndividual);
                ontology.createIndividual(temporalAssertion);

//                Set the object properties to point back to the individual
                final OWLObjectPropertyAssertionAxiom temporalPropertyAssertion = df.getOWLObjectPropertyAssertionAxiom(temporal_of, temporalIndividual, gaulIndividual);
                ontology.writeIndividualObjectProperty(temporalPropertyAssertion);

//                Write the data properties. I know these are closed intervals
                final OWLLiteral fromLiteral = df.getOWLLiteral(temporal.asInterval().getFromTime().toString(), OWL2Datatype.XSD_DATE_TIME);
                final OWLLiteral toLiteral = df.getOWLLiteral(temporal.asInterval().getToTime().get().toString(), OWL2Datatype.XSD_DATE_TIME);
                final OWLDataPropertyAssertionAxiom fromAssertionAxiom = df.getOWLDataPropertyAssertionAxiom(valid_from, temporalIndividual, fromLiteral);
                final OWLDataPropertyAssertionAxiom toAssertionAxiom = df.getOWLDataPropertyAssertionAxiom(valid_to, temporalIndividual, toLiteral);
                ontology.writeIndividualDataProperty(fromAssertionAxiom);
                ontology.writeIndividualDataProperty(toAssertionAxiom);
            }

            //        Write the data properties
            final Optional<List<OWLDataPropertyAssertionAxiom>> gaulDataProperties = tp.classParser.GetDataProperties(gaul);
            for (OWLDataPropertyAssertionAxiom dataAxiom : gaulDataProperties.orElseThrow(() -> new RuntimeException("Missing data properties"))) {

                ontology.writeIndividualDataProperty(dataAxiom);
            }
        }
        ontology.commitTransaction(false);
        ontology.runInference();

//        Check to see if it worked.
        final Set<OWLNamedIndividual> gaulInstances = ontology.getInstances(datasetClass, true);
        assertEquals(191, gaulInstances.size(), "Wrong number of GAUL records from instances method");

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#> " +
                "SELECT * WHERE {?m rdf:type :GAUL_Test}";

        List<QuerySolution> resultSet = ResultSetFormatter.toList(ontology.executeSPARQL(queryString));
        assertEquals(191, resultSet.size(), "Wrong number of GAUL records from sparql method");

//        SPARQL Query of spatial intersections.
        final OWLClass gaul_test = df.getOWLClass(IRI.create("trestle:", "GAUL_Test"));
        QueryBuilder qb = new QueryBuilder(QueryBuilder.DIALECT.ORACLE, ontology.getUnderlyingPrefixManager());
//        queryString = qb.buildOracleIntersection(gaul_test, "Point(39.5398864750001 -12.0671005249999)");
        queryString = qb.buildSpatialIntersection(gaul_test, "Point(39.5398864750001 -12.0671005249999)", 0.0, QueryBuilder.UNITS.METER);

        resultSet = ResultSetFormatter.toList(ontology.executeSPARQL(queryString));
        assertEquals(2, resultSet.size(), "Wrong number of intersected results");

//        Try some inference
        final OWLNamedIndividual ndorwa = df.getOWLNamedIndividual(IRI.create("trestle:", "Ndorwa"));

        final OWLObjectProperty has_temporal = df.getOWLObjectProperty(IRI.create("trestle:", "has_temporal"));
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> has_temporalProperty = ontology.getIndividualObjectProperty(ndorwa, has_temporal);
        assertTrue(has_temporalProperty.isPresent(), "Should have inferred temporal");
        assertEquals(1, has_temporalProperty.get().size(), "Should only have 1 temporal");


        ontology.writeOntology(IRI.create(new File("/Users/nrobison/Desktop/gaul.owl")), false);
    }

    @AfterEach
    public void cleanup() {
        ontology.close(true);
    }
}

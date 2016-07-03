import com.hp.hpl.jena.query.ResultSet;
import com.nickrobison.trestle.common.ClassParser;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.ontology.OracleOntology;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by nrobison on 7/1/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization"})
public class TestGAULoader {

    private List<GAULTestClass> gaulObjects = new ArrayList<>();
    private OWLDataFactory df;
    private OracleOntology ontology;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");

    @Before
    public void setup() throws IOException, OWLOntologyCreationException {

        final InputStream is = TestGAULoader.class.getClassLoader().getResourceAsStream("objects.csv");

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
            gaulObjects.add(new GAULTestClass(code, splitLine[1].replace("\"", ""), date.atStartOfDay().plusSeconds(1), splitLine[4].replace("\"", "")));
        }

        final IRI iri = IRI.create("file:///Users/nrobison/Developer/git/dissertation/trestle-ontology/trestle.owl");
        df = OWLManager.getOWLDataFactory();

        ontology = (OracleOntology) new OntologyBuilder()
                .withDBConnection(
                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
                        "spatialUser",
                        "spatial1")
                .fromIRI(iri)
                .name("trestle")
                .build().get();

        ontology.initializeOntology();
    }

    @Test
    public void testRAWDataLoader() throws MissingOntologyEntity, OWLOntologyStorageException, SQLException {

        OWLClass datasetClass = df.getOWLClass(IRI.create("trestle:", "GAUL_Test"));

        final OWLClass temporalClass = df.getOWLClass(IRI.create("trestle:", "Temporal_Object"));
        final OWLObjectProperty temporal_of = df.getOWLObjectProperty(IRI.create("trestle:", "temporal_of"));
        final OWLDataProperty valid_from = df.getOWLDataProperty(IRI.create("trestle:", "valid_from"));
        final OWLDataProperty valid_to = df.getOWLDataProperty(IRI.create("trestle:", "valid_to"));

        ontology.openTransaction();
        for (GAULTestClass gaul : gaulObjects) {
            datasetClass = ClassParser.GetObjectClass(gaul);
            final OWLNamedIndividual gaulIndividual = ClassParser.GetIndividual(gaul);
            final OWLClassAssertionAxiom testClass = df.getOWLClassAssertionAxiom(datasetClass, gaulIndividual);
            ontology.createIndividual(testClass);

            final Optional<List<TemporalObject>> temporalObjects = ClassParser.GetTemporalObjects(gaul);
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
            final Optional<List<OWLDataPropertyAssertionAxiom>> gaulDataProperties = ClassParser.GetDataProperties(gaul);
            for (OWLDataPropertyAssertionAxiom dataAxiom : gaulDataProperties.orElseThrow(() -> new RuntimeException("Missing data properties"))) {

                ontology.writeIndividualDataProperty(dataAxiom);
            }
        }

        ontology.commitTransaction();
        ontology.runInference();

//        Check to see if it worked.
        final Set<OWLNamedIndividual> gaulInstances = ontology.getInstances(datasetClass, true);
        assertEquals("Wrong number of GAUL records from instances method", 200, gaulInstances.size());

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#> " +
                "SELECT * WHERE {?m rdf:type :GAUL_Test}";

        ResultSet resultSet = ontology.executeSPARQL(queryString);
        assertEquals("Wrong number of GAUL records from sparql method", 200, resultSet.getRowNumber());

//        SPRAQL Query of spatial intersections.
        queryString = "PREFIX : <http://nickrobison.com/dissertation/trestle.owl#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>\n" +
                "PREFIX ogcf: <http://www.opengis.net/def/function/geosparql/>\n" +
                "SELECT ?m ?wkt WHERE { ?m rdf:type :GAUL_Test . ?m ogc:asWKT ?wkt\n" +
                "    FILTER (ogcf:sfIntersects(?wkt, \"Point(39.5398864750001 -12.0671005249999)\"^^ogc:wktLiteral)) }";

        resultSet = ontology.executeSPARQL(queryString);
        assertEquals("Wrong number of intersected results", 3, resultSet.getRowNumber());

//        Try some inference
        final OWLNamedIndividual balama = df.getOWLNamedIndividual(IRI.create("trestle:", "Balama"));

        final OWLObjectProperty has_temporal = df.getOWLObjectProperty(IRI.create("trestle:", "has_temporal"));
        final Optional<Set<OWLObjectProperty>> has_temporalProperty = ontology.getIndividualObjectProperty(balama, has_temporal);
        assertTrue("Should have inferred temporal", has_temporalProperty.isPresent());
        assertEquals("Should only have 1 temporal", 1, has_temporalProperty.get().size());


//        ontology.writeOntology(IRI.create(new File("/Users/nrobison/Desktop/gaul.owl")), false);
    }

    @After
    public void cleanup() {
        ontology.close(false);
    }
}

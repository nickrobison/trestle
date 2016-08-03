package com.nickrobison.trestle;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.parser.GAULTestClass;
import com.nickrobison.trestle.parser.OracleOntologyGAULoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 7/27/16.
 */
@SuppressWarnings({"Duplicates", "initialization"})
public class OracleTrestleTest {

    private TrestleReasoner reasoner;
    private OWLDataFactory df;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");

    @BeforeEach
    public void setup() {
        reasoner = new TrestleReasoner.TrestleBuilder()
                .withDBConnection("jdbc:virtuoso://localhost:1111", "dba", "dba")
//                .withDBConnection(
//                        "jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial",
//                        "spatialUser",
//                        "spatial1")
                .withName("trestle_gaul")
                .withInputClasses(GAULTestClass.class)
                .initialize()
                .build();

        df = OWLManager.getOWLDataFactory();
    }

    @Test
    public void gaulLoader() throws IOException, TrestleClassException, MissingOntologyEntity, OWLOntologyStorageException {
//        Parse the CSV
        List<GAULTestClass> gaulObjects = new ArrayList<>();

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
            gaulObjects.add(new GAULTestClass(code, splitLine[1].replace("\"", ""), date.atStartOfDay().plusSeconds(1), splitLine[4].replace("\"", "")));
        }

//        Write the objects
        gaulObjects.forEach(gaul -> {
            try {
                reasoner.writeObjectAsConcept(gaul);
            } catch (TrestleClassException e) {
                throw new RuntimeException(String.format("Problem storing object %s", gaul.adm0_name), e);
            }
        });

//        Validate Results
        final Set<OWLNamedIndividual> gaulInstances = reasoner.getInstances(GAULTestClass.class);
        assertEquals(191, gaulInstances.size(), "Wrong number of GAUL records from instances method");

//        reasoner.getUnderlyingOntology().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/gaul.owl")), false);

//        Try to read one out.
        final GAULTestClass ancuabe = reasoner.readAsObject(GAULTestClass.class, IRI.create("trestle:", "Ancuabe"));
        assertEquals(ancuabe.adm0_name, "Ancuabe", "Wrong name");


//        Validate correctness
    }


    @AfterEach
    public void close() throws OWLOntologyStorageException {
//        reasoner.getUnderlyingOntology().writeOntology(IRI.create(new File("/Users/nrobison/Desktop/gaul.owl")), true);
        reasoner.shutdown(true);
    }
}

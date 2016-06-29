import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.common.ClassParser;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "initialization"})
public class TrestleParserTest {

    private GAULTestClass test1;
    private MoreGAULTests test2;
    private OWLDataFactory df;

    @Before
    public void Setup() {
        test1 = new GAULTestClass(1234, "test1");
//        test2 = new GAULTestClass(2345, "test2");
        test2 = new MoreGAULTests();
        df = OWLManager.getOWLDataFactory();
    }

    @Test
    public void TestGAULParser() {

//        Test the class
        final OWLClass owlClass = ClassParser.GetObjectClass(test1);
        final OWLClass gaul_test1 = df.getOWLClass(IRI.create("trestle:", "GAUL_Test"));
        assertEquals("Wrong OWL Class", gaul_test1, owlClass);
//        Test the named individual
        OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(test1);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "test_me"));
        assertEquals("Wrong named individual", owlNamedIndividual, gaul_test);

//        Test the data property parser
//        Code
        final OWLDataProperty adm0_code = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));
        final OWLLiteral adm0_code_literal = df.getOWLLiteral("1234", OWL2Datatype.XSD_INTEGER);
        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(test1);
        assertTrue("Should have properties", owlDataPropertyAssertionAxioms.isPresent());
        assertEquals("Wrong number of properties", 2, owlDataPropertyAssertionAxioms.get().size());
        final OWLDataPropertyAssertionAxiom parsed_code = owlDataPropertyAssertionAxioms.get().get(0);
        assertEquals("Wrong named individual", gaul_test, parsed_code.getSubject());
        assertEquals("Data property IRIs don't match", adm0_code, parsed_code.getProperty());
        assertEquals("Data property values are wrong", adm0_code_literal, parsed_code.getObject());

//        Name
        final OWLDataProperty adm0_name = df.getOWLDataProperty(IRI.create("trestle:", "adm0_name"));
        final OWLLiteral adm0_name_literal = df.getOWLLiteral("test1");
        final OWLDataPropertyAssertionAxiom parsed_name = owlDataPropertyAssertionAxioms.get().get(1);
        assertEquals("Wrong named individual", gaul_test, parsed_name.getSubject());
        assertEquals("Data property IRIs don't match", adm0_name, parsed_name.getProperty());
        assertEquals("Data property values are wrong", adm0_name_literal, parsed_name.getObject());

//        Test the new gaul test
        //        Test the named individual
        owlNamedIndividual = ClassParser.GetIndividual(test2);
        gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "test region"));
        assertEquals("Wrong named individual", owlNamedIndividual, gaul_test);

        owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(test2);
        assertTrue("Should have properties", owlDataPropertyAssertionAxioms.isPresent());
        assertEquals("Wrong number of properties", 3, owlDataPropertyAssertionAxioms.get().size());

    }

    @OWLClassName(className = "GAUL_Test")
    protected class MoreGAULTests {

        public int adm0_code;
        @IndividualIdentifier
        public String adm0_name;
        public String test_name;
        @TemporalObject(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        @Ignore
        public LocalDateTime testtime;

        public MoreGAULTests() {
            this.adm0_code = 4326;
            this.test_name = "new_test";
            this.adm0_name = "test region";
            this.testtime = LocalDateTime.now();
        }
    }




}

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.TemporalProperty;
import com.nickrobison.trestle.common.ClassParser;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.nickrobison.trestle.types.temporal.TemporalObjectBuilder.exists;
import static com.nickrobison.trestle.types.temporal.TemporalObjectBuilder.valid;
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
    private TemporalObject temporal;
    private TemporalObject temporalPoint;

    @Before
    public void Setup() {
        test1 = new GAULTestClass(1234, "test1");
        test2 = new MoreGAULTests();
        df = OWLManager.getOWLDataFactory();
        LocalDateTime dt = LocalDateTime.of(1989, 3, 26, 0, 0);
        temporal = valid().from(dt).to(dt.plusYears(1)).withRelations();
        temporalPoint = exists().at(dt).withRelations();
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
        assertEquals("Wrong number of properties", 3, owlDataPropertyAssertionAxioms.get().size());
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

//        Test the temporal
        final Optional<List<TemporalObject>> temporalObjects = ClassParser.GetTemporalObjects(test2);
        assertTrue("Should have objects", temporalObjects.isPresent());
        assertEquals("Wrong number of objects", 2, temporalObjects.get().size());
//        Check for the same type and scope for interval
        assertEquals("Wrong temporal type", temporal.getType(), temporalObjects.get().get(0).getType());
        assertEquals("Wrong temporal scope", temporal.getScope(), temporalObjects.get().get(0).getScope());
        assertEquals("Wrong # of temporal relations", 1, temporalObjects.get().get(0).getTemporalRelations().size());
        assertEquals("Wrong temporal relation", gaul_test, temporalObjects.get().get(0).getTemporalRelations().stream().findFirst().get());

//        Check point
        assertEquals("Wrong temporal type", temporalPoint.getType(), temporalObjects.get().get(1).getType());
        assertEquals("Wrong temporal scope", temporalPoint.getScope(), temporalObjects.get().get(1).getScope());
        assertEquals("Wrong # of temporal relations", 1, temporalObjects.get().get(1).getTemporalRelations().size());
        assertEquals("Wrong temporal relation", gaul_test, temporalObjects.get().get(1).getTemporalRelations().stream().findFirst().get());

    }

    @OWLClassName(className = "GAUL_Test")
    protected class MoreGAULTests {

        public int adm0_code;
        @IndividualIdentifier
        public String adm0_name;
        @Spatial
        public String test_name;
        @TemporalProperty(type = TemporalType.INTERVAL, duration = 1, unit = ChronoUnit.YEARS)
        @Ignore
        public LocalDateTime testtime;
        private String privateField;
        @TemporalProperty(type = TemporalType.POINT, scope= TemporalScope.EXISTS, duration = 0, unit = ChronoUnit.YEARS)
        public LocalDateTime testpoint;
        public MoreGAULTests() {
            this.adm0_code = 4326;
            this.test_name = "new_test";
            this.adm0_name = "test region";
            this.testtime = LocalDateTime.of(1998, 3, 26, 0, 0);
            this.privateField = "don't read me";
            this.testpoint = LocalDateTime.of(1989, 3, 26, 0, 0);
        }
    }




}

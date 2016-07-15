package com.nickrobison.trestle.common;

import com.nickrobison.trestle.annotations.*;
import com.nickrobison.trestle.annotations.TemporalProperty;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.nickrobison.trestle.types.temporal.TemporalObjectBuilder.exists;
import static com.nickrobison.trestle.types.temporal.TemporalObjectBuilder.valid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @BeforeEach
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
        assertEquals(gaul_test1, owlClass, "Wrong OWL Class");
//        Test the named individual
        OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(test1);
        OWLNamedIndividual gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "test_me"));
        assertEquals(owlNamedIndividual, gaul_test, "Wrong named individual");

//        Test the data property parser
//        Code
        final OWLDataProperty adm0_code = df.getOWLDataProperty(IRI.create("trestle:", "ADM0_Code"));
        final OWLLiteral adm0_code_literal = df.getOWLLiteral("1234", OWL2Datatype.XSD_INTEGER);
        Optional<List<OWLDataPropertyAssertionAxiom>> owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(test1);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(3, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");
        final OWLDataPropertyAssertionAxiom parsed_code = owlDataPropertyAssertionAxioms.get().get(0);
        assertEquals(gaul_test, parsed_code.getSubject(), "Wrong named individual");
        assertEquals(adm0_code, parsed_code.getProperty(), "Data property IRIs don't match");
        assertEquals(adm0_code_literal, parsed_code.getObject(), "Data property values are wrong");

//        Name
        final OWLDataProperty adm0_name = df.getOWLDataProperty(IRI.create("trestle:", "adm0_name"));
        final OWLLiteral adm0_name_literal = df.getOWLLiteral("test1");
        final OWLDataPropertyAssertionAxiom parsed_name = owlDataPropertyAssertionAxioms.get().get(1);
        assertEquals(gaul_test, parsed_name.getSubject(), "Wrong named individual");
        assertEquals(adm0_name, parsed_name.getProperty(), "Data property IRIs don't match");
        assertEquals(adm0_name_literal, parsed_name.getObject(), "Data property values are wrong");

//        Test the new gaul test
        //        Test the named individual
        owlNamedIndividual = ClassParser.GetIndividual(test2);
        gaul_test = df.getOWLNamedIndividual(IRI.create("trestle:", "test region"));
        assertEquals(owlNamedIndividual, gaul_test, "Wrong named individual");

        owlDataPropertyAssertionAxioms = ClassParser.GetDataProperties(test2);
        assertTrue(owlDataPropertyAssertionAxioms.isPresent(), "Should have properties");
        assertEquals(4, owlDataPropertyAssertionAxioms.get().size(), "Wrong number of properties");

//        Test the temporal
        final Optional<List<TemporalObject>> temporalObjects = ClassParser.GetTemporalObjects(test2);
        assertTrue(temporalObjects.isPresent(), "Should have objects");
        assertEquals(2, temporalObjects.get().size(), "Wrong number of objects");
//        Check for the same type and scope for interval
        assertEquals(temporal.getType(), temporalObjects.get().get(0).getType(), "Wrong temporal type");
        assertEquals(temporal.getScope(), temporalObjects.get().get(0).getScope(), "Wrong temporal scope");
        assertEquals(1, temporalObjects.get().get(0).getTemporalRelations().size(), "Wrong # of temporal relations");
        assertEquals(gaul_test, temporalObjects.get().get(0).getTemporalRelations().stream().findFirst().get(), "Wrong temporal relation");

//        Check point
        assertEquals(temporalPoint.getType(), temporalObjects.get().get(1).getType(), "Wrong temporal type");
        assertEquals(temporalPoint.getScope(), temporalObjects.get().get(1).getScope(), "Wrong temporal scope");
        assertEquals(1, temporalObjects.get().get(1).getTemporalRelations().size(), "Wrong # of temporal relations");
        assertEquals(gaul_test, temporalObjects.get().get(1).getTemporalRelations().stream().findFirst().get(), "Wrong temporal relation");

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

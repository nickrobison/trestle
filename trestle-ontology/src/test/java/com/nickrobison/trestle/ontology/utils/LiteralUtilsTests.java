package com.nickrobison.trestle.ontology.utils;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import static com.nickrobison.trestle.ontology.utils.SharedLiteralUtils.parseNumericDatatype;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nickrobison on 7/3/20.
 */
public class LiteralUtilsTests {

    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    @Test
    void testLiteralParsing() {
        assertEquals(df.getOWLDatatype(OWL2Datatype.XSD_DECIMAL), parseNumericDatatype("3.14"), "Should be double");
        assertEquals(df.getOWLDatatype(OWL2Datatype.XSD_INTEGER), parseNumericDatatype("3"), "Should be int");
        assertEquals(df.getOWLDatatype(OWL2Datatype.XSD_LONG), parseNumericDatatype(Long.toString(Long.MAX_VALUE)), "Should be long");
    }
}

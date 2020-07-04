package com.nickrobison.trestle.ontology.utils;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nickrobison on 7/3/20.
 */
public class LiteralFactoryTests {

    public static RDF4JLiteralFactory factory;

    @BeforeAll
    static void setup() {
        factory = new RDF4JLiteralFactory(OWLManager.getOWLDataFactory(), SimpleValueFactory.getInstance());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void testLiteralRoundTrip() {
        // Simple integer literal
        final OWLLiteral owlIntLiteral = factory.getDataFactory().getOWLLiteral(10);
        final Literal intLiteral = factory.createLiteral(owlIntLiteral);
        assertAll(() -> assertEquals(owlIntLiteral.parseInteger(), intLiteral.intValue(), "Should have correct integer"),
                () -> assertEquals(owlIntLiteral.getDatatype().toStringID(), intLiteral.getDatatype().toString(), "Should have correct datatype"));

        final OWLLiteral intOptional = factory.createOWLLiteral(intLiteral);
        assertEquals(owlIntLiteral, intOptional, "Should convert correctly");

        // Simple double literal
        final OWLLiteral owlDoubleLiteral = factory.getDataFactory().getOWLLiteral("3.14", OWL2Datatype.XSD_DECIMAL);
        final Literal doubleLiteral = factory.createLiteral(owlDoubleLiteral);
        assertAll(() -> assertEquals(owlDoubleLiteral.parseDouble(), doubleLiteral.doubleValue(), "Should have correct double"),
                () -> assertEquals(owlDoubleLiteral.getDatatype().toStringID(), doubleLiteral.getDatatype().toString(), "Should have correct datatype"));
        assertEquals(owlDoubleLiteral, factory.createOWLLiteral(doubleLiteral), "Should convert correctly");

        // Language literal
        final OWLLiteral owlLangLiteral = factory.getDataFactory().getOWLLiteral("this is a test", "es");
        final Literal langLiteral = factory.createLiteral(owlLangLiteral);
        assertAll(() -> assertEquals(owlLangLiteral.getLiteral(), langLiteral.stringValue(), "Should have correct string"),
                () -> assertEquals(owlLangLiteral.getDatatype().toStringID(), langLiteral.getDatatype().toString(), "Should have correct datatype"),
                () -> assertEquals(owlLangLiteral.getLang(), langLiteral.getLanguage().get(), "Should have correct language"));
        assertEquals(owlLangLiteral, factory.createOWLLiteral(langLiteral), "Should convert correctly");
    }

    @Test
    void testLiteralExceptions() {
        final SimpleValueFactory vf = factory.getValueFactory();
        // Null URI
        final Literal nullLiteral = Mockito.mock(Literal.class);
        Mockito.when(nullLiteral.getDatatype()).thenReturn(null);
        Mockito.when(nullLiteral.stringValue()).thenReturn("I am null");
        final OWLLiteral nullOptional = factory.createOWLLiteral(nullLiteral);
        assertAll(() -> assertEquals(OWL2Datatype.XSD_STRING, nullOptional.getDatatype().getBuiltInDatatype(), "Should have string datatype"),
                () -> assertEquals("I am null", nullOptional.getLiteral(), "Should have correct value"));
    }
}

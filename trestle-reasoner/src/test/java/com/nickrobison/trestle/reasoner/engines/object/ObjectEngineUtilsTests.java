package com.nickrobison.trestle.reasoner.engines.object;

import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Created by nickrobison on 2/8/21
 */
public class ObjectEngineUtilsTests {

    private static final String TEST_PREFIX = "http://nickrobison.com/test-owl";

    private ITrestleOntology ontology;
    private TrestleParser parser;
    private ObjectEngineUtils utils;
    private final OWLDataFactory df = OWLManager.getOWLDataFactory();


    @BeforeEach
    void setup() {
        this.ontology = Mockito.mock(ITrestleOntology.class);
        this.parser = Mockito.mock(TrestleParser.class);
        this.utils = new ObjectEngineUtils(parser, ontology, TEST_PREFIX);
    }

    @AfterEach
    void cleanup() {
        Mockito.reset(ontology, parser);
    }

    @Test
    void testAdjustedQueryTemporal() {
        Mockito.when(this.ontology.getTemporalsForIndividual(Mockito.any())).thenAnswer(answer -> {
            final OWLIndividual individual = answer.getArgument(0);

            final OWLDataPropertyAssertionAxiom p1 = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty("http://nickrobison.com/dissertation/trestle.owl#end_temporal"),
                    individual,
                    df.getOWLLiteral("019-01-11T00:00:00Z", OWL2Datatype.XSD_DATE_TIME));
            final OWLDataPropertyAssertionAxiom p2 = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty("http://nickrobison.com/dissertation/trestle.owl#exists_to"),
                    individual,
                    df.getOWLLiteral("2019-01-11T00:00:00Z", OWL2Datatype.XSD_DATE_TIME));

            final OWLDataPropertyAssertionAxiom p3 = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty("<http://nickrobison.com/dissertation/trestle.owl#start_temporal"),
                    individual,
                    df.getOWLLiteral("2018-01-11T00:00:00Z", OWL2Datatype.XSD_DATE_TIME));

            final OWLDataPropertyAssertionAxiom p4 = df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty("http://nickrobison.com/dissertation/trestle.owl#exists_from"),
                    individual,
                    df.getOWLLiteral("2018-01-11T00:00:00Z", OWL2Datatype.XSD_DATE_TIME));
            return Flowable.fromArray(p1, p2, p3, p4);
        });

        this.utils.getAdjustedQueryTemporal("100111", OffsetDateTime.now(), null)
                .test()
                .assertComplete()
                .assertValue(LocalDateTime.parse("2019-01-10T23:59:59.999999"))
                .assertNoErrors();

        this.utils.getAdjustedQueryTemporal("100111", OffsetDateTime.of(LocalDateTime.of(2017, 1, 1, 1, 1), ZoneOffset.UTC), null)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(LocalDateTime.parse("2018-01-11T00:00"));

        final OffsetDateTime at = OffsetDateTime.of(LocalDateTime.of(2018, 10, 1, 1, 1), ZoneOffset.UTC);

        this.utils.getAdjustedQueryTemporal("100111", at, null)
                .test()
                .assertComplete()
                .assertNoErrors()
                .assertValue(at);


        Mockito.verify(this.ontology, Mockito.times(3)).returnAndCommitTransaction(Mockito.any());
        Mockito.verify(this.ontology, Mockito.times(0)).returnAndAbortTransaction(Mockito.any());
    }

    @Test
    void testNoTemporals() {
        Mockito.when(this.ontology.getTemporalsForIndividual(Mockito.any())).thenAnswer(answer -> Flowable.empty());

        this.utils.getAdjustedQueryTemporal("100111", OffsetDateTime.now(), null)
                .test()
                .assertError(RuntimeException.class);
        Mockito.verify(this.ontology, Mockito.times(0)).returnAndCommitTransaction(Mockito.any());
        Mockito.verify(this.ontology, Mockito.times(1)).returnAndAbortTransaction(Mockito.any());
    }
}

package com.nickrobison.trestle.reasoner;

import com.google.common.collect.ImmutableList;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public abstract class AbstractReasonerTest {
    protected static final String OVERRIDE_PREFIX = "http://nickrobison.com/test-owl#";
    protected TrestleParser tp;
    protected TrestleReasonerImpl reasoner;
    protected OWLDataFactory df;

    @BeforeEach
    public void setup() {
        final Config config = ConfigFactory.load(ConfigFactory.parseResources("application.conf"));
        reasoner = (TrestleReasonerImpl) new TrestleBuilder()
                .withDBConnection(config.getString("trestle.ontology.connectionString"),
                        config.getString("trestle.ontology.username"),
                        config.getString("trestle.ontology.password"))
                .withName(getTestName())
                .withOntology(IRI.create(config.getString("trestle.ontology.location")))
                .withPrefix(AbstractReasonerTest.OVERRIDE_PREFIX)
                .withInputClasses(registerClasses().toArray(new Class<?>[registerClasses().size()]))
                .withoutCaching()
                .withoutMetrics()
                .initialize()
                .build();

        df = OWLManager.getOWLDataFactory();
        tp = new TrestleParser(df, AbstractReasonerTest.OVERRIDE_PREFIX, false, null);
    }

    @AfterEach
    public void close() throws OWLOntologyStorageException {
        Assertions.assertEquals(reasoner.getUnderlyingOntology().getOpenedTransactionCount(), reasoner.getUnderlyingOntology().getCommittedTransactionCount() + reasoner.getUnderlyingOntology().getAbortedTransactionCount(), "Should have symmetric opened/aborted+committed transactions");
        reasoner.shutdown(true);
    }

    protected abstract String getTestName();

    protected abstract ImmutableList<Class<?>> registerClasses();
}

package com.nickrobison.trestle.reasoner.engines.temporal;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Metriced
public class TemporalEngine {
    private static final Logger logger = LoggerFactory.getLogger(TemporalEngine.class);
    private static final String MISSING_TEMPORAL = "%s must have temporal objects";

    private final TrestleParser tp;

    @Inject
    public TemporalEngine(TrestleParser trestleParser) {
        this.tp = trestleParser;
    }

    @Timed
    public <A, B> TemporalComparisonReport compareObjects(A objectA, B objectB) {

        final OWLNamedIndividual objectAID = this.tp.classParser.getIndividual(objectA);
        final OWLNamedIndividual objectBID = this.tp.classParser.getIndividual(objectB);

        TemporalObject objectATemporal = this.tp.temporalParser.getTemporalObjects(objectA)
                .orElseThrow(() -> new IllegalStateException(String.format(MISSING_TEMPORAL, objectAID))).get(0);
        TemporalObject objectBTemporal = this.tp.temporalParser.getTemporalObjects(objectB)
                .orElseThrow(() -> new IllegalStateException(String.format(MISSING_TEMPORAL, objectBID))).get(0);

        final TemporalComparisonReport comparisonReport = new TemporalComparisonReport(objectAID, objectBID);

//        Does A start B?

        return comparisonReport;
    }
}

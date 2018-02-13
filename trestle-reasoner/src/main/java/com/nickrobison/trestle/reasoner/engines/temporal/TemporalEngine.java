package com.nickrobison.trestle.reasoner.engines.temporal;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.types.relations.ObjectRelation;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import javax.inject.Inject;

@Metriced
public class TemporalEngine {
    private static final String MISSING_TEMPORAL = "%s must have temporal objects";

    private final TrestleParser tp;

    @Inject
    public TemporalEngine(TrestleParser trestleParser) {
        this.tp = trestleParser;
    }

    @Timed
    public <A extends @NonNull Object, B extends @NonNull Object> TemporalComparisonReport compareObjects(A objectA, B objectB) {

        final OWLNamedIndividual objectAID = this.tp.classParser.getIndividual(objectA);
        final OWLNamedIndividual objectBID = this.tp.classParser.getIndividual(objectB);

        TemporalObject objectATemporal = this.tp.temporalParser.getTemporalObjects(objectA)
                .orElseThrow(() -> new IllegalStateException(String.format(MISSING_TEMPORAL, objectAID))).get(0);
        TemporalObject objectBTemporal = this.tp.temporalParser.getTemporalObjects(objectB)
                .orElseThrow(() -> new IllegalStateException(String.format(MISSING_TEMPORAL, objectBID))).get(0);

        final TemporalComparisonReport comparisonReport = new TemporalComparisonReport(objectAID, objectBID);

        if (objectATemporal.equals(objectBTemporal)) {
            comparisonReport.addRelation(ObjectRelation.EQUALS);
        } else if (objectATemporal.meets(objectBTemporal)) {
            comparisonReport.addRelation(ObjectRelation.TEMPORAL_MEETS);
        } else if (objectATemporal.starts(objectBTemporal)) {
            //        A during B?
            comparisonReport.addRelation(ObjectRelation.STARTS);
        } else if (objectATemporal.finishes(objectBTemporal)) {
            comparisonReport.addRelation(ObjectRelation.FINISHES);
        } else if (objectATemporal.during(objectBTemporal)) {
            comparisonReport.addRelation(ObjectRelation.DURING);

//            Is A entirely before B?
        } else if (objectATemporal.compareTo(objectBTemporal.getIdTemporal()) == -1) {
//            Do they meet?
            comparisonReport.addRelation(ObjectRelation.BEFORE);
        } else if (objectATemporal.compareTo(objectBTemporal.getIdTemporal()) == 1) {
//            Do they meet?
            comparisonReport.addRelation(ObjectRelation.AFTER);
        }

        return comparisonReport;
    }
}

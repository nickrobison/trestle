package com.nickrobison.trestle.reasoner.engines.temporal;

import com.nickrobison.trestle.reasoner.engines.AbstractComparisonReport;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

public class TemporalComparisonReport extends AbstractComparisonReport {

    public TemporalComparisonReport(OWLNamedIndividual objectA, OWLNamedIndividual objectB) {
        super(objectA, objectB);
    }


}

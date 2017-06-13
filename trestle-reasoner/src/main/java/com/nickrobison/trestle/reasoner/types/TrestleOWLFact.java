package com.nickrobison.trestle.reasoner.types;

import com.nickrobison.trestle.reasoner.types.temporal.TemporalObject;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

/**
 * Created by nrobison on 6/5/17.
 * Wrapper class for carrying an {@link org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom} along with the required {@link TemporalObject} for valid/database time.
 */
public class TrestleOWLFact {
    private static final long serialVersionUID = 42L;

    private final OWLDataPropertyAssertionAxiom axiom;
    private final TemporalObject validTemporal;
    private final TemporalObject dbTemporal;

    public TrestleOWLFact(OWLDataPropertyAssertionAxiom axiom, TemporalObject validTemporal, TemporalObject dbTemporal) {
        this.axiom = axiom;
        this.validTemporal = validTemporal;
        this.dbTemporal = dbTemporal;
    }

    public OWLDataPropertyAssertionAxiom getAxiom() {
        return axiom;
    }

    public TemporalObject getValidTemporal() {
        return validTemporal;
    }

    public TemporalObject getDbTemporal() {
        return dbTemporal;
    }

    @Override
    public String toString() {
        return "TrestleOWLFact{" +
                "axiom=" + axiom +
                ", validTemporal=" + validTemporal +
                ", dbTemporal=" + dbTemporal +
                '}';
    }
}

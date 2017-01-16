package com.nickrobison.trestle.ontology.types;

import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nrobison on 1/10/17.
 */
public class TrestleResult {

    Map<String, OWLObject> resultValues;

    public TrestleResult() {
        this.resultValues = new HashMap<>();
    }

    /**
     * Add var/object pair to TrestleResult
     * @param varName - String variable name
     * @param owlObject - OWLObject of result variable
     */
    public void addValue(String varName, OWLObject owlObject) {
        this.resultValues.put(varName, owlObject);
    }

    public OWLLiteral getLiteral(String varName) {
        if (resultValues.containsKey(varName)) {
            final OWLObject owlObject = resultValues.get(varName);
            if (owlObject instanceof OWLLiteral) {
                return OWLLiteral.class.cast(owlObject);
            }
        }
        throw new RuntimeException(String.format("OWLObject for variable %s is not an OWLLiteral", varName));
    }

    public OWLIndividual getIndividual(String varName) {
        if (resultValues.containsKey(varName)) {
            final OWLObject owlObject = resultValues.get(varName);
            if (owlObject instanceof OWLIndividual) {
                return OWLIndividual.class.cast(owlObject);
            }
        }
        throw new RuntimeException(String.format("OWLObject for variable %s is not an OWLIndividual", varName));
    }
}

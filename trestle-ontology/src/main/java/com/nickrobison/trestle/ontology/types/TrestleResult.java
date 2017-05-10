package com.nickrobison.trestle.ontology.types;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by nrobison on 1/10/17.
 */

/**
 * Single row from a {@link TrestleResultSet}, contains an internal {@link Map} of variable/value pairs
 * An empty Optional for a given varName indicates that that variable is unbound
 */
public class TrestleResult {

    private final Map<String, @Nullable OWLObject> resultValues;

    public TrestleResult() {
        this.resultValues = new HashMap<>();
    }

    /**
     * Add var/object pair to TrestleResult
     *
     * @param varName   - String variable name
     * @param owlObject - Nullable {@link OWLObject} of result variable
     */
    public void addValue(String varName, @Nullable OWLObject owlObject) {
        this.resultValues.put(varName, owlObject);
    }

    /**
     * Get the variable as an {@link OWLLiteral}
     * Returns an empty optional if the result is null, meaning the variable is unbound
     * Throws an {@link ClassCastException} if the result is not an {@link OWLLiteral}
     *
     * @param varName - Variable name to access
     * @return - Optional {@link OWLLiteral}
     */
    public Optional<OWLLiteral> getLiteral(String varName) {
        if (resultValues.containsKey(varName)) {
            final OWLObject owlObject = resultValues.get(varName);
            if (owlObject != null) {
                if (owlObject instanceof OWLLiteral) {
                    return Optional.of(OWLLiteral.class.cast(owlObject));
                }
                throw new ClassCastException(String.format("OWLObject for variable %s is not an OWLLiteral", varName));
            }
        }
        return Optional.empty();
    }

    /**
     * Get the variable as an {@link OWLIndividual}
     * Returns an empty optional if the result is null, meaning the variable is unbound
     * Throws an {@link ClassCastException} if the result is not an {@link OWLIndividual}
     *
     * @param varName - Variable name to access
     * @return - Optional {@link OWLIndividual}
     */
    public Optional<OWLIndividual> getIndividual(String varName) {
        if (resultValues.containsKey(varName)) {
            final OWLObject owlObject = resultValues.get(varName);
            if (owlObject != null) {
                if (owlObject instanceof OWLIndividual) {
                    return Optional.of(OWLIndividual.class.cast(owlObject));
                }
            }
            throw new ClassCastException(String.format("OWLObject for variable %s is not an OWLIndividual", varName));
        }
        return Optional.empty();
    }

    @SuppressWarnings({"lambda.param.type.incompatible"})
    public Map<String, String> getResultValues() {
        Map<String, String> stringMap = new HashMap<>();
        this.resultValues
                .entrySet()
                .forEach(entry -> {
                    stringMap.put(entry.getKey(), entry.getValue().toString());
                });
        return stringMap;
    }
}

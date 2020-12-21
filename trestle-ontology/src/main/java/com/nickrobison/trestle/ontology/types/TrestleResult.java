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
 * <p>
 * Single row from a {@link TrestleResultSet}, contains an internal {@link Map} of variable/value pairs
 * An empty Optional for a given varName indicates that that variable is unbound
 */
public class TrestleResult {

    private final Map<String, @Nullable OWLObject> resultValues;

    public TrestleResult() {
        this.resultValues = new HashMap<>();
    }

    public TrestleResult(Map<String, @Nullable OWLObject> resultValues) {
        this.resultValues = resultValues;
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
                    return Optional.of((OWLLiteral) owlObject);
                }
                throw new ClassCastException(String.format("OWLObject for variable %s is not an OWLLiteral", varName));
            }
        }
        return Optional.empty();
    }

    /**
     * Safely unwrap {@link TrestleResult#getLiteral(String)}
     * Call this if the desired {@link OWLLiteral} should never be empty
     * throw {@link IllegalStateException} if {@link Optional#empty()}
     *
     * @param varName - {@link String} Variable name to access
     * @return - {@link OWLLiteral}
     */
    public OWLLiteral unwrapLiteral(String varName) {
        return getLiteral(varName).orElseThrow(() -> new IllegalStateException(String.format("Unable to get OWLLiteral for property %s", varName)));
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
            if (owlObject instanceof OWLIndividual) {
                return Optional.of((OWLIndividual) owlObject);
            }
            throw new ClassCastException(String.format("OWLObject for variable %s is not an OWLIndividual", varName));
        }
        return Optional.empty();
    }

    /**
     * Safely unwrap {@link TrestleResult#getIndividual(String)}
     * Call this if the desired {@link OWLIndividual} should never be empty
     * <p>
     * throw {@link IllegalStateException} if {@link Optional#empty()}
     *
     * @param varName - Variable name to access
     * @return - {@link OWLIndividual} for that variable
     */
    public OWLIndividual unwrapIndividual(String varName) {
        return getIndividual(varName).orElseThrow(() -> new IllegalStateException(String.format("Unable to get Individual for property %s", varName)));
    }

    @SuppressWarnings({"lambda.param.type.incompatible"})
    public Map<String, String> getResultValues() {
        Map<String, String> stringMap = new HashMap<>();
        this.resultValues
                .forEach((key, value) -> {
                    if (value != null) {
                        stringMap.put(key, value.toString());
                    }
                });
        return stringMap;
    }

    @Override
    public String toString() {
        return "TrestleResult{" +
                "resultValues=" + resultValues +
                '}';
    }
}

package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.annotations.Fact;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * Created by nickrobison on 3/5/18.
 */
public interface ITypeConverter {
    /**
     * Convert the {@link Class} into its corresponding primitive, if it is a primitive type
     * This is required to handle the auto-boxing of the Reflection APIs
     *
     * @param returnClass - {@link Class} to parse
     * @return - Primitive {@link Class}, or original class if the input is not primitive
     */
    @SuppressWarnings({"Duplicates", "squid:S1199"})
    static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
            switch (returnClass.getTypeName()) {
                case "int": {
                    return Integer.class;
                }
                case "float": {
                    return Float.class;
                }
                case "double": {
                    return Double.class;
                }
                case "boolean": {
                    return Boolean.class;
                }
                case "long": {
                    return Long.class;
                }
                default: {
                    throw new ClassCastException(String.format("Unsupported cast of %s to primitive type", returnClass.getTypeName()));
                }
            }
        }

        return returnClass;
    }

    /**
     * Register {@link TypeConstructor} class with the reasoner
     *
     * @param constructor - {@link TypeConstructor} to register
     */
    void registerTypeConstructor(TypeConstructor constructor);

    /**
     * Extracts a java object of type T from a given OWL Literal
     * Also handles the object/primitive conversion
     *
     * @param javaClass - Java class to cast literal into
     * @param literal   - OWLLiteral to extract
     * @param <T>       - Java type
     * @return Java type of type T
     */
    //    I need the unchecked casts in order to get the correct primitives for the constructor generation
    @SuppressWarnings({"unchecked", "return.type.incompatible", "squid:S1199"})
    <T extends @NonNull Object> T extractOWLLiteral(Class<T> javaClass, @Nullable OWLLiteral literal);

    /**
     * Lookup java type from OWL Datatype
     * If classToVerify is not null, check against the class in case the constructor requires a primitive
     *
     * @param dataProperty  - OWLDataPropertyAssertionAxiom to get java type from
     * @param classToVerify - @Nullable Class to cross-check with to ensure we're parsing the correct boxed/unboxed type.
     * @return - Java Class corresponding to OWL Datatype and required Class constructor argument
     */
    @SuppressWarnings({"dereference.of.nullable", "return.type.incompatible"})
    Class<?> lookupJavaClassFromOWLDatatype(OWLDataPropertyAssertionAxiom dataProperty, @Nullable Class<?> classToVerify);

    /**
     * Inspect Java class to determine correct datatype for a given OWLDataProperty
     *
     * @param classToVerify - Class to verify type against
     * @param property      - OWLDataProperty to lookup
     * @return - Java class of corresponding data property
     */
    Class<?> lookupJavaClassFromOWLDataProperty(Class<?> classToVerify, OWLDataProperty property);

    /**
     * Get {@link OWLDatatype} from the given {@link Fact} annotation.
     * Or, use the Return type from the Java member
     *
     * @param annotation  - {@link Fact} annotation to parse
     * @param returnType - {@link Class} return type of Java method
     * @return - {@link OWLDatatype} of {@link Fact}
     */
    OWLDatatype getDatatypeFromAnnotation(Fact annotation, Class<?> returnType);

    /**
     * Convert the given java {@link Class} to the corresponding {@link OWLDatatype}
     *
     * @param javaTypeClass - {@link Class} Java class to convert
     * @return - {@link OWLDatatype} that relates to the {@link Class}
     */
    OWLDatatype getDatatypeFromJavaClass(Class<?> javaTypeClass);
}

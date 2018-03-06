package com.nickrobison.trestle.reasoner.annotations;

import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nrobison on 6/27/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@TrestleDataProperty
public @interface Fact {

    /**
     * Specify the Fact IRI suffix.
     * If nothing is specified, the fact will default to the filtered name of the Java member
     *
     * @return - {@link String} IRI suffix
     */
    String name();

    /**
     * Override Java class member with this given return type
     * Note: This doesn't check if it's possible to convert between the Java type and the {@link OWL2Datatype}
     * If they're not compatible, the reasoner will throw a {@link ClassCastException}
     *
     * @return - {@link OWL2Datatype}
     */
    OWL2Datatype datatype() default OWL2Datatype.XSD_ANY_URI;
}

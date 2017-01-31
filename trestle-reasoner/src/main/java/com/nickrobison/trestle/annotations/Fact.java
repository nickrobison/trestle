package com.nickrobison.trestle.annotations;

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
public @interface Fact {

    String name();
//    FIXME(nrobison): This is not the way to handle defaults to ignore. I should probably migrate this to a string or something
    OWL2Datatype datatype() default OWL2Datatype.XSD_NMTOKEN;
}

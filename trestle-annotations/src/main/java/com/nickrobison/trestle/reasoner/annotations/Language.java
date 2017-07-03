package com.nickrobison.trestle.reasoner.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nrobison on 12/5/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Language {

    /**
     * Set the language for the multi-language support
     * Defined as an ISO shortcode
     * @return - Language string
     */
    String language();
}

package com.nickrobison.trestle.reasoner.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nrobison on 7/1/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@TrestleDataProperty
public @interface Spatial {
    String name() default "";
    int projection() default 0;
}

package com.nickrobison.trestle.reasoner.annotations;

import com.nickrobison.trestle.types.ObjectRestriction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nrobison on 6/27/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ObjectProperty {
    ObjectRestriction restriction();
    int cardinality() default 1;
}

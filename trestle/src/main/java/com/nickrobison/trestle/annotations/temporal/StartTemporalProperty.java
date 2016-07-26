package com.nickrobison.trestle.annotations.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nrobison on 7/20/16.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@TemporalProperty
public @interface StartTemporalProperty {
    TemporalType type() default TemporalType.INTERVAL;
    TemporalScope scope() default TemporalScope.VALID;
}

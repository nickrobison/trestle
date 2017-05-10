package com.nickrobison.trestle.reasoner.annotations.temporal;

import com.nickrobison.trestle.reasoner.types.TemporalScope;
import com.nickrobison.trestle.reasoner.types.TemporalType;

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
public @interface EndTemporal {
    String name() default "";
    TemporalType type() default TemporalType.INTERVAL;
    TemporalScope scope() default TemporalScope.EXISTS;
    String timeZone() default "";
}

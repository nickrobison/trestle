package com.nickrobison.trestle.annotations.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Created by nrobison on 7/20/16.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@TemporalProperty
public @interface StartTemporal {
    String name() default "";
    TemporalType type() default TemporalType.INTERVAL;
    TemporalScope scope() default TemporalScope.EXISTS;
    String timeZone() default "";
}

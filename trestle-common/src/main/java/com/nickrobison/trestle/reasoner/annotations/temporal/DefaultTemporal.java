package com.nickrobison.trestle.reasoner.annotations.temporal;

import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * Created by nrobison on 6/28/16.
 */
@TemporalProperty
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultTemporal {
    String name() default "";
    TemporalType type();
    TemporalScope scope() default TemporalScope.EXISTS;
    int duration();
    ChronoUnit unit();
    String timeZone() default "";
}

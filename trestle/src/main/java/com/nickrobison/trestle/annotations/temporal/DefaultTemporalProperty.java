package com.nickrobison.trestle.annotations.temporal;

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
public @interface DefaultTemporalProperty {
    TemporalType type();
    TemporalScope scope() default TemporalScope.VALID;
    int duration();
    ChronoUnit unit();
}

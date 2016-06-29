package com.nickrobison.trestle.annotations;

import com.nickrobison.trestle.types.TemporalType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * Created by nrobison on 6/28/16.
 */
// TODO(nrobison): This should support more than just fields
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Temporal {
    TemporalType type();
    int duration();
    ChronoUnit unit();
}

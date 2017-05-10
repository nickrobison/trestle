package com.nickrobison.trestle.reasoner.annotations.metrics;

import java.lang.annotation.*;

/**
 * Created by nrobison on 3/17/17.
 */
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Metriced {

    /**
     * Default registry name
     * @return - registry name
     */
    String registry() default "trestle-registry";
}

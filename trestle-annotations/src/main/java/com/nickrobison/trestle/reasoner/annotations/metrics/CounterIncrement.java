package com.nickrobison.trestle.reasoner.annotations.metrics;

import java.lang.annotation.*;

/**
 * Created by nrobison on 3/19/17.
 */

/**
 * An annotation for marking an incrementing method of a Counter metric
 * This annotation requires a name field, because it assumes there's a corresponding {@link CounterDecrement} annotation
 * There doesn't need to be one, but if there is, we want to make sure everything is in sync, so we require a manually specified name
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CounterIncrement {

    /**
     * Name of the counter to increment when called
     * @return - Counter name
     */
    String name();

    boolean absolute() default false;

    long amount() default 1;
}

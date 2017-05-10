package com.nickrobison.trestle.reasoner.annotations.metrics;

import java.lang.annotation.*;

/**
 * Created by nrobison on 3/19/17.
 */
/**
 * An annotation for marking a decrementing method of a Counter metric
 * This annotation requires a name field, because it assumes there's a corresponding {@link CounterIncrement} annotation
 * There doesn't need to be one, but if there is, we want to make sure everything is in sync, so we require a manually specified name
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CounterDecrement {
    String name();

    boolean absolute() default false;

    long amount() default 1;
}

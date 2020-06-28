package com.nickrobison.trestle.server.annotations;

import com.nickrobison.trestle.server.auth.Privilege;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by nrobison on 1/19/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface PrivilegesAllowed {
    Privilege[] value() default Privilege.USER;
}

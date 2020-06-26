package com.nickrobison.trestle.server.auth;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nrobison on 1/19/17.
 */
public enum Privilege {
    USER(1, "USER"),
    ADMIN(2, "ADMIN"),
    DBA(4, "DBA");

    private final int value;
    private final String name;
    Privilege(int val, String name) {
        this.value = val;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
      return name;
    }

    public static Set<Privilege> parsePrivileges(int val) {
        return Stream.of(values())
                .filter(priv -> (val & priv.getValue()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Privilege.class)));
    }

    public static int buildPrivilageMask(Set<Privilege> privileges) {
        int privMask = 0;
        for (Privilege priv : privileges) {
            privMask = privMask | priv.getValue();
        }
        return privMask;
    }
}

package com.nickrobison.trestle.server.auth;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nrobison on 1/19/17.
 */
public enum Privilege {
    USER(1),
    ADMIN(2),
    DBA(4);

    private int _val;
    Privilege(int val) {
        _val = val;
    }

    public int getValue() {
        return _val;
    }

    public static Set<Privilege> parsePrivileges(int val) {
        return Stream.of(values())
                .filter(priv -> (val & priv.getValue()) != 0)
                .collect(Collectors.toSet());
    }

    public static int buildPrivilageMask(Set<Privilege> privileges) {
        int privMask = 0;
        for (Privilege priv : privileges) {
            privMask = privMask | priv.getValue();
        }
        return privMask;
    }
}

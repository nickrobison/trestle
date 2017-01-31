package com.nickrobison.trestle.iri;

/**
 * Created by nrobison on 1/23/17.
 */

import com.nickrobison.trestle.iri.exceptions.IRIParseException;

import java.util.stream.Stream;

/**
 * Specifies the version of the IRIs being generated
 * V1: All date/times converted UNIX timestamps
 * V2: Date/times encoded using Temporenc (not implemented)
 * V3: Variable length encoding (not implemented)
 */
public enum IRIVersion {
    V1("V1"),
    V2("V2"),
    V3("V3");

    private final String name;
    IRIVersion(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static IRIVersion matchVersion(String version) {
        return Stream.of(IRIVersion.values())
                .filter(value -> value.getName().equals(version))
                .findAny().orElseThrow(() -> new IRIParseException(version));
    }
}

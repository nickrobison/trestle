package com.nickrobison.trestle.iri;

/**
 * Created by nrobison on 1/23/17.
 */

/**
 * Specifies the version of the IRIs being generated
 * V1: All date/times converted UNIX timestamps
 * V2: Date/times encoded using Temporenc (not implemented)
 * V3: Variable length encoding (not implemented)
 */
public enum IRIVersion {
    V1,
    V2,
    V3
}

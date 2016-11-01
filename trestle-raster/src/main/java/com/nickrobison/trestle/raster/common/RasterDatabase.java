package com.nickrobison.trestle.raster.common;

import java.awt.image.Raster;

/**
 * Created by nrobison on 9/25/16.
 */

/**
 * Enum defining supported raster databases
 */
public enum RasterDatabase {
    ORACLE ("Oracle"),
    POSTGRES ("Postgres");

    private final String name;

    private RasterDatabase(String name) {
     this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}

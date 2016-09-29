package com.nickrobison.trestle.raster.common;

/**
 * Created by nrobison on 9/25/16.
 */
public class RasterID {

    private final RasterDatabase database;
    private final String table;
    private final long id;

    public RasterID(RasterDatabase database, String table, long id) {
        this.database = database;
        this.table = table;
        this.id = id;
    }

    public RasterDatabase getDatabase() {
        return database;
    }

    public String getTable() {
        return table;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", database, table, id);
    }
}

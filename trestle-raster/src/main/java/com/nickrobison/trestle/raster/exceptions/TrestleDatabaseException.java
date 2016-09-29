package com.nickrobison.trestle.raster.exceptions;

/**
 * Created by nrobison on 9/23/16.
 */
public class TrestleDatabaseException extends TrestleRasterException {

    public TrestleDatabaseException(Exception e) {
        super(e.getMessage());
    }
}

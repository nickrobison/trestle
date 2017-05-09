package com.nickrobison.trestle.raster.exceptions;

/**
 * Created by nrobison on 9/23/16.
 */
@SuppressWarnings({"argument.type.incompatible"})
public class TrestleDatabaseException extends TrestleRasterException {

    public TrestleDatabaseException(Exception e) {
        super(e.getMessage());
    }
}

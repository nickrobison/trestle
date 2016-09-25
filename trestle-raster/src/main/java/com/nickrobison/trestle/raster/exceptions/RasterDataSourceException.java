package com.nickrobison.trestle.raster.exceptions;

import java.net.URI;

/**
 * Created by nrobison on 9/23/16.
 */
public class RasterDataSourceException extends TrestleRasterException {

    public enum Type {
        LOCATION,
        FORMAT,
        GEOREFERENCING
    }

    public RasterDataSourceException(Type errorType, URI file, Exception e) {
        super(parseErrorType(errorType, file, e));
    }

    private static String parseErrorType(Type errorType, URI file, Exception thrownException) {
        final String message;
        switch (errorType) {
            case LOCATION: {
                message = String.format("Unable to access file %s. Error: %s", file.toString(), thrownException.getMessage());
                break;
            }
            case FORMAT: {
                message = String.format("File %s is not a valid Raster format. Error: %s", file.toString(), thrownException.getMessage());
                break;
            }
            case GEOREFERENCING: {
                message = String.format("Could not get valid georeferencing information from file %s. Error: %s", file.toString(), thrownException.getMessage());
                break;
            }
            default: {
                message = String.format("Data source exception for file %s. Error: %s", file.toString(), thrownException.getMessage());
                break;
            }
        }
        return message;
    }
}

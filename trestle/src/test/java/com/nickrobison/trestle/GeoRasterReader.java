package com.nickrobison.trestle;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.junit.jupiter.api.Test;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

/**
 * Created by nrobison on 9/17/16.
 */
public class GeoRasterReader {


    private static final Logger logger = LoggerFactory.getLogger(GeoRasterReader.class);
    private GridCoverage2D grid;
    private Raster data;
    private GeoTiffReader geoTiffReader;
    private GridEnvelope dimensions;
    private GridCoordinates maxDimensions;
    private GridCoverage2D coverage;
    private GridGeometry2D geometry;

    @Test
    public void readGeoTiff() throws IOException {
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);

        //this will basically read 4 tiles worth of data at once from the disk...
        ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

        //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);

        final File tiffFile = new File("/Users/nrobison/Movies/afripop/MOZ15v4.tif");
        geoTiffReader = new GeoTiffReader(tiffFile);
        dimensions = geoTiffReader.getOriginalGridRange();
        maxDimensions = dimensions.getHigh();
        coverage = geoTiffReader.read(new GeneralParameterValue[]{policy, gridsize, useJaiRead});
        geometry = coverage.getGridGeometry();
        readRaster();
    }

    private void readRaster() {
//        final Rectangle bounds = data.getBounds();
        final int numXTiles = maxDimensions.getCoordinateValue(0) + 1;
        final int numYTiles = maxDimensions.getCoordinateValue(1) + 1;
        int xTile, yTile, minX, minY;
        for (xTile = 0; xTile < numXTiles; xTile++) {
            for (yTile = 0; yTile < numYTiles; yTile++) {
                final double[] pixel = new double[1];
                coverage.evaluate(new GridCoordinates2D(xTile, yTile), pixel);
                if (pixel[0] > 0) {
                    logger.info("Pixel {},{} has value {}", xTile, yTile, pixel);
                }

            }
        }

    }
}

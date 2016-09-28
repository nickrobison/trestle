package com.nickrobison.trestle.raster;

import com.nickrobison.trestle.raster.OracleRaster.OracleRasterManager;
import com.nickrobison.trestle.raster.common.RasterConverter;
import com.nickrobison.trestle.raster.common.RasterDatabase;
import com.nickrobison.trestle.raster.common.RasterID;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import oracle.spatial.georaster.geotiff.GeoTIFFWriter;
import oracle.spatial.georaster.geotiff.GeorGtfParser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.function.Consumer;

/**
 * Created by nrobison on 9/17/16.
 */
@SuppressWarnings("deprecation")
public class GeoRasterReader {


    private static final Logger logger = LoggerFactory.getLogger(GeoRasterReader.class);

    @Test
    public void readGeoTiff() throws Exception {
        final OracleRasterManager oracleRasterManager = new OracleRasterManager("jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial", "spatialUser", "spatial1");

        //        final File tiffFile = new File("/Users/nrobison/Movies/afripop/MOZ15v4.tif");
//        final File tiffFile = new File("/Users/nrobison/Movies/LE71670732016249SG100/merged.tif");
//        final File tiffFile = new File("/Users/nrobison/Movies/glccafg20_tif/afndvimar93g.tif");
//        final File tiffFile = new File("/Users/nrobison/Movies/gt30e020s10.tif");
        final File tiffFile = new File("/Users/nrobison/Movies/LE71670732016249SG100/LE71670732016249SG100_B1.TIF");
//        Consumer<Double> progressCallback = (progress -> logger.info("{}% complete", progress));
//        final RasterID rasterID = oracleRasterManager.writeRaster(tiffFile.toURI(), progressCallback);
        final RasterID rasterID = new RasterID(RasterDatabase.ORACLE, "TRESTLE_RDT_1", 42);

//        Read it back out
        final RenderedImage renderedImage = oracleRasterManager.readRaster(rasterID);
        ImageIO.write(renderedImage, "tif", new File("/Users/nrobison/Desktop/rastertest.tif"));

//        Try for an intersetion
        final Geometry read = new WKTReader().read("POLYGON ((31.08333950117128 -25.57202871446725, 31.08333950117128 -24.57695170392678, 33.8656270988277 -24.57695170392678, 33.8656270988277 -25.57202871446725, 31.08333950117128 -25.57202871446725))");
        final RenderedImage intersectedRaster = oracleRasterManager.getIntersectedRaster(read, rasterID);
        RasterConverter.convertImage(intersectedRaster, RasterConverter.ImageType.TIFF, new File("/Users/nrobison/Desktop/test-intersect.tiff"));
        oracleRasterManager.shutdown();
    }
}

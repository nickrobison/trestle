package com.nickrobison.trestle;

import oracle.spatial.geometry.JGeometry;
import oracle.spatial.georaster.GeoRasterException;
import oracle.spatial.georaster.JGeoRaster;
import oracle.spatial.georaster.JGeoRasterMeta;
import oracle.spatial.georaster.RasterInfo;
import oracle.sql.NUMBER;
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
import sun.awt.image.ByteInterleavedRaster;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * Created by nrobison on 9/17/16.
 */
@SuppressWarnings("deprecation")
public class GeoRasterReader {


    private static final Logger logger = LoggerFactory.getLogger(GeoRasterReader.class);
    private GridCoverage2D grid;
    private RenderedImage renderedImage;
    private GeoTiffReader geoTiffReader;
    private GridEnvelope dimensions;
    private GridCoordinates maxDimensions;
    private GridCoverage2D coverage;
    private GridGeometry2D geometry;
    private Connection connection;
    private JGeoRaster jGeor;
    private int numBands;

    @Test
    public void readGeoTiff() throws Exception {
        connection = DriverManager.getConnection("jdbc:oracle:thin:@//oracle7.hobbithole.local:1521/spatial", "spatialUser", "spatial1");
        connection.setAutoCommit(false);
//        connection.close();

//        Create the stuff
        final PreparedStatement delete = connection.prepareStatement("DELETE FROM TRESTLE_GEORASTER WHERE GEORID = 1");
        delete.execute();
        final PreparedStatement rdtDelete = connection.prepareStatement("DELETE FROM TRESTLE_RDT_1 WHERE RASTERID = 1");
        rdtDelete.execute();
        final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO trestle_georaster VALUES(1, 'TIFF', mdsys.sdo_geor.init('TRESTLE_RDT_1', 1))");
        preparedStatement.execute();
        jGeor = new JGeoRaster(connection, "trestle_rdt_1", new NUMBER(1));

        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);

        //this will basically read 4 tiles worth of renderedImage at once from the disk...
        ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

        //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);

//        final File tiffFile = new File("/Users/nrobison/Movies/afripop/MOZ15v4.tif");
        final File tiffFile = new File("/Users/nrobison/Movies/LE71670732016249SG100/merged.tif");
//        final File tiffFile = new File("/Users/nrobison/Movies/gt30e020s10.tif");
//        final File tiffFile = new File("/Users/nrobison/Movies/LE71670732016249SG100/LE71670732016249SG100_B1.TIF");
        geoTiffReader = new GeoTiffReader(tiffFile);
//        numBands = geoTiffReader.getGridCoverageCount();
        dimensions = geoTiffReader.getOriginalGridRange();
        maxDimensions = dimensions.getHigh();
        coverage = geoTiffReader.read(new GeneralParameterValue[]{gridsize, policy, useJaiRead});
//        coverage = geoTiffReader.read(null);
        numBands = coverage.getNumSampleDimensions();
        geometry = coverage.getGridGeometry();


        final String gridString = coverage.getGridGeometry().getGridToCRS().toWKT();
//        00, 01, 10, 11, 02, 12
        renderedImage = coverage.getRenderedImage();
        readRaster();
    }

    private void readRaster() throws Exception {

        final int numXTiles = maxDimensions.getCoordinateValue(0) + 1;
        final int numYTiles = maxDimensions.getCoordinateValue(1) + 1;
        final int totalTiles = numXTiles * numYTiles;
        int xTile, yTile;
        int minX = 0;
        int minY = 0;
        int tileWidth = renderedImage.getTileWidth();
        int tileHeight = renderedImage.getTileHeight();
//        Metadata
//        String dim = String.format("dimSize=(%d,%d,%d)", renderedImage.getHeight(), renderedImage.getWidth(), numBands);
//        jGeor.getMetadataObject().init(21001, "interleaving=BIP cellDepth=8BIT_U " + dim);
//        jGeor.storeToDB();

        for (xTile = 0; xTile < numXTiles; xTile++) {
            for (yTile = 0; yTile < numYTiles; yTile++) {
                final double[] pixel = new double[1];
                final Raster tile = renderedImage.getTile(xTile, yTile);

                if (tile != null) {
//                    Else, keep moving
                    minX = tile.getMinX();
                    minY = tile.getMinY();
//                    if (tileWidth != tile.getWidth()) {
//                        logger.debug("Widths have changed");
//                    }
                    tileWidth = tile.getWidth();
                    tileHeight = tile.getHeight();
//                    logger.debug("Getting {},{}", xTile, yTile);
                    final JGeometry jGeometry = new JGeometry(minX, minY, minX + tileWidth - 1, minY + tileHeight - 1, 0);
                    final Blob blob = jGeor.getRasterObject().initRasterBlockJS(0, 0, yTile, xTile, jGeometry);
                    jGeor.getRasterObject().storeRasterBlock(extractRasterData(tile), blob);
//                    Store the image
//                    Get buffered image
//
//                    final short[] data = ((DataBufferShort) tile.getDataBuffer()).getData(0);
//                    ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES * data.length);
//
//                    for (short value : data) {
//                        buffer.putShort(value);
//                    }
//
//                    final BufferedImage bufferedImage = new BufferedImage(renderedImage.getColorModel(), tile.createCompatibleWritableRaster(), renderedImage.getColorModel().isAlphaPremultiplied(), null);
//                    final ImageInputStream imageInputStream = ImageIO.createImageInputStream(bufferedImage);
//                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                    ImageIO.write(bufferedImage, "tif", bos);
//                    jGeor.getRasterObject().storeRasterBlock(buffer.array(), blob);
//                    jGeor.getRasterObject().storeRasterBlock(tile.getDataBuffer(), blob);
//                    writeRasterData(blob, tile, renderedImage.getColorModel());
//                    logger.debug("Wrote {}, {}", xTile, yTile);

//                coverage.evaluate(new GridCoordinates2D(xTile, yTile), pixel);
                    if (pixel[0] > 0) {
                        logger.info("Pixel {},{} has value {}", xTile, yTile, pixel);
                    }
                }
            }
            logger.debug("{}/{}", xTile, numXTiles);
        }

        initMetadata(renderedImage, jGeor);
        jGeor.storeToDB();
        connection.commit();

//        Try to read it out
        if (!jGeor.getRasterObject().validateBlockMBR()) {
            logger.error("Problem with MBR");
        }
        final RenderedImage rasterImage = jGeor.getGeoRasterImageObject().getRasterImage(0);
        ImageIO.write(rasterImage, "tif", new File("/Users/nrobison/Desktop/test2.tif"));

        connection.close();
    }

    private static byte[] extractRasterData(Raster tile) {
        final int numBands = tile.getNumBands();
        final int dataType = tile.getDataBuffer().getDataType();
        final int bankSize = tile.getDataBuffer().getSize();
        byte[] bank = new byte[bankSize];
        ByteBuffer outputBuffer;
        if (tile instanceof ByteInterleavedRaster) {
            outputBuffer = ByteBuffer.allocate(bankSize);
        } else {
            outputBuffer = ByteBuffer.allocate(dataType * bankSize);
        }


//        If the bytes are interleaved, we need to read the whole thing, and then parse out the individual layers

        for (int i = 0; i < numBands; i++) {
            if (dataType == Float.BYTES) {
                final float[] data = ((DataBufferFloat) tile.getDataBuffer()).getData(i);
                ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES * data.length);

                for (float value : data) {
                    buffer.putFloat(value);
                }

                bank = buffer.array();
            } else if (dataType == Short.BYTES) {
                final short[] data = ((DataBufferShort) tile.getDataBuffer()).getData(i);
                ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES * data.length);

                for (short value : data) {
                    buffer.putShort(value);
                }
                bank = buffer.array();
            } else {
//                If we can just pull out the bytes themselves, we just need to make sure we aren't reading the non-existent band info
                final DataBufferByte dataBuffer = (DataBufferByte) tile.getDataBuffer();
                if (i < dataBuffer.getNumBanks()) {
                    bank = dataBuffer.getData(i);
                } else {
                    continue;
                }
            }
            outputBuffer.put(bank);
        }

        return outputBuffer.array();
    }

//    private void writeRasterData(BLOB rasterBlockBlob, Raster tile, ColorModel colorModel) throws Exception {
//        OutputStream outStream = rasterBlockBlob.setBinaryStream(1L);
//        int bufferSize = rasterBlockBlob.getBufferSize();
//        byte[] outBuffer = new byte[bufferSize];
//        int srcPos = 0;
//        int padSize = 0;
//
//        int dataType, numBanks, bankSize;
//        int[] offsets;
//        int i = 0;
//        DataBuffer rasterData = tile.getDataBuffer();
////        final BufferedImage bufferedImage = new BufferedImage(tile.getWidth(), tile.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
////        bufferedImage.getGraphics().drawImage(tile., 0, 0, null);
////        final BufferedImage bufferedImage = new BufferedImage(colorModel, (WritableRaster) tile, colorModel.isAlphaPremultiplied(), null);
//        dataType = rasterData.getDataType();
//        numBanks = rasterData.getNumBanks();
//        bankSize = rasterData.getSize();
//        offsets = rasterData.getOffsets();
//        byte[] bank = new byte[bankSize];
//
//        for (i = 0; i < numBanks; i++) {
//            srcPos = srcPos + offsets[i];
//            if (dataType == Float.BYTES) {
//                final float[] data = ((DataBufferFloat) tile.getDataBuffer()).getData(i);
//                ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES * data.length);
//
//                for (float value : data) {
//                    buffer.putFloat(value);
//                }
//
//                bank = buffer.array();
//            } else if (dataType == Short.BYTES) {
//                final short[] data = ((DataBufferShort) tile.getDataBuffer()).getData(i);
//                ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES * data.length);
//
//                for (short value : data) {
//                    buffer.putShort(value);
//                }
//                bank = buffer.array();
//            } else {
//                bank = ((DataBufferByte) tile.getDataBuffer()).getData(i);
//            }
//
//            if (padSize > 0) {
//                System.arraycopy(bank, srcPos, outBuffer, bufferSize - padSize, padSize);
//                outStream.write(outBuffer);
//                srcPos = srcPos + padSize;
//            }
//
//            while (srcPos <= bankSize - bufferSize) {
//                System.arraycopy(bank, srcPos, outBuffer, 0, bufferSize);
//                outStream.write(outBuffer);
//                srcPos = srcPos + bufferSize;
//            }
//            if (srcPos == bankSize)
//                srcPos = 0; // start another bank
//            else // what left must be less than one chunk
//            {
//                System.arraycopy(bank, srcPos, outBuffer, 0, bankSize - srcPos);
//                if (i == numBanks - 1) //the last bank
//                {
//                    outStream.write(outBuffer, 0, bankSize - srcPos);
//                } else {
//                    //The size of data which needs to be padded
//                    //to the outBuffer from the next bank if
//                    //there is at least one more bank
//                    padSize = bufferSize - (bankSize - srcPos);
//                    srcPos = 0;
//                }
//            }
//        }
//        outStream.close();
//    }

    private void initMetadata(RenderedImage image, JGeoRaster geor) throws GeoRasterException {

        final JGeoRasterMeta meta = geor.getMetadataObject();

        final SampleModel sampleModel = image.getSampleModel();
        final ColorModel colorModel = image.getColorModel();

        final int column = image.getWidth();
        final int row = image.getHeight();
        final int dataType = sampleModel.getDataType();
        String rasterSpec = String.format("dimSize=(%d,%d,%d) ", row, column, numBands);
        final String sm_class = sampleModel.getClass().getName();
        String rasterInfo_cellDepth_text = "";

        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                rasterInfo_cellDepth_text = "8BIT_U";
                break;
            case DataBuffer.TYPE_USHORT:
                rasterInfo_cellDepth_text = "16BIT_U";
                break;
            case DataBuffer.TYPE_INT:
                rasterInfo_cellDepth_text = "32BIT_U";
                break;
            case DataBuffer.TYPE_SHORT:
                rasterInfo_cellDepth_text = "16BIT_S";
                break;
            case DataBuffer.TYPE_FLOAT:
                rasterInfo_cellDepth_text = "32BIT_REAL";
                break;
            case DataBuffer.TYPE_DOUBLE:
                rasterInfo_cellDepth_text = "64BIT_REAL";
                break;
        }

        rasterSpec = rasterSpec + "cellDepth=" + rasterInfo_cellDepth_text + " ";
        rasterSpec = rasterSpec + "ultCoord=(" + image.getMinY() + "," + image.getMinX() + ",0) ";
        rasterSpec = rasterSpec + "compression=NONE ";

        final int numXTiles = image.getNumXTiles();
        final int numYTiles = image.getNumYTiles();

        int rasterInfo_blocking_totalRowBlocks = numYTiles;
        int rasterInfo_blocking_totalColumnBlocks = numXTiles;
        int rasterInfo_blocking_totalBandBlocks = 1;
        final int rowBlockSize = image.getTileHeight();
        final int columnBlockSize = image.getTileWidth();
        final int bandBlockSize = numBands;
        if (rasterInfo_blocking_totalRowBlocks == 1 && rasterInfo_blocking_totalColumnBlocks == 1) {
            rasterSpec = rasterSpec + "blocking=FALSE ";
        } else {
            rasterSpec = rasterSpec + String.format("blocking=True blocksize=(%d,%d,%d) ", rowBlockSize, columnBlockSize, bandBlockSize);
        }

        if (sm_class.startsWith("java.awt.image.PixelInterleavedSampleModel")) {
            rasterSpec = rasterSpec + "interleaving=BIP ";
        } else if (sm_class.startsWith("java.awt.image.BandedSampleModel")) {
            rasterSpec = rasterSpec + "interleaving=BSQ ";
        }
        rasterSpec = rasterSpec + "rLevel=0 ";
        meta.init(21001, rasterSpec);
        meta.getRasterInfo().setPyramidType(RasterInfo.PYRAMID_NONE);

        if (numBands >= 3) {

            if (sampleModel == null) {
                meta.getObjectInfo().setDefaultRed(1L);
                meta.getObjectInfo().setDefaultGreen(1L);
                meta.getObjectInfo().setDefaultBlue(1L);
            } else {
                int[] offsets = ((ComponentSampleModel) sampleModel).getBandOffsets();
                meta.getObjectInfo().setDefaultRed((long) (offsets[0] + 1));
                meta.getObjectInfo().setDefaultGreen((long) (offsets[1] + 1));
                meta.getObjectInfo().setDefaultBlue((long) (offsets[2] + 1));
            }
        }
    }

    private void loadWorldFile(String srs, JGeoRaster jGeor) {
//        jGeor.getMetadataObject().getSpatialReferenceInfo().setWorldFile();
    }
}

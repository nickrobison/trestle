package com.nickrobison.trestle.raster.OracleRaster;

import com.nickrobison.trestle.raster.ITrestleRasterManager;
import com.nickrobison.trestle.raster.exceptions.RasterDataSourceException;
import com.nickrobison.trestle.raster.exceptions.TrestleDatabaseException;
import com.nickrobison.trestle.raster.exceptions.TrestleRasterException;
import oracle.jdbc.OracleTypes;
import oracle.spatial.geometry.JGeometry;
import oracle.spatial.georaster.*;
import oracle.spatial.georaster.sql.SdoGeoRaster;
import oracle.sql.NUMBER;
import oracle.sql.STRUCT;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GroundControlPoints;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.imageio.geotiff.TiePoint;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.awt.image.ByteInterleavedRaster;

import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.nickrobison.trestle.raster.exceptions.RasterDataSourceException.Type.LOCATION;

/**
 * Created by nrobison on 9/23/16.
 */
@SuppressWarnings("deprecation")
public class OracleRasterManager implements ITrestleRasterManager {

    private static final Logger logger = LoggerFactory.getLogger(OracleRasterManager.class);
    private final Connection connection;
    private final ParameterValue<Boolean> useJaiRead;
    private final ParameterValue<String> gridsize;
    private final ParameterValue<OverviewPolicy> policy;


    public OracleRasterManager(String oracleConnectionString, String username, String password) throws SQLException {

        connection = DriverManager.getConnection(oracleConnectionString, username, password);
        connection.setAutoCommit(false);

//        Setup geotiff reader properties
        policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);

        //this will basically read 4 tiles worth of renderedImage at once from the disk...
        gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();

        //Setting read type: use JAI ImageRead (true) or ImageReaders read methods (false)
        useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);
    }


    public long writeRaster(URI fileURI, @Nullable Consumer<Double> progressCallback) throws TrestleRasterException {

//       Setup the GeotiffReader
        final GeoTiffReader geoTiffReader;
        final GridEnvelope gridDimensions;
        final GridCoordinates maxDimensions;
        final GridCoverage2D coverage;
        final String gridString;
        final RenderedImage renderedImage;
        final int numBands;
        try {
            geoTiffReader = new GeoTiffReader(new File((fileURI)));
            gridDimensions = geoTiffReader.getOriginalGridRange();
             maxDimensions = gridDimensions.getHigh();
             coverage = geoTiffReader.read(new GeneralParameterValue[]{gridsize, policy, useJaiRead});
             gridString = coverage.getGridGeometry().getGridToCRS().toWKT();
            renderedImage = coverage.getRenderedImage();
             numBands = coverage.getNumSampleDimensions();
        } catch (IOException e) {
            throw new RasterDataSourceException(LOCATION, fileURI, e);
        }

        final int numXTiles = maxDimensions.getCoordinateValue(0) + 1;
        final int numYTiles = maxDimensions.getCoordinateValue(1) + 1;
        final int totalTiles = numXTiles * numYTiles;

        //        Add the new raster into the database table
//        First, create an empty raster in the rdt
//        final NUMBER RasterID;
        final long rasterID;
        final SdoGeoRaster sdoGeoRaster;
        final JGeoRaster jGeor;
        try {
            final CallableStatement createRasterStatement = connection.prepareCall("DECLARE gr SDO_GEORASTER;\n" +
                    "BEGIN\n" +
                    "gr := sdo_geor.init('trestle_rdt_1');\n" +
                    "insert into TRESTLE_GEORASTER (GEORID, TYPE, GEORASTER) values(gr.rasterid, 'TIFF', gr) RETURNING GEORASTER into ?;\n" +
                    "END;");
            createRasterStatement.registerOutParameter(1, OracleTypes.STRUCT, "MDSYS.SDO_GEORASTER");
            final ResultSet resultSet = createRasterStatement.executeQuery();
//            resultSet.next();
            final STRUCT rasterStruct = (STRUCT) createRasterStatement.getObject(1);
            sdoGeoRaster = new SdoGeoRaster(rasterStruct);
            rasterID = sdoGeoRaster.getRasterID().longValue();
            jGeor = new JGeoRaster(connection, sdoGeoRaster.getRasterDataTable(), sdoGeoRaster.getRasterID());
        } catch (Exception e) {
            throw new TrestleDatabaseException(e);
        }

//        Stream the tiles
        IntStream.range(0, numXTiles)
                .forEach(xTile -> {
                    IntStream.range(0, numYTiles)
                            .forEach(yTile -> writeRasterTile(renderedImage, xTile, yTile, jGeor));
                    if (progressCallback != null) {
                        progressCallback.accept(((double) xTile/(double) numXTiles) * 100.0);
                    }
                });

        //        Update the raster metadata and store to db
        try {
            initMetadata(renderedImage, jGeor, numBands);
            setSpatialData(jGeor, coverage, geoTiffReader.getGroundControlPoints());
            jGeor.storeToDB();
        } catch (Exception e) {
            throw new TrestleDatabaseException(e);
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new TrestleDatabaseException(e);
        }

        return rasterID;
    }

    public RenderedImage getIntersectedRaster() {
        return null;
    }

    public RenderedImage readRaster(long rasterID) throws TrestleDatabaseException {
        final JGeoRaster raster;
        try {
            raster = new JGeoRaster(connection, "TRESTLE_RDT_1", new NUMBER(rasterID));
        } catch (GeoRasterException e) {
            throw new TrestleDatabaseException(e);
        }

        try {
            return raster.getGeoRasterImageObject().getRasterImage(0);
        } catch (Exception e) {
            throw new TrestleDatabaseException(e);
        }
    }

    private static void writeRasterTile(RenderedImage image, int xTile, int yTile, JGeoRaster jGeor) {
        final Raster tile = image.getTile(xTile, yTile);
        if (tile != null) {
            final int minX = tile.getMinX();
            final int minY = tile.getMinY();
            final int tileWidth = tile.getWidth();
            final int tileHeight = tile.getHeight();
            final JGeometry jGeometry = new JGeometry(minX, minY, minX + tileWidth - 1, minY + tileHeight - 1, 0);
//            Try to store it
            try {
                final Blob blob = jGeor.getRasterObject().initRasterBlockJS(0, 0, yTile, xTile, jGeometry);
                jGeor.getRasterObject().storeRasterBlock(extractRasterData(tile), blob);
            } catch (Exception e) {
                logger.error("Cannot store object", e);
            }
        }
    }



    public void shutdown() {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("Could not close down connection", e);
        }
    }

    private static final byte[] extractRasterData(Raster tile) {
        final int numBands = tile.getNumBands();
        final int dataType = tile.getDataBuffer().getDataType();
        final int bankSize = tile.getDataBuffer().getSize();
        byte[] bank = new byte[bankSize];
        ByteBuffer outputBuffer;
        if (tile instanceof ByteInterleavedRaster) {
//            If the bytes are interleaved, we can just read one bank and move on
            outputBuffer = ByteBuffer.allocate(bankSize);
            final DataBufferByte dataBuffer = (DataBufferByte) tile.getDataBuffer();
            outputBuffer.put(dataBuffer.getData());
            return outputBuffer.array();
        } else {
            outputBuffer = ByteBuffer.allocate(dataType * bankSize);
        }

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

    private static void setSpatialData(JGeoRaster jGeor, GridCoverage coverage, @Nullable GroundControlPoints gcps) throws GeoRasterException {
        jGeor.getMetadataObject().initSpatialReferenceInfo();
        final SpatialReferenceInfo spatialReferenceInfo = jGeor.getMetadataObject().getSpatialReferenceInfo();
        final Optional<ReferenceIdentifier> first = coverage.getCoordinateReferenceSystem().getIdentifiers().stream().findFirst();
        if (first.isPresent()) {
            spatialReferenceInfo.setModelSRID(Integer.parseInt(first.get().getCode()));
        }

//        GCPs?
        if (gcps != null) {
            spatialReferenceInfo.setGcpPoints(new Vector<>(gcps.getTiePoints()));
            spatialReferenceInfo.setModelType(SpatialReferenceInfo.MDGRX_SRM_STOREDFUNC);
        } else {
            spatialReferenceInfo.setModelType(SpatialReferenceInfo.MDGRX_SRM_FUNCFITTING);
            spatialReferenceInfo.setReferenced(true);
        }
        spatialReferenceInfo.setWorldFile(30.0, 0.0, 0.0, -30.0, 572100.0, -1971900.0);
    }

    private static void initMetadata(RenderedImage image, JGeoRaster geor, int numBands) throws GeoRasterException {

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
}

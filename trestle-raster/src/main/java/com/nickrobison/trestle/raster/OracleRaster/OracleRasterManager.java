package com.nickrobison.trestle.raster.OracleRaster;

import com.nickrobison.trestle.common.exceptions.TrestleMissingIndividualException;
import com.nickrobison.trestle.common.exceptions.UnsupportedFeatureException;
import com.nickrobison.trestle.raster.ITrestleRasterManager;
import com.nickrobison.trestle.raster.common.RasterDatabase;
import com.nickrobison.trestle.raster.common.RasterID;
import com.nickrobison.trestle.raster.common.RasterUtils;
import com.nickrobison.trestle.raster.exceptions.RasterDataSourceException;
import com.nickrobison.trestle.raster.exceptions.TrestleDatabaseException;
import com.nickrobison.trestle.raster.exceptions.TrestleRasterException;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import oracle.jdbc.OracleTypes;
import oracle.spatial.geometry.JGeometry;
import oracle.spatial.georaster.*;
import oracle.spatial.georaster.sql.SdoGeoRaster;
import oracle.sql.NUMBER;
import oracle.sql.STRUCT;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.ReferenceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.nickrobison.trestle.raster.exceptions.RasterDataSourceException.Type.LOCATION;

/**
 * Created by nrobison on 9/23/16.
 */
@SuppressWarnings("deprecation")
public class OracleRasterManager implements ITrestleRasterManager {

    private static final Logger logger = LoggerFactory.getLogger(OracleRasterManager.class);
    private static final RasterDatabase RASTER_DATABASE = RasterDatabase.ORACLE;
    private static final String RASTER_DATA_TABLE = "TRESTLE_RDT_1";
    private static final int PYRAMIDLEVEL = 0;
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


    public RasterID writeRaster(URI fileURI, @Nullable Consumer<Double> progressCallback) throws TrestleRasterException {

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
        try (final CallableStatement createRasterStatement = connection.prepareCall("DECLARE gr SDO_GEORASTER;\n" +
                "BEGIN\n" +
                "gr := sdo_geor.init('trestle_rdt_1');\n" +
                "INSERT INTO TRESTLE_GEORASTER (GEORID, TYPE, GEORASTER) VALUES(gr.rasterid, 'TIFF', gr) RETURNING GEORASTER INTO ?;\n" +
                "END;")){
            createRasterStatement.registerOutParameter(1, OracleTypes.STRUCT, "MDSYS.SDO_GEORASTER");
            try (final ResultSet resultSet = createRasterStatement.executeQuery()) {
                //            resultSet.next();
                final STRUCT rasterStruct = (STRUCT) createRasterStatement.getObject(1);
                sdoGeoRaster = new SdoGeoRaster(rasterStruct);
                rasterID = sdoGeoRaster.getRasterID().longValue();
                jGeor = new JGeoRaster(connection, sdoGeoRaster.getRasterDataTable(), sdoGeoRaster.getRasterID());
            }
        } catch (Exception e) {
            throw new TrestleDatabaseException(e);
        }

//        Stream the tiles
        IntStream.range(0, numXTiles)
                .forEach(xTile -> {
                    IntStream.range(0, numYTiles)
                            .forEach(yTile -> writeRasterTile(renderedImage, xTile, yTile, jGeor));
                    if (progressCallback != null) {
                        progressCallback.accept(((double) xTile / (double) numXTiles) * 100.0);
                    }
                });

        //        Update the raster metadata and store to db
        try {
            initMetadata(renderedImage, jGeor, numBands);
            setSpatialData(jGeor, coverage);
            jGeor.storeToDB();
        } catch (Exception e) {
            throw new TrestleDatabaseException(e);
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new TrestleDatabaseException(e);
        }

        return new RasterID(RasterDatabase.ORACLE, RASTER_DATA_TABLE, rasterID);
    }

    /**
     * Retrieve the subset of a raster that intersects with the given geometry
     * Complex geometries are simplified to a bounding box
     *
     * @param geom     - Geometry to intersect with raster
     * @param rasterID - RasterID to retrieve
     * @return - RenderedImage of Raster subsection
     * @throws UnsupportedFeatureException
     * @throws TrestleMissingIndividualException
     * @throws TrestleRasterException
     */
    public RenderedImage getIntersectedRaster(Geometry geom, RasterID rasterID) throws UnsupportedFeatureException, TrestleMissingIndividualException, TrestleRasterException {

        if (rasterID.getDatabase() != RASTER_DATABASE) {
            logger.error("Cannot access raster from {} database on {} database", rasterID.getDatabase(), RASTER_DATABASE);
            throw new TrestleMissingIndividualException(rasterID.toString());
        }

        final JGeometry jGeometry;
        if (geom instanceof Point) {
            Point point = (Point) geom;
            jGeometry = new JGeometry(point.getX(), point.getY(), 4326);
        } else if (geom.isRectangle()) {
            final Envelope envelopeInternal = geom.getEnvelopeInternal();
            jGeometry = new JGeometry(envelopeInternal.getMinX(), envelopeInternal.getMinY(), envelopeInternal.getMaxX(), envelopeInternal.getMaxY(), 4326);
        } else {
            logger.warn("Simplifying given geometry to bounding box");
            final Envelope envelopeInternal = geom.getEnvelopeInternal();
            jGeometry = new JGeometry(envelopeInternal.getMinX(), envelopeInternal.getMinY(), envelopeInternal.getMaxX(), envelopeInternal.getMaxY(), 4326);
        }

        final JGeoRaster raster;
        try {
            raster = new JGeoRaster(connection, rasterID.getTable(), new NUMBER(rasterID.getId()));
        } catch (GeoRasterException e) {
            throw new TrestleMissingIndividualException(rasterID.toString());
        }

        long[] outWindow = new long[4];

        try {
            return raster.getGeoRasterImageObject().getRasterImage(PYRAMIDLEVEL, jGeometry, outWindow);
        } catch (Exception e) {
            throw new TrestleDatabaseException(e);
        }
    }

    /**
     * Return the entire raster as an AWT RenderedImage
     *
     * @param rasterID - RasterID to retrieve
     * @return - RenderedImage of entire raster
     * @throws TrestleDatabaseException
     * @throws TrestleMissingIndividualException
     */
    public RenderedImage readRaster(RasterID rasterID) throws TrestleDatabaseException, TrestleMissingIndividualException {
        final JGeoRaster raster;
        if (rasterID.getDatabase() != RASTER_DATABASE) {
            logger.error("Cannot access raster from {} database on {} database", rasterID.getDatabase(), RASTER_DATABASE);
            throw new TrestleMissingIndividualException(rasterID.toString());
        }
//
//
//        try {
//            final CallableStatement callableStatement = connection.prepareCall("DECLARE gr SDO_GEORASTER; blob1 BLOB;" +
//                    "BEGIN SELECT GEORASTER INTO gr FROM TRESTLE_GEORASTER WHERE GEORID=?; " +
//                    "SDO_GEOR.exportTo(gr, '', 'GeoTIFF', ?); END;");
//
//            callableStatement.setLong(1, rasterID.getId());
//            callableStatement.registerOutParameter(2, OracleTypes.BLOB);
//            final ResultSet resultSet = callableStatement.executeQuery();
//            final Blob blob = resultSet.getBlob(1);
//            return ImageIO.read(blob.getBinaryStream());
//
//        } catch (Exception e) {
//            throw new TrestleDatabaseException(e);
//        }

        try {
            raster = new JGeoRaster(connection, rasterID.getTable(), new NUMBER(rasterID.getId()));
        } catch (GeoRasterException e) {
            logger.error("Problem reading individual {} from database", rasterID, e);
            throw new TrestleMissingIndividualException(rasterID.toString());
        }

        try {
            return raster.getGeoRasterImageObject().getRasterImage(0, RasterInfo.COMPRESSION_DEFLATE, 80);
        } catch (Exception e) {
            throw new TrestleDatabaseException(e);
        }
    }

    /**
     * Shutdown the raster manager and dispose of the connection
     */
    public void shutdown() {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("Could not close down connection", e);
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
                final Blob blob = jGeor.getRasterObject().initRasterBlockJS(PYRAMIDLEVEL, 0, yTile, xTile, jGeometry);
                jGeor.getRasterObject().storeRasterBlock(RasterUtils.extractRasterData(tile), blob);
            } catch (Exception e) {
                logger.error("Cannot store object", e);
            }
        }
    }

    private static void setSpatialData(JGeoRaster jGeor, GridCoverage coverage) throws GeoRasterException {
        jGeor.getMetadataObject().initSpatialReferenceInfo();
        final SpatialReferenceInfo spatialReferenceInfo = jGeor.getMetadataObject().getSpatialReferenceInfo();
        final Optional<ReferenceIdentifier> first = coverage.getCoordinateReferenceSystem().getIdentifiers().stream().findFirst();
        if (first.isPresent()) {
            spatialReferenceInfo.setModelSRID(Integer.valueOf(first.get().getCode()));
        }

        spatialReferenceInfo.setModelType(SpatialReferenceInfo.MDGRX_SRM_FUNCFITTING);
        spatialReferenceInfo.setReferenced(Boolean.TRUE);
        spatialReferenceInfo.setWorldFile(30.0, 0.0, 0.0, -30.0, 572100.0, -1971900.0);
    }

    private static void initMetadata(RenderedImage image, JGeoRaster geor, int numBands) throws GeoRasterException {

        final JGeoRasterMeta meta = geor.getMetadataObject();

        final @Nullable SampleModel sampleModel = image.getSampleModel();
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

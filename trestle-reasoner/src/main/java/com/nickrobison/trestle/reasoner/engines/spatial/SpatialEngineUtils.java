package com.nickrobison.trestle.reasoner.engines.spatial;

import com.codahale.metrics.annotation.Metered;
import com.esri.core.geometry.OperatorExportToWkb;
import com.esri.core.geometry.Polygon;
import com.nickrobison.trestle.common.exceptions.TrestleInvalidDataException;
import com.nickrobison.trestle.reasoner.parser.SpatialParser;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.measure.unit.SI.METER;

public class SpatialEngineUtils {

    private static final OperatorExportToWkb operatorExport = OperatorExportToWkb.local();
    private static final Logger logger = LoggerFactory.getLogger(SpatialEngineUtils.class);

    private SpatialEngineUtils() {
//        UNUSED
    }

    /**
     * Re-project input {@link Geometry} into the given SRID
     * Adding to the provided {@link Cache}
     *
     * @param inputObject   - {@link Object} to reproject
     * @param inputSRID     - {@link Integer} SRID of input geometry
     * @param outputSRID    - {@link Integer} SRID to project into
     * @param geometryCache - {@link Cache} cache to add value to
     * @return - {@link Geometry} reprojected
     */
    public static Geometry reprojectObject(Object inputObject, int inputSRID, int outputSRID, Cache<Integer, Geometry> geometryCache) {

        final int hashCode = inputObject.hashCode();

//        Check to see if we already have a reprojected geometry, or an original one
//        Reprojected first
        final Geometry cachedProjectedGeom = geometryCache.get(hashCode + outputSRID);
        if (cachedProjectedGeom != null) {
            return cachedProjectedGeom;
        }

//        Original next
        final Geometry originalGeom;
        if (geometryCache.containsKey(hashCode + inputSRID)) {
            originalGeom = geometryCache.get(hashCode + inputSRID);
        } else {
            originalGeom = SpatialEngineUtils.buildObjectGeometry(inputObject, inputSRID);
//            Add to cache
            geometryCache.put(hashCode + inputSRID, originalGeom);
        }
        final Geometry projectedGeom = reprojectGeometry(originalGeom, inputSRID, outputSRID, null, null);
//        Add it to the cache, and return the value
        geometryCache.put(hashCode + outputSRID, projectedGeom);
        return projectedGeom;
    }

    /**
     * Reproject input {@link Geometry} into the given SRID
     * Optionally, adding to the provided {@link Cache}
     *
     * @param inputGeom  - {@link Geometry} to reproject
     * @param inputSRID  - {@link Integer} SRID of input geometry
     * @param outputSRID - {@link Integer} SRID to project into
     * @return - {@link Geometry} reprojected
     */
    public static Geometry reprojectGeometry(Geometry inputGeom, int inputSRID, int outputSRID) {
        return reprojectGeometry(inputGeom, inputSRID, outputSRID, null, null);
    }

    /**
     * Reproject input {@link Geometry} into the given SRID
     * Optionally, adding to the provided {@link Cache}
     *
     * @param inputGeom      - {@link Geometry} to reproject
     * @param inputSRID      - {@link Integer} SRID of input geometry
     * @param outputSRID     - {@link Integer} SRID to project into
     * @param geometryCache  - {@link Cache} optional cache to add value to
     * @param objectHashCode - {@link Integer} object hash code to use as key
     * @return - {@link Geometry} reprojected
     */
    public static Geometry reprojectGeometry(Geometry inputGeom, int inputSRID, int outputSRID, @Nullable Cache<Integer, Geometry> geometryCache, @Nullable Integer objectHashCode) {

//        If we're the same, don't reproject
        if (inputSRID == outputSRID) {
            return inputGeom;
        }

//        See if we already have a reprojected geom
        if (geometryCache != null && objectHashCode != null) {
            final Geometry geometry = geometryCache.get(objectHashCode + outputSRID);
            if (geometry != null) {
                return geometry;
            }
        }
        final Geometry transformedGeom;
        try {
//          Make sure we force longitude first mode
            final CoordinateReferenceSystem inputCRS = CRS.decode("EPSG:" + inputSRID, true);
            final CoordinateReferenceSystem outputCRS = CRS.decode("EPSG:" + outputSRID, true);

            final MathTransform mathTransform = CRS.findMathTransform(inputCRS, outputCRS, true);
            transformedGeom = JTS.transform(inputGeom, mathTransform);
        } catch (FactoryException e) {
            final String projectionError = String.format("Cannot find transformation from %s to %s", inputSRID, outputSRID);
            logger.error(projectionError, e);
            throw new IllegalStateException(projectionError);
        } catch (TransformException e) {
            final String transformError = String.format("Cannot re-project %s into %s", inputSRID, outputSRID);
            logger.error(transformError, e);
            throw new IllegalStateException(transformError);
        }

        //        Add the transformed Polygon to the cache
        if (geometryCache != null && objectHashCode != null) {
            geometryCache.put(objectHashCode + outputSRID, transformedGeom);
        }
        return transformedGeom;
    }

    /**
     * Get the object {@link Geometry}, in the specified projection from the provided cache, computing if absent
     * Object keys are built by calling {@link Object#hashCode()} and adding to the projection SRID to it
     *
     * @param object        - {@link Object inputObject}
     * @param srid          - {@link Integer} srid
     * @param geometryCache - {@link Cache} of pre-computed geometries to use
     * @return - {@link Geometry}
     */
    public static Geometry getGeomFromCache(Object object, int srid, Cache<Integer, Geometry> geometryCache) {
        final int objectKey = object.hashCode() + srid;
        final Geometry value = geometryCache.get(objectKey);

        if (value == null) {
            final Geometry geometry = computeGeometry(object, srid);
            geometryCache.put(objectKey, geometry);
            return geometry;
        }
        return value;
    }

    /**
     * Add Buffer to WKT value
     * If Buffer amount is 0, the original WKT is returned
     * Buffer amount is in the provided unit
     *
     * @param wkt        - {@link String} WKT string
     * @param buffer     - {@link Double} buffer amount, in meters
     * @param lengthUnit -{@link Unit} of {@link Length} units of buffer
     * @return - {@link String} WKT with buffer
     */
    public static String addWKTBuffer(String wkt, double buffer, Unit<Length> lengthUnit) {
        if (buffer > 0.0) {
//            Convert from whatever it is, to Meters, which is the distance unit of the default WGS84 (4326) projection
            final UnitConverter converterTo = lengthUnit.getConverterTo(METER);
            final double meterBuffer = converterTo.convert(buffer);
            logger.trace("Adding {} buffer to WKT", buffer);
            final WKTWriter writer = new WKTWriter();
            final WKTReader reader = new WKTReader(new GeometryFactory(new PrecisionModel(), 4326));
            try {
                return writer.write(reader.read(wkt).buffer(meterBuffer));
            } catch (ParseException e) {
                logger.error("Unable to parse wkt");
                throw new TrestleInvalidDataException("Unable to parse WKT", wkt);
            }
        }
        return wkt;
    }

    /**
     * Compute {@link Geometry} for the given {@link Object}
     *
     * @param object  - {@link Object} to get Geometry from
     * @param inputSR - {@link Integer} input spatial reference
     * @return - {@link Geometry}
     */
    @Metered(name = "geometry-calculation-meter")
    private static Geometry computeGeometry(Object object, int inputSR) {
        logger.trace("Cache miss for {}, computing", object);
        return SpatialEngineUtils.buildObjectGeometry(object, inputSR);
    }


    /**
     * Compute {@link Geometry} for the given {@link Object}
     *
     * @param object  - {@link Object} to get Geometry from
     * @param inputSR - {@link Integer} input spatial reference
     * @return - {@link Geometry}
     */
    public static Geometry buildObjectGeometry(Object object, int inputSR) {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), inputSR);
        final WKTReader wktReader = new WKTReader(geometryFactory);
        final WKBReader wkbReader = new WKBReader(geometryFactory);
        return buildObjectGeometry(object, wktReader, wkbReader);
    }

    /**
     * Compute {@link Geometry} for the given {@link Object} using the provided Readers
     *
     * @param object    - {@link Object} to get Geometry from
     * @param wkbReader - {@link WKBReader} to use
     * @param wktReader - {@link WKTReader} to use
     * @return - {@link Geometry}
     */
    public static Geometry buildObjectGeometry(Object object, WKTReader wktReader, WKBReader wkbReader) {
        return parseJTSGeometry(SpatialParser.getSpatialValue(object), wktReader, wkbReader);
    }

    /**
     * Returns a new {@link WKTReader} for the specified SRID
     *
     * @param srid - {@link Integer} SRID
     * @return - {@link WKTReader} projected reader
     */
    public static WKTReader getProjectedReader(int srid) {
        return new WKTReader(new GeometryFactory(new PrecisionModel(), srid));
    }

    /**
     * Extract exterior Polygon rings from a given {@link Geometry}
     * Implicitly expects either a {@link com.vividsolutions.jts.geom.MultiPolygon} or {@link com.vividsolutions.jts.geom.Polygon}
     * otherwise, it will throw a {@link ClassCastException} when trying to cast the ring to a Polygon type
     *
     * @param inputGeoms - {@link List} of {@link Geometry} to extract exteriors from
     * @param srid       - {@link Integer} input SRID of geometries
     * @return - {@link List} of exterior {@link com.vividsolutions.jts.geom.Polygon}
     */
    public static List<com.vividsolutions.jts.geom.Polygon> getJTSExteriorRings(List<Geometry> inputGeoms, int srid) {
//        Create a new Geometry factory
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
        List<com.vividsolutions.jts.geom.Polygon> polygonExteriors = new ArrayList<>();
        for (Geometry inputGeom : inputGeoms) {
            for (int i = 0; i < inputGeom.getNumGeometries(); i++) {
                final com.vividsolutions.jts.geom.Polygon polygon = com.vividsolutions.jts.geom.Polygon.class.cast(inputGeom.getGeometryN(i));
                polygonExteriors.add(geometryFactory
                        .createPolygon(polygon
                                .getExteriorRing()
                                .getCoordinates()));
            }
        }
        return polygonExteriors;
    }

    /**
     * Build a {@link Geometry} from a given {@link Object} representing the spatial value
     *
     * @param spatialValue - {@link Optional} {@link Object} representing a spatialValue
     * @param wktReader    - {@link WKTReader} for marshalling Strings to Geometries
     * @param wkbReader    - {@link WKBReader} for converting ESRI {@link Polygon} to JTS Geom
     * @return - {@link Polygon}
     * @throws IllegalArgumentException    if the {@link Object} is not a subclass of {@link Polygon} or {@link String}
     * @throws TrestleInvalidDataException if JTS is unable to Parse the spatial input
     */
    private static Geometry parseJTSGeometry(Optional<Object> spatialValue, WKTReader wktReader, WKBReader wkbReader) {
        final Object spatial = spatialValue.orElseThrow(() -> new IllegalStateException("Cannot get spatial value for object"));
        try {
            if (spatial instanceof Geometry) {
                return (Geometry) spatial;
            } else if (spatial instanceof Polygon) {
//                Progress tracker can be null, so this is an error
                @SuppressWarnings("argument.type.incompatible") final ByteBuffer wkbBuffer = operatorExport.execute(0, (Polygon) spatial, null);
                return wkbReader.read(wkbBuffer.array());
            } else if (spatial instanceof String) {
                return wktReader.read(String.class.cast(spatial));
            }
            throw new IllegalArgumentException("Unsupported spatial type");
        } catch (ParseException e) {
            throw new TrestleInvalidDataException(e.getMessage(), spatial);
        }
    }
}

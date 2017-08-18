package com.nickrobison.trestle.reasoner.parser;

import com.esri.core.geometry.Geometry;
import com.nickrobison.trestle.reasoner.annotations.Fact;
import com.nickrobison.trestle.reasoner.annotations.Spatial;
import com.nickrobison.trestle.reasoner.parser.spatial.ESRIParser;
import com.nickrobison.trestle.reasoner.parser.spatial.GeotoolsParser;
import com.nickrobison.trestle.reasoner.parser.spatial.JTSParser;
import com.vividsolutions.jts.io.ParseException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static com.nickrobison.trestle.common.StaticIRI.GEOSPARQLPREFIX;
import static com.nickrobison.trestle.common.StaticIRI.WKTDatatypeIRI;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.dfStatic;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.filterMethodName;

/**
 * Created by nrobison on 8/29/16.
 */
public class SpatialParser {
    private static final Logger logger = LoggerFactory.getLogger(SpatialParser.class);

    private SpatialParser() {

    }

    static Optional<OWLLiteral> parseWKTFromGeom(Object spatialObject) {

        final OWLDatatype wktDatatype = dfStatic.getOWLDatatype(WKTDatatypeIRI);
        final OWLLiteral wktLiteral;
        final String typeName = spatialObject.getClass().getTypeName();
        if (typeName.contains("java.lang.String")) {
            wktLiteral = dfStatic.getOWLLiteral(spatialObject.toString().replace("\"", ""), wktDatatype);
        } else if (typeName.contains("com.vividsolutions")) {
            String jtsWKT = JTSParser.parseJTSToWKT(spatialObject);
            wktLiteral = dfStatic.getOWLLiteral(jtsWKT, wktDatatype);
        } else if (typeName.contains("com.esri.core.geometry")) {
            final String esriWKT = ESRIParser.parseESRIToWKT((Geometry) spatialObject);
            wktLiteral = dfStatic.getOWLLiteral(esriWKT, wktDatatype);
        } else if (typeName.contains("org.opengis.geometry")) {
            final String geoToolsWKT = GeotoolsParser.parseGeotoolsToWKT((org.opengis.geometry.Geometry) spatialObject);
            wktLiteral = dfStatic.getOWLLiteral(geoToolsWKT, wktDatatype);
        } else {
            return Optional.empty();
        }
//                ESRI
//                Geotools
        return Optional.of(wktLiteral);
    }

    static Optional<Object> parseWKTtoGeom(String wkt, Class<?> geomClass) {
        final String typeName = geomClass.getTypeName();
        if (typeName.contains("java.lang.String")) {
            return Optional.of(wkt);
        } else if (typeName.contains("com.vividsolutions")) {
            try {
                Object jtsGeom = JTSParser.wktToJTSObject(wkt, geomClass);
                return Optional.of(geomClass.cast(jtsGeom));
            } catch (ParseException e) {
                logger.error("Cannot parse wkt to ESRI geom", e);
            }
        } else if (typeName.contains("com.esri.core.geometry")) {
            final Object esriGeom = ESRIParser.wktToESRIObject(wkt, geomClass);
            return Optional.of(geomClass.cast(esriGeom));
        } else if (typeName.contains("org.opengis.geometry")) {
            try {
                final Object geotoolsObject = GeotoolsParser.wktToGeotoolsObject(wkt, geomClass);
                return Optional.of(geomClass.cast(geotoolsObject));
            } catch (Exception e) {
                logger.error("Cannot parse wkt to geotools geoms", e);
            }
        }
        return Optional.empty();
    }

    static Class<?> getSpatialClass(Class<?> clazz) {

        //        Methods first
        final Optional<Method> matchedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Spatial.class))
                .findFirst();
        if (matchedMethod.isPresent()) {
            return matchedMethod.get().getReturnType();
        }

//        Fields
        final Optional<Field> matchedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (matchedField.isPresent()) {
            return matchedField.get().getType();
        }

        return String.class;

    }


    /**
     * Returns the {@link String} representation of the spatial fact of the given {@link Object}
     *
     * @param inputObject - {@link Object} to extract spatial fact from
     * @return - {@link Optional} {@link String} value of spatial data
     */
    public static Optional<String> getSpatialValueAsString(Object inputObject) {
        final Optional<Object> spatialValue = getSpatialValue(inputObject);
        if (spatialValue.isPresent()) {
            return Optional.of(spatialValue.get().toString());
        }
        return Optional.empty();
    }

    /**
     * Returns the the spatial fact of the given {@link Object}
     *
     * @param inputObject - {@link Object} to extract spatial fact from
     * @return - {@link Optional} {@link Object} value of spatial data
     */
    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    public static Optional<Object> getSpatialValue(Object inputObject) {
        final Class<?> clazz = inputObject.getClass();

//        Methods first
        final Optional<Method> matchedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (matchedMethod.isPresent()) {
            final Optional<Object> methodValue = ClassParser.accessMethodValue(matchedMethod.get(), inputObject);

            if (methodValue.isPresent()) {
                return Optional.of(methodValue.get());
            }
        }

//        Now fields
        final Optional<Field> matchedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (matchedField.isPresent()) {
            Object fieldValue = null;
            try {
                fieldValue = matchedField.get().get(inputObject);
            } catch (IllegalAccessException e) {
                logger.warn("Cannot access field {}", matchedField.get().getName(), e);
            }

            if (fieldValue != null) {
                return Optional.of(fieldValue);
            }
        }

        return Optional.empty();
    }

    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    static IRI filterDataSpatialName(Field classField, String propertyPrefix) {
        if (classField.isAnnotationPresent(Fact.class)) {
            return IRI.create(propertyPrefix, classField.getAnnotation(Fact.class).name());
        } else if (classField.isAnnotationPresent(Spatial.class)) {
            return IRI.create(GEOSPARQLPREFIX, "asWKT");
        } else {
            return IRI.create(propertyPrefix, classField.getName());
        }
    }

    //    We can suppress this for default annotation properties
    @SuppressWarnings({"dereference.of.nullable"})
    static IRI filterDataSpatialName(Method classMethod, String propertyPrefix) {
        if (classMethod.isAnnotationPresent(Fact.class)) {
            return IRI.create(propertyPrefix, classMethod.getAnnotation(Fact.class).name());
        } else if (classMethod.isAnnotationPresent(Spatial.class)) {
            return IRI.create(GEOSPARQLPREFIX, "asWKT");
        } else {
            return IRI.create(propertyPrefix, filterMethodName(classMethod));
        }
    }
}

package com.nickrobison.trestle.parser;

import com.esri.core.geometry.Geometry;
import com.nickrobison.trestle.annotations.DataProperty;
import com.nickrobison.trestle.annotations.Spatial;
import com.nickrobison.trestle.parser.spatial.ESRIParser;
import com.nickrobison.trestle.parser.spatial.JTSParser;
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
import static com.nickrobison.trestle.common.StaticIRI.PREFIX;
import static com.nickrobison.trestle.common.StaticIRI.WKTDatatypeIRI;
import static com.nickrobison.trestle.parser.ClassParser.df;
import static com.nickrobison.trestle.parser.ClassParser.filterMethodName;

/**
 * Created by nrobison on 8/29/16.
 */
public class SpatialParser {

    private static final Logger logger = LoggerFactory.getLogger(SpatialParser.class);

    static Optional<OWLLiteral> parseWKTFromGeom(Object spatialObject) {

        final OWLDatatype wktDatatype = df.getOWLDatatype(WKTDatatypeIRI);
        final OWLLiteral wktLiteral;
        final String typeName = spatialObject.getClass().getTypeName();
        if (typeName.contains("java.lang.String")) {
            wktLiteral = df.getOWLLiteral(spatialObject.toString().replace("\"", ""), wktDatatype);
        } else if (typeName.contains("com.vividsolutions")) {
            String jtsWKT = JTSParser.parseJTSToWKT(spatialObject);
            wktLiteral = df.getOWLLiteral(jtsWKT, wktDatatype);
        } else if (typeName.contains("com.esri.core.geometry")) {
            final String esriWKT = ESRIParser.parseESRIToWKT((Geometry) spatialObject);
            wktLiteral = df.getOWLLiteral(esriWKT, wktDatatype);
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
                logger.error("Cannot case wkt to geom", e);
            }
        } else if (typeName.contains("com.esri.core.geometry")) {
            final Object esriGeom = ESRIParser.wktToESRIObject(wkt, geomClass);
            return Optional.of(geomClass.cast(esriGeom));
        }
        return Optional.empty();
    }

    static Class<?> GetSpatialClass(Class<?> clazz) {

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

    public static Optional<String> GetSpatialValue(Object inputObject) {
        final Class<?> clazz = inputObject.getClass();

//        Methods first
        final Optional<Method> matchedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (matchedMethod.isPresent()) {
            final Optional<Object> methodValue = ClassParser.accessMethodValue(matchedMethod.get(), inputObject);

            if (methodValue.isPresent()) {
                return Optional.of(methodValue.get().toString());
            }
        }

//        Now fields
        final Optional<Field> matchedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (matchedField.isPresent()) {
            String fieldValue = null;
            try {
                fieldValue = matchedField.get().get(inputObject).toString();
            } catch (IllegalAccessException e) {
                logger.debug("Cannot access field {}", matchedField.get().getName(), e);
            }

            if (fieldValue != null) {
                return Optional.of(fieldValue);
            }
        }

        return Optional.empty();
    }

    static IRI filterDataSpatialName(Field classField) {
        if (classField.isAnnotationPresent(DataProperty.class)) {
            return IRI.create(PREFIX, classField.getAnnotation(DataProperty.class).name());
        } else if (classField.isAnnotationPresent(Spatial.class)) {
            return IRI.create(GEOSPARQLPREFIX, "asWKT");
        } else {
            return IRI.create(PREFIX, classField.getName());
        }
    }

    static IRI filterDataSpatialName(Method classMethod) {
        if (classMethod.isAnnotationPresent(DataProperty.class)) {
            return IRI.create(PREFIX, classMethod.getAnnotation(DataProperty.class).name());
        } else if (classMethod.isAnnotationPresent(Spatial.class)) {
            return IRI.create(GEOSPARQLPREFIX, "asWKT");
        } else {
            return IRI.create(PREFIX, filterMethodName(classMethod));
        }
    }
}

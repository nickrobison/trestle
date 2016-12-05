package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.DataProperty;
import com.vividsolutions.jts.geom.Geometry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

import static com.nickrobison.trestle.common.StaticIRI.WKTDatatypeIRI;
import static com.nickrobison.trestle.common.StaticIRI.dateDatatypeIRI;
import static com.nickrobison.trestle.parser.ClassParser.dfStatic;
import static com.nickrobison.trestle.parser.ClassParser.getFieldName;
import static com.nickrobison.trestle.parser.ClassParser.getMethodName;

/**
 * Created by nrobison on 8/24/16.
 */
public class TypeConverter {

    private static final Logger logger = LoggerFactory.getLogger(TypeConverter.class);
    private static final Map<OWLDatatype, Class<?>> datatypeMap = buildDatatype2ClassMap();
    private static final Map<Class<?>, OWLDatatype> owlDatatypeMap = buildClassMap();
    private static final Map<String, Function> javaClassConstructors = new HashMap<>();

    public static void registerTypeConstructor(Class<?> clazz, @Nullable OWLDatatype datatype, Function constructorFunc) {
        logger.debug("Registering java class {} with OWLDatatype {}", clazz.getName(), datatype.getIRI());
        datatypeMap.put(datatype, clazz);
        owlDatatypeMap.put(clazz, datatype);
        javaClassConstructors.put(clazz.getTypeName(), constructorFunc);
    }

    /**
     * Extracts a java object of type T from a given OWL Literal
     * Also handles the object/primitive conversion
     * @param javaClass - Java class to cast literal into
     * @param literal - OWLLiteral to extract
     * @param <T> - Java type
     * @return Java type of type T
     */
    //    I need the unchecked casts in order to get the correct primitives for the constructor generation
    @SuppressWarnings({"unchecked"})
    public static <T> @NonNull T extractOWLLiteral(Class<@NonNull T> javaClass, OWLLiteral literal) {

        switch (javaClass.getTypeName()) {

            case "int": {
                return (@NonNull T) (Object) Integer.parseInt(literal.getLiteral());
            }

            case "java.lang.Integer": {
                return javaClass.cast(Integer.parseInt(literal.getLiteral()));
            }

            case "long": {
                return (@NonNull T) (Object) Long.parseLong(literal.getLiteral());
            }

            case "java.lang.Long": {
                return javaClass.cast(Long.parseLong(literal.getLiteral()));
            }

            case "java.lang.LocalDateTime": {
                return javaClass.cast(LocalDateTime.parse(literal.getLiteral()));
            }

            case "java.lang.String": {
                return javaClass.cast(literal.getLiteral());
            }

            case "java.lang.Double": {
                return javaClass.cast(literal.parseDouble());
            }

            case "java.lang.Boolean": {
                return javaClass.cast(literal.parseBoolean());
            }

            default: {
//                    Is it a geom type?
                final Optional<Object> geomObject = SpatialParser.parseWKTtoGeom(literal.getLiteral(), javaClass);
                if (geomObject.isPresent()) {
                    return javaClass.cast(geomObject.get());
                }
//                    Try to get a match from the custom constructor registry
                final Function constructorFunction = javaClassConstructors.get(javaClass.getTypeName());
                if (constructorFunction == null) {
                    throw new RuntimeException(String.format("Unsupported cast %s", javaClass));
                }

                return javaClass.cast(constructorFunction.apply(literal.getLiteral()));
            }
        }
    }

    /**
     * Lookup java type from OWL Datatype
     * If classToVerify is not null, check against the class in case the constructor requires a primitive
     *
     * @param dataproperty  - OWLDataPropertyAssertionAxiom to get java type from
     * @param classToVerify - @Nullable Class to cross-check with to ensure we're parsing the correct boxed/unboxed type.
     * @return - Java Class corresponding to OWL Datatype and required Class constructor argument
     */
    @SuppressWarnings("dereference.of.nullable")
    public static Class<?> lookupJavaClassFromOWLDatatype(OWLDataPropertyAssertionAxiom dataproperty, @Nullable Class<?> classToVerify) {
        final Class<?> javaClass;
        final OWLDatatype datatype = dataproperty.getObject().getDatatype();
        if (datatype.isBuiltIn()) {

            //            Check with the class to make sure the types are correct
            OWLDatatype dataTypeToLookup = null;
            if (classToVerify != null) {
                dataTypeToLookup = verifyOWLType(classToVerify, dataproperty.getProperty().asOWLDataProperty());
            }
            if (dataTypeToLookup == null) {
                dataTypeToLookup = datatype.getBuiltInDatatype().getDatatype(dfStatic);
            }
            javaClass = datatypeMap.get(dataTypeToLookup);
            if (javaClass == null) {
                throw new RuntimeException(String.format("Unsupported OWLDatatype %s", datatype));
            }
//            If it's from the geosparql group, we need to figure out the correct return class
//                Virtuoso smashes everything into its own Geometry class, so geosparql isn't sufficient.
        } else if (datatype.getIRI().getShortForm().equals("wktLiteral") || datatype.getIRI().getShortForm().equals("Geometry")) {
            if (classToVerify == null) {
                javaClass = String.class;
            } else {
                javaClass = SpatialParser.GetSpatialClass(classToVerify);
            }
        } else {
            //            String as a last resort.
            javaClass = String.class;
        }

        return javaClass;
    }

    /**
     * Inspect Java class to determine correct datatype for a given OWLDataProperty
     * @param classToVerify - Class to verify type against
     * @param property - OWLDataProperty to lookup
     * @return - Java class of corresponding data property
     */
    public static Class<?> lookupJavaClassFromOWLDataProperty(Class<?> classToVerify, OWLDataProperty property) {
        final @Nullable OWLDatatype owlDatatype = verifyOWLType(classToVerify, property);
        @Nullable Class<?> javaClass = null;
        if (owlDatatype != null) {
            javaClass = datatypeMap.get(owlDatatype);
        }

        if (javaClass == null) {
//            If we have a WKT, we need to handle it like a string
            if (property.asOWLDataProperty().getIRI().getShortForm().equals("asWKT")) {
                return String.class;
            }
            throw new RuntimeException(String.format("Unsupported dataproperty %s", property.asOWLDataProperty().getIRI()));
        }

        return javaClass;

    }

    private static @Nullable OWLDatatype verifyOWLType(Class<?> classToVerify, OWLDataProperty property) {

        //        Check to see if it matches any annotated data methods
        final Optional<Method> matchedMethod = Arrays.stream(classToVerify.getDeclaredMethods())
                .filter(m -> getMethodName(m).equals(property.getIRI().getShortForm()))
                .findFirst();

        if (matchedMethod.isPresent()) {
            return getDatatypeFromJavaClass(matchedMethod.get().getReturnType());
        }

        //        Fields
        final Optional<Field> matchedField = Arrays.stream(classToVerify.getDeclaredFields())
                .filter(f -> getFieldName(f).equals(property.getIRI().getShortForm()))
                .findFirst();

        if (matchedField.isPresent()) {
            return getDatatypeFromJavaClass(matchedField.get().getType());
        }

        return null;
    }

    static Map<OWLDatatype, Class<?>> buildDatatype2ClassMap() {
        Map<OWLDatatype, Class<?>> datatypeMap = new HashMap<>();

        datatypeMap.put(OWL2Datatype.XSD_INTEGER.getDatatype(dfStatic), Integer.class);
        datatypeMap.put(OWL2Datatype.XSD_INT.getDatatype(dfStatic), int.class);
        datatypeMap.put(OWL2Datatype.XSD_LONG.getDatatype(dfStatic), long.class);
        datatypeMap.put(OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic), Double.class);
        datatypeMap.put(OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic), double.class);
        datatypeMap.put(OWL2Datatype.XSD_DECIMAL.getDatatype(dfStatic), Double.class);
        datatypeMap.put(OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic), LocalDateTime.class);
        datatypeMap.put(dfStatic.getOWLDatatype(dateDatatypeIRI), LocalDate.class);
        datatypeMap.put(OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic), Boolean.class);
        datatypeMap.put(OWL2Datatype.XSD_STRING.getDatatype(dfStatic), String.class);

        return datatypeMap;
    }

    private static Map<Class<?>, OWLDatatype> buildClassMap() {
        Map<Class<?>, OWLDatatype> types = new HashMap<>();
        types.put(Integer.class, OWL2Datatype.XSD_INTEGER.getDatatype(dfStatic));
        types.put(int.class, OWL2Datatype.XSD_INT.getDatatype(dfStatic));
        types.put(Double.class, OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic));
        types.put(double.class, OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic));
        types.put(Float.class, OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic));
        types.put(float.class, OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic));
        types.put(Boolean.class, OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic));
        types.put(boolean.class, OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic));
        types.put(Long.class, OWL2Datatype.XSD_LONG.getDatatype(dfStatic));
        types.put(long.class, OWL2Datatype.XSD_LONG.getDatatype(dfStatic));
        types.put(String.class, OWL2Datatype.XSD_STRING.getDatatype(dfStatic));
//        Java temporals
        types.put(LocalDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(LocalDate.class, dfStatic.getOWLDatatype(dateDatatypeIRI));
        types.put(OffsetDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(ZonedDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
//        Joda temporals
        types.put(DateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(org.joda.time.LocalDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(org.joda.time.LocalDate.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic));
        types.put(Geometry.class, dfStatic.getOWLDatatype(WKTDatatypeIRI));

        return types;
    }

    static OWLDatatype getDatatypeFromAnnotation(DataProperty annotation, Class<?> objectClass) {
//        I don't think this will ever be true
        if (annotation.datatype().toString().equals("") || annotation.datatype().equals(OWL2Datatype.XSD_NMTOKEN)) {
            return getDatatypeFromJavaClass(objectClass);
        } else {
            return annotation.datatype().getDatatype(dfStatic);
        }
    }

    static @NotNull OWLDatatype getDatatypeFromJavaClass(Class<?> javaTypeClass) {
        OWLDatatype owlDatatype = owlDatatypeMap.get(javaTypeClass);
        if (owlDatatype == null) {
            logger.error("Unsupported Java type {}", javaTypeClass);
//            throw new RuntimeException(String.format("Unsupported Java type %s", javaTypeClass));
            owlDatatype = OWL2Datatype.XSD_STRING.getDatatype(dfStatic);
        }
        return owlDatatype;
    }

    @SuppressWarnings("Duplicates")
    static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
            logger.debug("Converting primitive type {} to object", returnClass.getTypeName());
            switch (returnClass.getTypeName()) {
                case "int": {
                    return Integer.class;
                }
                case "double": {
                    return Double.class;
                }
                case "boolean": {
                    return boolean.class;
                }
                case "long": {
                    return Long.class;
                }
                default: {
                    throw new RuntimeException(String.format("Unsupported cast of %s to primitive type", returnClass.getTypeName()));
                }
            }
        }

        return returnClass;
    }
}

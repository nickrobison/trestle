package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.DataProperty;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
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
import java.util.*;

import static com.nickrobison.trestle.common.StaticIRI.UUIDDatatypeIRI;
import static com.nickrobison.trestle.common.StaticIRI.dateDatatypeIRI;
import static com.nickrobison.trestle.parser.ClassParser.df;
import static com.nickrobison.trestle.parser.ClassParser.getFieldName;
import static com.nickrobison.trestle.parser.ClassParser.getMethodName;

/**
 * Created by nrobison on 8/24/16.
 */
public class TypeConverter {

    private static final Logger logger = LoggerFactory.getLogger(TypeConverter.class);
    static final Map<OWLDatatype, Class<?>> datatypeMap = buildDatatype2ClassMap();
    private static final Map<Class<?>, OWLDatatype> owlDatatypeMap = buildClassMap();

    //    I need the unchecked casts in order to get the correct primitives for the constructor generation
        @SuppressWarnings({"unchecked"})
    //    TODO(nrobison): Need to provide support for registering your own object generation
        public static <T> @NonNull T extractOWLLiteral(Class<@NonNull T> javaClass, OWLLiteral literal) {

            switch (javaClass.getTypeName()) {

                case "int": {
    //                return javaClass.cast(literal.parseInteger());
                    return (@NonNull T) (Object) Integer.parseInt(literal.getLiteral());
                }

                case "java.lang.Integer": {
                    return javaClass.cast(Integer.parseInt(literal.getLiteral()));
    //                return (T) (Object) literal.parseInteger();
                }

                case "long": {
    //                return javaClass.cast(Long.parseLong(literal.getLiteral()));
                    return (@NonNull T) (Object) Long.parseLong(literal.getLiteral());
                }

                case "java.lang.Long": {
                    return javaClass.cast(Long.parseLong(literal.getLiteral()));
    //                return (T) (Object) Long.parseLong(literal.getLiteral());
                }

                case "java.lang.LocalDateTime": {
                    return javaClass.cast(LocalDateTime.parse(literal.getLiteral()));
    //                return (T) (Object) LocalDateTime.parse(literal.getLiteral());
                }

                case "java.lang.String": {
                    return javaClass.cast(literal.getLiteral());
    //                return (T) (Object) literal.getLiteral();
                }

                case "java.lang.Double": {
                    return javaClass.cast(literal.parseDouble());
    //                return (T) (Object) literal.parseDouble();
                }

                case "java.lang.Boolean": {
                    return javaClass.cast(literal.parseBoolean());
    //                return (T) (Object) literal.parseBoolean();
                }

                case "java.util.UUID": {
                    return javaClass.cast(UUID.fromString(literal.getLiteral()));
                }

                default: {
                    throw new RuntimeException(String.format("Unsupported cast %s", javaClass));
                }
            }
        }

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
                    dataTypeToLookup = datatype.getBuiltInDatatype().getDatatype(df);
                }
                javaClass = datatypeMap.get(dataTypeToLookup);
                if (javaClass == null) {
                    throw new RuntimeException(String.format("Unsupported OWLDatatype %s", datatype));
                }
            } else if (datatype.getIRI().getScheme().equals("geosparql")) {
    //            If it's from the geosparql group, we can just treat it as a string
                javaClass = String.class;
            } else {
    //            String as a last resort.
                javaClass = String.class;
            }

            return javaClass;
        }

    private static OWLDatatype verifyOWLType(Class<?> classToVerify, OWLDataProperty property) {

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

    //    TODO(nrobison): Need to add support for registering your own type mappings.
    static Map<OWLDatatype, Class<?>> buildDatatype2ClassMap() {
        Map<OWLDatatype, Class<?>> datatypeMap = new HashMap<>();

        datatypeMap.put(OWL2Datatype.XSD_INTEGER.getDatatype(df), Integer.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_INT.getDatatype(df), int.class);
        datatypeMap.put(OWL2Datatype.XSD_LONG.getDatatype(df), long.class);
        datatypeMap.put(OWL2Datatype.XSD_DOUBLE.getDatatype(df), Double.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_FLOAT.getDatatype(df), double.class);
        datatypeMap.put(OWL2Datatype.XSD_DECIMAL.getDatatype(df), Double.class);
        datatypeMap.put(OWL2Datatype.XSD_DATE_TIME.getDatatype(df), LocalDateTime.class);
        datatypeMap.put(df.getOWLDatatype(dateDatatypeIRI), LocalDate.class);
        datatypeMap.put(OWL2Datatype.XSD_BOOLEAN.getDatatype(df), Boolean.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_STRING.getDatatype(df), String.class);
        datatypeMap.put(df.getOWLDatatype(UUIDDatatypeIRI), UUID.class);

        return datatypeMap;
    }

    static OWLDatatype getDatatypeFromAnnotation(DataProperty annotation, Class<?> objectClass) {
//        I don't think this will ever be true
        if (annotation.datatype().toString().equals("")) {
            return getDatatypeFromJavaClass(objectClass);
        } else {
            return annotation.datatype().getDatatype(df);
        }
    }

    static
    @NotNull
    OWLDatatype getDatatypeFromJavaClass(Class<?> javaTypeClass) {
        OWLDatatype owlDatatype = owlDatatypeMap.get(javaTypeClass);
        if (owlDatatype == null) {
            logger.error("Unsupported Java type {}", javaTypeClass);
//            throw new RuntimeException(String.format("Unsupported Java type %s", javaTypeClass));
            owlDatatype = OWL2Datatype.XSD_STRING.getDatatype(df);
        }
        return owlDatatype;
    }

    //    TODO(nrobison): Need to add support for registering your own type mappings.
    private static Map<Class<?>, OWLDatatype> buildClassMap() {
        Map<Class<?>, OWLDatatype> types = new HashMap<>();
        types.put(Integer.TYPE, OWL2Datatype.XSD_INTEGER.getDatatype(df));
        types.put(int.class, OWL2Datatype.XSD_INT.getDatatype(df));
        types.put(Double.TYPE, OWL2Datatype.XSD_DOUBLE.getDatatype(df));
        types.put(double.class, OWL2Datatype.XSD_FLOAT.getDatatype(df));
        types.put(Float.TYPE, OWL2Datatype.XSD_DOUBLE.getDatatype(df));
        types.put(float.class, OWL2Datatype.XSD_FLOAT.getDatatype(df));
        types.put(Boolean.TYPE, OWL2Datatype.XSD_BOOLEAN.getDatatype(df));
        types.put(boolean.class, OWL2Datatype.XSD_BOOLEAN.getDatatype(df));
        types.put(Long.TYPE, OWL2Datatype.XSD_LONG.getDatatype(df));
        types.put(long.class, OWL2Datatype.XSD_LONG.getDatatype(df));
        types.put(String.class, OWL2Datatype.XSD_STRING.getDatatype(df));
        types.put(LocalDateTime.class, OWL2Datatype.XSD_DATE_TIME.getDatatype(df));
        types.put(LocalDate.class, df.getOWLDatatype(dateDatatypeIRI));
        types.put(UUID.class, df.getOWLDatatype(UUIDDatatypeIRI));

        return types;
    }

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

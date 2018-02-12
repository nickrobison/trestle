package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.annotations.Fact;
import com.vividsolutions.jts.geom.Geometry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.nickrobison.trestle.common.StaticIRI.WKTDatatypeIRI;
import static com.nickrobison.trestle.common.StaticIRI.dateDatatypeIRI;
import static com.nickrobison.trestle.reasoner.parser.ClassParser.*;

/**
 * Created by nrobison on 8/24/16.
 */
@SuppressWarnings({"argument.type.incompatible"})
public class TypeConverter {

    private static final Logger logger = LoggerFactory.getLogger(TypeConverter.class);
    private static final Map<OWLDatatype, Class<?>> datatypeMap = buildDatatype2ClassMap();
    private static final Map<Class<?>, OWLDatatype> owlDatatypeMap = buildClassMap();
    private static final Map<String, TypeConstructor> javaClassConstructors = new HashMap<>();

    private TypeConverter() {
//        Not used
    }

    public static void registerTypeConstructor(TypeConstructor constructor) {
        final Class javaClass = constructor.getJavaType();
        if (owlDatatypeMap.containsKey(javaClass)) {
            logger.warn("Overwriting mapping of Java Class {} with {}", javaClass, constructor.getConstructorName());
        } else {
            logger.info("Registering Type Constructor {} for {} and {}", constructor.getConstructorName(),
                    javaClass, constructor.getOWLDatatype());
        }
        final OWLDatatype owlDatatype = dfStatic.getOWLDatatype(IRI.create(constructor.getOWLDatatype()));
        datatypeMap.put(owlDatatype, javaClass);
        owlDatatypeMap.put(javaClass, owlDatatype);
        javaClassConstructors.put(javaClass.getTypeName(), constructor);
    }

    /**
     * Extracts a java object of type T from a given OWL Literal
     * Also handles the object/primitive conversion
     *
     * @param javaClass - Java class to cast literal into
     * @param literal   - OWLLiteral to extract
     * @param <T>       - Java type
     * @return Java type of type T
     */
    //    I need the unchecked casts in order to get the correct primitives for the constructor generation
    @SuppressWarnings({"unchecked", "return.type.incompatible", "squid:S1199"})
    public static <@NonNull T> @NonNull T extractOWLLiteral(Class<@NonNull T> javaClass, OWLLiteral literal) {

        switch (javaClass.getTypeName()) {

            case "int": {
                return (@NonNull T) (Object) Integer.parseInt(literal.getLiteral());
            }

            case "java.lang.Integer": {
                return javaClass.cast(Integer.parseInt(literal.getLiteral()));
            }

            case "short": {
                return (@NonNull T) (Object) Short.parseShort(literal.getLiteral());
            }

            case "java.lang.Short": {
                return javaClass.cast(Short.parseShort(literal.getLiteral()));
            }

            case "long": {
                return (@NonNull T) (Object) Long.parseLong(literal.getLiteral());
            }

            case "java.lang.Long": {
                return javaClass.cast(Long.parseLong(literal.getLiteral()));
            }

            case "java.time.LocalDateTime": {
                return javaClass.cast(LocalDateTime.parse(literal.getLiteral()));
            }

            case "java.time.LocalDate": {
                return javaClass.cast(LocalDate.parse(literal.getLiteral()));
            }

            case "java.time.OffsetDateTime": {
                return javaClass.cast(OffsetDateTime.parse(literal.getLiteral()));
            }

            case "java.time.ZonedDateTime": {
                return javaClass.cast(ZonedDateTime.parse(literal.getLiteral()));
            }

            case "java.lang.String": {
                return javaClass.cast(literal.getLiteral());
            }

            case "float": {
                return (@NonNull T) (Object) Float.parseFloat(literal.getLiteral());
            }

            case "java.lang.Float": {
                return javaClass.cast(literal.parseFloat());
            }

            case "double": {
                return (@NonNull T) (Object) Double.parseDouble(literal.getLiteral());
            }

            case "java.lang.Double": {
                return javaClass.cast(literal.parseDouble());
            }

            case "boolean": {
                return (@NonNull T) (Object) Boolean.getBoolean(literal.getLiteral());
            }

            case "java.lang.Boolean": {
                return javaClass.cast(literal.parseBoolean());
            }

            case "java.math.BigInteger": {
                return (@NonNull T) new BigInteger(literal.getLiteral());
            }

            case "java.math.BigDecimal": {
                return (@NonNull T) new BigDecimal(literal.getLiteral());
            }

            default: {
//                    Is it a geom type?
                final Optional<Object> geomObject = SpatialParser.parseWKTtoGeom(literal.getLiteral(), javaClass);
                if (geomObject.isPresent()) {
                    return javaClass.cast(geomObject.get());
                }
//                    Try to get a match from the custom constructor registry
                final TypeConstructor constructor = javaClassConstructors.get(javaClass.getTypeName());
                if (constructor == null) {
                    throw new ClassCastException(String.format("Unsupported cast %s", javaClass));
                }

                return javaClass.cast(constructor.constructType(literal.getLiteral()));
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
    @SuppressWarnings({"dereference.of.nullable", "return.type.incompatible"})
    public static Class<@NonNull ?> lookupJavaClassFromOWLDatatype(OWLDataPropertyAssertionAxiom dataproperty, @Nullable Class<?> classToVerify) {
        Class<?> javaClass;
        final OWLDatatype datatype = dataproperty.getObject().getDatatype();
        if (datatype.isBuiltIn()) {

//            Check with the class to make sure the types are correct. Sometimes the ontologies give us the wrong type
            OWLDatatype dataTypeToLookup = null;
            if (classToVerify != null) {
                dataTypeToLookup = verifyOWLType(classToVerify, dataproperty.getProperty().asOWLDataProperty());
            }
            if (dataTypeToLookup == null) {
                dataTypeToLookup = datatype.getBuiltInDatatype().getDatatype(dfStatic);
            }
            javaClass = datatypeMap.get(dataTypeToLookup);
            if (javaClass == null) {
                throw new IllegalArgumentException(String.format("Unsupported OWLDatatype %s", datatype));
            }
//            If it comes back as a primitive, check if we need the full class
            if (javaClass.isPrimitive()) {
                javaClass = getJavaMemberType(classToVerify, dataproperty.getProperty().asOWLDataProperty(), javaClass);
            }
//            If it's from the geosparql group, we need to figure out the correct return class
//                Virtuoso smashes everything into its own Geometry class, so geosparql isn't sufficient.
        } else if (datatype.getIRI().getShortForm().equals("wktLiteral") || datatype.getIRI().getShortForm().equals("Geometry")) {
            if (classToVerify == null) {
                javaClass = String.class;
            } else {
                javaClass = SpatialParser.getSpatialClass(classToVerify);
            }
        } else {
//            Look it up from the datatype map, else return a string
            final Class<?> matchedClass = datatypeMap.get(datatype);
            if (matchedClass == null) {
//            String as a last resort.
                javaClass = String.class;
            } else {
                javaClass = matchedClass;
            }
        }

        return javaClass;
    }

    /**
     * Get the java type for the class member matching the OWLDataProperty
     *
     * @param clazz     - Input class to parse
     * @param property  - OWLDataProperty to match with the class
     * @param inputType - Previously determined type
     * @return - Nullable java type
     */
    private static @Nullable Class<?> getJavaMemberType(@Nullable Class<?> clazz, OWLDataProperty property, Class<?> inputType) {
        if (clazz == null) {
            return inputType;
        }
        final String classMember = property.asOWLDataProperty().getIRI().getShortForm();

        //        Check to see if it matches any annotated data methods
        final Optional<Method> matchedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> getMethodName(m).equals(classMember))
                .findFirst();

        if (matchedMethod.isPresent()) {
            return matchedMethod.get().getReturnType();
        }

        //        Fields
        final Optional<Field> matchedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> getFieldName(f).equals(classMember))
                .findFirst();

        return matchedField.map(Field::getType).orElse(null);

    }

    /**
     * Inspect Java class to determine correct datatype for a given OWLDataProperty
     *
     * @param classToVerify - Class to verify type against
     * @param property      - OWLDataProperty to lookup
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
            throw new IllegalArgumentException(String.format("Unsupported dataproperty %s", property.asOWLDataProperty().getIRI()));
        }

        return javaClass;

    }

    private static @Nullable OWLDatatype verifyOWLType(Class<?> classToVerify, OWLDataProperty property) {

        return getDatatypeFromJavaClass(getJavaMemberType(classToVerify, property, null));
    }

    static Map<OWLDatatype, Class<?>> buildDatatype2ClassMap() {
        Map<OWLDatatype, Class<?>> datatypeMap = new HashMap<>();

        datatypeMap.put(OWL2Datatype.XSD_INTEGER.getDatatype(dfStatic), BigInteger.class);
        datatypeMap.put(OWL2Datatype.XSD_INT.getDatatype(dfStatic), int.class);
        datatypeMap.put(OWL2Datatype.XSD_SHORT.getDatatype(dfStatic), short.class);
        datatypeMap.put(OWL2Datatype.XSD_LONG.getDatatype(dfStatic), long.class);
        datatypeMap.put(OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic), double.class);
        datatypeMap.put(OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic), float.class);
        datatypeMap.put(OWL2Datatype.XSD_DECIMAL.getDatatype(dfStatic), BigDecimal.class);
        datatypeMap.put(OWL2Datatype.XSD_DATE_TIME.getDatatype(dfStatic), LocalDateTime.class);
        datatypeMap.put(dfStatic.getOWLDatatype(dateDatatypeIRI), LocalDate.class);
        datatypeMap.put(OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic), boolean.class);
        datatypeMap.put(OWL2Datatype.XSD_STRING.getDatatype(dfStatic), String.class);
        datatypeMap.put(OWL2Datatype.XSD_BYTE.getDatatype(dfStatic), byte.class);
        datatypeMap.put(OWL2Datatype.RDF_LANG_STRING.getDatatype(dfStatic), String.class);

        return datatypeMap;
    }

    private static Map<Class<?>, OWLDatatype> buildClassMap() {
        Map<Class<?>, OWLDatatype> types = new HashMap<>();
        types.put(Integer.class, OWL2Datatype.XSD_INT.getDatatype(dfStatic));
        types.put(int.class, OWL2Datatype.XSD_INT.getDatatype(dfStatic));
        types.put(Double.class, OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic));
        types.put(double.class, OWL2Datatype.XSD_DOUBLE.getDatatype(dfStatic));
        types.put(float.class, OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic));
        types.put(Float.class, OWL2Datatype.XSD_FLOAT.getDatatype(dfStatic));
        types.put(Boolean.class, OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic));
        types.put(boolean.class, OWL2Datatype.XSD_BOOLEAN.getDatatype(dfStatic));
        types.put(Long.class, OWL2Datatype.XSD_LONG.getDatatype(dfStatic));
        types.put(long.class, OWL2Datatype.XSD_LONG.getDatatype(dfStatic));
        types.put(BigInteger.class, OWL2Datatype.XSD_INTEGER.getDatatype(dfStatic));
        types.put(BigDecimal.class, OWL2Datatype.XSD_DECIMAL.getDatatype(dfStatic));
        types.put(short.class, OWL2Datatype.XSD_SHORT.getDatatype(dfStatic));
        types.put(Short.class, OWL2Datatype.XSD_SHORT.getDatatype(dfStatic));
        types.put(String.class, OWL2Datatype.XSD_STRING.getDatatype(dfStatic));
        types.put(byte.class, OWL2Datatype.XSD_BYTE.getDatatype(dfStatic));
        types.put(Byte.class, OWL2Datatype.XSD_BYTE.getDatatype(dfStatic));
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

    public static OWLDatatype getDatatypeFromAnnotation(Fact annotation, Class<?> objectClass) {
//        I don't think this will ever be true
        if (annotation.datatype().toString().equals("") || annotation.datatype() == OWL2Datatype.XSD_NMTOKEN) {
            return getDatatypeFromJavaClass(objectClass);
        } else {
            return annotation.datatype().getDatatype(dfStatic);
        }
    }

    public static @NotNull
    OWLDatatype getDatatypeFromJavaClass(Class<?> javaTypeClass) {
        OWLDatatype owlDatatype = owlDatatypeMap.get(javaTypeClass);
        if (owlDatatype == null) {
            logger.error("Unsupported Java type {}", javaTypeClass);
            owlDatatype = OWL2Datatype.XSD_STRING.getDatatype(dfStatic);
        }
        return owlDatatype;
    }

    @SuppressWarnings({"Duplicates", "squid:S1199"})
    public static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
            logger.trace("Converting primitive type {} to object", returnClass.getTypeName());
            switch (returnClass.getTypeName()) {
                case "int": {
                    return Integer.class;
                }
                case "float": {
                    return Float.class;
                }
                case "double": {
                    return Double.class;
                }
                case "boolean": {
                    return Boolean.class;
                }
                case "long": {
                    return Long.class;
                }
                default: {
                    throw new ClassCastException(String.format("Unsupported cast of %s to primitive type", returnClass.getTypeName()));
                }
            }
        }

        return returnClass;
    }
}

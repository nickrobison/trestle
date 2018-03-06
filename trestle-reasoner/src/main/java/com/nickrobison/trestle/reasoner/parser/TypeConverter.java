package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.reasoner.annotations.Fact;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.nickrobison.trestle.reasoner.parser.ClassParser.dfStatic;

/**
 * Created by nrobison on 8/24/16.
 */
//I'm suppressing the boxing warning, becaus I think I need it in order to get the correct primitives out
@SuppressWarnings({"argument.type.incompatible", "fb-contrib:NAB_NEEDLESS_BOXING_VALUEOF"})
public class TypeConverter implements ITypeConverter {

    private static final Logger logger = LoggerFactory.getLogger(TypeConverter.class);
    private final Map<OWLDatatype, Class<?>> datatypeMap = TypeUtils.buildDatatype2ClassMap();
    private final Map<Class<?>, OWLDatatype> owlDatatypeMap = TypeUtils.buildClassMap();
    private final Map<String, TypeConstructor> javaClassConstructors = new HashMap<>();

    TypeConverter() {
//        Not used
    }

    @Override
    public void registerTypeConstructor(TypeConstructor constructor) {
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

    //    I need the unchecked casts in order to get the correct primitives for the constructor generation
    @Override
    @SuppressWarnings({"unchecked", "return.type.incompatible", "squid:S1199"})
    public <T extends @NonNull Object> T extractOWLLiteral(Class<T> javaClass, @Nullable OWLLiteral literal) {

        final T extractedLiteral = TypeUtils.rawLiteralConversion(javaClass, literal);
        if (extractedLiteral == null) {
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

        return extractedLiteral;
    }

    @Override
    @SuppressWarnings({"dereference.of.nullable", "return.type.incompatible"})
    public Class<?> lookupJavaClassFromOWLDatatype(OWLDataPropertyAssertionAxiom dataProperty, @Nullable Class<?> javaReturnType) {
        Class<?> javaClass;
        final OWLDatatype datatype = dataProperty.getObject().getDatatype();
        if (datatype.isBuiltIn()) {

//            Check with the class to make sure the types are correct. Sometimes the ontologies give us the wrong type
            OWLDatatype dataTypeToLookup = null;
            if (javaReturnType != null) {
                dataTypeToLookup = verifyOWLType(javaReturnType, dataProperty.getProperty().asOWLDataProperty());
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
                javaClass = getJavaMemberType(javaReturnType, dataProperty.getProperty().asOWLDataProperty(), javaClass);
            }
//            If it's from the geosparql group, we need to figure out the correct return class
//                Virtuoso smashes everything into its own Geometry class, so geosparql isn't sufficient.
        } else if (datatype.getIRI().getShortForm().equals("wktLiteral") || datatype.getIRI().getShortForm().equals("Geometry")) {
//            This is special casing to handle the fact that we can't get correct return types from TrestleIndividuals yet
            if (javaReturnType == null) {
                javaClass = String.class;
            } else {
                javaClass = javaReturnType;
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

    @Override
    public Class<?> lookupJavaClassFromOWLDataProperty(Class<?> classToVerify, OWLDataProperty property) {
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

    @Override
    public OWLDatatype getDatatypeFromAnnotation(Fact annotation, Class<?> returnType) {
//        I don't think this will ever be true
        if (annotation.datatype().toString().equals("") || annotation.datatype() == OWL2Datatype.XSD_ANY_URI) {
            return getDatatypeFromJavaClass(returnType);
        } else {
            return annotation.datatype().getDatatype(dfStatic);
        }
    }


    @Override
    public OWLDatatype getDatatypeFromJavaClass(Class<?> javaTypeClass) {
        OWLDatatype owlDatatype = owlDatatypeMap.get(javaTypeClass);
        if (owlDatatype == null) {
            logger.error("Unsupported Java type {}", javaTypeClass);
            owlDatatype = OWL2Datatype.XSD_STRING.getDatatype(dfStatic);
        }
        return owlDatatype;
    }

    /**
     * Convert the {@link Class} into its corresponding primitive, if it is a primitive type
     * This is required to handle the auto-boxing of the Reflection APIs
     *
     * @param returnClass - {@link Class} to parse
     * @return - Primitive {@link Class}, or original class if the input is not primitive
     */
    @SuppressWarnings({"Duplicates", "squid:S1199"})
    public static Class<?> parsePrimitiveClass(Class<?> returnClass) {
        if (returnClass.isPrimitive()) {
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
        return clazz;
    }

    private OWLDatatype verifyOWLType(Class<?> returnTypeToVerify, OWLDataProperty property) {
        return getDatatypeFromJavaClass(returnTypeToVerify);
    }
}

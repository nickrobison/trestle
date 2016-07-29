package com.nickrobison.trestle.parser;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.*;

import static com.nickrobison.trestle.parser.ClassParser.df;

/**
 * Created by nrobison on 7/28/16.
 */
public class ClassBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClassBuilder.class);
    private static final Map<OWL2Datatype, Class<?>> datatypeMap = buildDatatype2ClassMap();

    /**
     * Get a list of data properties from a given class
     * @param clazz - Class to parse for data property members
     * @return - Optional list of OWLDataProperty from given class
     */
    public static Optional<List<OWLDataProperty>> getPropertyMembers(Class<?> clazz) {

        List<OWLDataProperty> classFields = new ArrayList<>();
        Arrays.stream(clazz.getDeclaredFields())
                .filter(ClassParser::filterDataPropertyField)
                .forEach(field -> classFields.add(df.getOWLDataProperty(IRI.create(ClassParser.PREFIX, field.getName()))));

        Arrays.stream(clazz.getDeclaredMethods())
                .filter(ClassParser::filterDataPropertyMethod)
                .forEach(method -> classFields.add(df.getOWLDataProperty(IRI.create(ClassParser.PREFIX, method.getName()))));

        if (classFields.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(classFields);
    }

//    FIXME(nrobison): I think these warnings are important.
    @SuppressWarnings({"type.argument.type.incompatible", "assignment.type.incompatible"})
    public static <T> T ConstructObject(Class<T> clazz, List<Class<?>> inputClasses, List<Object> inputObjects) {

        final Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(inputClasses.toArray(new Class[inputClasses.size()]));
        } catch (NoSuchMethodException e) {
            logger.error("Cannot get constructor matching params: {}", inputClasses, e);
            throw new RuntimeException("Can't get constructor", e);
        }

        try {
            return (T) constructor.newInstance(inputObjects.toArray(new Object[inputObjects.size()]));
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked", "type.argument.type.incompatible", "assignment.type.incompatible"})
//    FIXME(nrobison): Fix the object casts
    static <T> T extractOWLLiteral(Class<T> javaClass, OWLLiteral literal) {

        switch(javaClass.getTypeName()) {

            case "int": {
                return (T) (Object) literal.parseInteger();
            }
            case "Integer": {
                return (T) (Object) literal.parseInteger();
            }

            case "LocalDateTime": {
                return (T) (Object) LocalDateTime.parse(literal.getLiteral());
            }

            case "java.lang.String": {
                return (T) (Object) literal.getLiteral();
            }

            case "Double": {
                return (T) (Object) literal.parseDouble();
            }

            case "Boolean": {
                return (T) (Object) literal.parseBoolean();
            }

            default: {
                throw new RuntimeException(String.format("Unsupported cast %s", javaClass));
            }
        }
    }

    static Class<?> lookupJavaClassFromOWLDatatype(OWL2Datatype datatype) {
        final Class<?> javaClass = datatypeMap.get(datatype);
        if (javaClass == null) {
            throw new RuntimeException(String.format("Unsupported OWL2Datatype %s", datatype));
        }

        return javaClass;
    }


    private static Map<OWL2Datatype, Class<?>> buildDatatype2ClassMap() {
        Map<OWL2Datatype, Class<?>> datatypeMap = new HashMap<>();

        datatypeMap.put(OWL2Datatype.XSD_INTEGER, Integer.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_INT, int.class);
        datatypeMap.put(OWL2Datatype.XSD_DOUBLE, Double.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_FLOAT, double.class);
        datatypeMap.put(OWL2Datatype.XSD_DATE_TIME, LocalDateTime.class);
        datatypeMap.put(OWL2Datatype.XSD_BOOLEAN, Boolean.TYPE);
        datatypeMap.put(OWL2Datatype.XSD_STRING, String.class);

        return datatypeMap;
    }
}

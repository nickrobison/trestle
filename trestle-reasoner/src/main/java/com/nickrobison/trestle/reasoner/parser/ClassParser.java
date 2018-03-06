package com.nickrobison.trestle.reasoner.parser;

import com.nickrobison.trestle.common.IRIUtils;
import com.nickrobison.trestle.reasoner.annotations.*;
import com.nickrobison.trestle.reasoner.annotations.temporal.DefaultTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.EndTemporal;
import com.nickrobison.trestle.reasoner.annotations.temporal.StartTemporal;
import com.nickrobison.trestle.reasoner.exceptions.MissingConstructorException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.nickrobison.trestle.common.StaticIRI.GEOSPARQLPREFIX;
import static com.nickrobison.trestle.reasoner.parser.SpatialParser.parseOWLLiteralFromGeom;
import static com.nickrobison.trestle.reasoner.parser.StringParser.fieldValueToMultiLangString;
import static com.nickrobison.trestle.reasoner.parser.StringParser.methodValueToMultiLangString;

/**
 * Created by nrobison on 6/28/16.
 */
@SuppressWarnings("initialization")
public class ClassParser implements IClassParser {

    enum AccessType {
        FIELD,
        METHOD
    }

    @Override
    public Object parseClass(Class<?> clazz) {
        return null;
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassParser.class);
    //    TODO(nrobison): Move all of this into a non-static context
    static final OWLDataFactory dfStatic = OWLManager.getOWLDataFactory();

    private final OWLDataFactory df;
    private final ITypeConverter typeConverter;
    private final String ReasonerPrefix;
    private final boolean multiLangEnabled;
    private final String defaultLanguageCode;
    private final Integer defaultProjection;


    @Inject
    ClassParser(@com.nickrobison.trestle.ontology.ReasonerPrefix String reasonerPrefix,
                ITypeConverter typeConverter,
                @MultiLangEnabled boolean multiLangEnabled,
                @DefaultLanguageCode String defaultLanguageCode,
                @DefaultProjection Integer defaultProjection) {
        this.df = OWLManager.getOWLDataFactory();
        this.typeConverter = typeConverter;
        this.ReasonerPrefix = reasonerPrefix;
        this.multiLangEnabled = multiLangEnabled;
        this.defaultLanguageCode = defaultLanguageCode;
        this.defaultProjection = defaultProjection;
    }

    @Override
    public boolean isMultiLangEnabled() {
        return this.multiLangEnabled;
    }

    @Override
    public @Nullable String getDefaultLanguageCode() {
        if (this.defaultLanguageCode.equals("")) {
            return null;
        }
        return this.defaultLanguageCode;
    }

    @Override
    public OWLClass getObjectClass(Object inputObject) {
        //        Get the class name, from the annotation, if possible;
        final Class<?> clazz = inputObject.getClass();
        return getObjectClass(clazz);
    }

    @Override
    public OWLClass getObjectClass(Class<?> clazz) {

        final String className;
        if (clazz.isAnnotationPresent(DatasetClass.class)) {
            className = clazz.getDeclaredAnnotation(DatasetClass.class).name();
        } else {
            className = clazz.getName();
        }
        final IRI iri = IRI.create(this.ReasonerPrefix, className);
        return df.getOWLClass(iri);
    }

    @Override
    public OWLNamedIndividual getIndividual(Object inputObject) {

        final Class<?> clazz = inputObject.getClass();
        String identifier = UUID.randomUUID().toString();
//            Loop through the fields and figure out which one has the identifier

//        Try for fields
        for (Field classField : clazz.getDeclaredFields()) {
            if (classField.isAnnotationPresent(IndividualIdentifier.class)) {
                try {
//                        We only grab the first
//                    Replace the spaces with underscores
//                    FIXME(nrobison): This should be broken into its own function
                    identifier = classField.get(inputObject).toString().replaceAll("\\s+", "_");
                    break;
                } catch (IllegalAccessException e) {
                    logger.warn("Cannot access field {}", classField.getName(), e);
                }
            }
        }

//        Try for methods
        for (Method classMethod : clazz.getMethods()) {
            if (classMethod.isAnnotationPresent(IndividualIdentifier.class)) {

                final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);
                if (methodValue.isPresent()) {
                    identifier = methodValue.get().toString().replaceAll("\\s+", "_");
                }
            }
        }

        return df.getOWLNamedIndividual(IRI.create(ReasonerPrefix, identifier));
    }

    //    TODO(nrobison): Implement this
//    static Optional<List<OWLObjectProperty>> GetObjectProperties(Object inputObject) {
//        final Class<?> clazz = inputObject.getClass();
//
//        if (clazz.isAnnotationPresent(ObjectProperty.class)) {
//            for (Field classField : clazz.getDeclaredFields()) {
//                if (classField.isAnnotationPresent(ObjectProperty.class)) {
//                    final ObjectProperty fieldAnnotation = classField.getAnnotation(ObjectProperty.class);
//                    final ObjectRestriction restriction = fieldAnnotation.restriction();
//                    switch (restriction) {
//                        case SOME: {
//
//                        }
//                    }
//                }
//            }
//        }
//
//        return Optional.empty();
//    }

    static boolean filterFactField(Field objectMember, boolean filterSpatial) {
//        Check first to ignore the field
        return (!objectMember.isAnnotationPresent(Ignore.class)
//                Only access it if it's public
                && Modifier.isPublic(objectMember.getModifiers())
                && (
                objectMember.isAnnotationPresent(Fact.class)
                        || (objectMember.isAnnotationPresent(Spatial.class) && !filterSpatial)
                        || objectMember.isAnnotationPresent(IndividualIdentifier.class)
                        || objectMember.isAnnotationPresent(Language.class)
                        || objectMember.isAnnotationPresent(NoMultiLanguage.class)
                        || (objectMember.getAnnotations().length == 0)))
//                Need to filter out the ebean stuff
                && !(objectMember.getName().contains("_ebean"));
    }

    static boolean filterFactMethod(Method objectMember, boolean filterSpatial) {
//        Check first to ignore the field
        return (!objectMember.isAnnotationPresent(Ignore.class)
//                Only access it if it's public
                && Modifier.isPublic(objectMember.getModifiers())
                && (
                objectMember.isAnnotationPresent(Fact.class)
                        || (objectMember.isAnnotationPresent(Spatial.class) && !filterSpatial)
                        || objectMember.isAnnotationPresent(IndividualIdentifier.class)
                        || objectMember.isAnnotationPresent(Language.class)
                        || objectMember.isAnnotationPresent(NoMultiLanguage.class)
                        || (objectMember.getAnnotations().length == 0)))
//                We need this to filter out setters and equals/hashcode stuff
                && ((objectMember.getParameters().length == 0)
                && !(Objects.equals(objectMember.getName(), "hashCode"))
                && !(Objects.equals(objectMember.getName(), "equals"))
                && !(Objects.equals(objectMember.getName(), "toString"))
//                Need to filter out random ebean methods
                && !(objectMember.getName().contains("_ebean"))
                && (objectMember.getReturnType() != void.class));
    }

    @Override
    public Optional<List<OWLDataPropertyAssertionAxiom>> getFacts(Object inputObject) {
        return getFacts(inputObject, false);
    }

    @Override
    public Optional<List<OWLDataPropertyAssertionAxiom>> getFacts(Object inputObject, boolean filterSpatial) {
        final Class<?> clazz = inputObject.getClass();
        final List<OWLDataPropertyAssertionAxiom> axioms = new ArrayList<>();

        final OWLNamedIndividual owlNamedIndividual = getIndividual(inputObject);

//        Fields:
        for (Field classField : clazz.getDeclaredFields()) {
            if (filterFactField(classField, filterSpatial)) {
                if (classField.isAnnotationPresent(Fact.class)) {
                    final Fact annotation = classField.getAnnotation(Fact.class);
                    final IRI iri = IRI.create(ReasonerPrefix, annotation.name());
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                    Object fieldValue = null;

                    try {
                        fieldValue = classField.get(inputObject);
                    } catch (IllegalAccessException e) {
                        logger.warn("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    if (fieldValue != null) {
//                        If the field values is a String, we need to handle
                        final OWLLiteral owlLiteral = fieldValueToMultiLangString(fieldValue,
                                classField,
                                isMultiLangEnabled(),
                                getDefaultLanguageCode())
                                .orElse(df.getOWLLiteral(fieldValue.toString(),
                                        this.typeConverter.getDatatypeFromAnnotation(annotation, classField.getType())));
                        axioms.add(df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, owlLiteral));
                    }
                } else if (classField.isAnnotationPresent(Spatial.class) && !filterSpatial) {
                    final IRI iri = IRI.create(GEOSPARQLPREFIX, "asWKT");
                    final OWLDataProperty spatialDataProperty = df.getOWLDataProperty(iri);
                    Object fieldValue = null;
                    try {
                        fieldValue = classField.get(inputObject);
                    } catch (IllegalAccessException e) {
                        logger.warn("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    final Optional<OWLLiteral> owlLiteral = parseOWLLiteralFromGeom(fieldValue);
                    owlLiteral.ifPresent(owlLiteral1 -> axioms.add(df.getOWLDataPropertyAssertionAxiom(spatialDataProperty, owlNamedIndividual, owlLiteral1)));
                } else {
                    final IRI iri = IRI.create(ReasonerPrefix, classField.getName());
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                    Object fieldValue = null;
                    try {
                        fieldValue = classField.get(inputObject);
                    } catch (IllegalAccessException e) {
                        logger.warn("Cannot access field {}", classField.getName(), e);
                        continue;
                    }
                    if (fieldValue != null) {
//                            If the field type is a String, we need to handle any multi-lang modifications
                        final OWLLiteral owlLiteral = fieldValueToMultiLangString(fieldValue,
                                classField,
                                isMultiLangEnabled(),
                                getDefaultLanguageCode())
                                .orElse(df.getOWLLiteral(fieldValue.toString(),
                                        this.typeConverter.getDatatypeFromJavaClass(classField.getType())));

                        axioms.add(df.getOWLDataPropertyAssertionAxiom(owlDataProperty, owlNamedIndividual, owlLiteral));
                    }
                }
            }
        }

//        Methods
        for (Method classMethod : clazz.getDeclaredMethods()) {
            if (filterFactMethod(classMethod, filterSpatial)) {
                if (classMethod.isAnnotationPresent(Fact.class)) {
                    final Fact annotation = classMethod.getAnnotation(Fact.class);
                    final IRI iri = IRI.create(ReasonerPrefix, annotation.name());
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);

                    final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);

                    if (methodValue.isPresent()) {
                        final OWLLiteral owlLiteral = methodValueToMultiLangString(methodValue.get(),
                                classMethod,
                                isMultiLangEnabled(),
                                getDefaultLanguageCode())
                                .orElse(df.getOWLLiteral(methodValue.get().toString(), this.typeConverter.getDatatypeFromAnnotation(annotation, classMethod.getReturnType())));

                        axioms.add(df.getOWLDataPropertyAssertionAxiom(
                                owlDataProperty,
                                owlNamedIndividual,
                                owlLiteral
                        ));
                    }
                } else if (classMethod.isAnnotationPresent(Spatial.class) && !filterSpatial) {
                    final IRI iri = IRI.create(GEOSPARQLPREFIX, "asWKT");
                    final OWLDataProperty spatialDataProperty = df.getOWLDataProperty(iri);
                    final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);

                    if (methodValue.isPresent()) {
                        final Optional<OWLLiteral> owlLiteral = parseOWLLiteralFromGeom(methodValue.get());
//                    Since it's a literal, we need to strip out the double quotes.
                        owlLiteral.ifPresent(owlLiteral1 -> axioms.add(df.getOWLDataPropertyAssertionAxiom(spatialDataProperty, owlNamedIndividual, owlLiteral1)));
                    }
                } else {
                    final IRI iri = IRI.create(ReasonerPrefix, filterMethodName(classMethod));
                    final OWLDataProperty owlDataProperty = df.getOWLDataProperty(iri);
                    final Optional<Object> methodValue = accessMethodValue(classMethod, inputObject);
                    if (methodValue.isPresent()) {
                        final OWLLiteral owlLiteral = methodValueToMultiLangString(methodValue.get(),
                                classMethod,
                                isMultiLangEnabled(),
                                getDefaultLanguageCode())
                                .orElse(df.getOWLLiteral(methodValue.get().toString(), this.typeConverter.getDatatypeFromJavaClass(classMethod.getReturnType())));
                        axioms.add(df.getOWLDataPropertyAssertionAxiom(
                                owlDataProperty,
                                owlNamedIndividual,
                                owlLiteral));
                    }
                }
            }
        }

        if (axioms.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(axioms);
    }

    @Override
    public Optional<OWLDataPropertyAssertionAxiom> getSpatialFact(Object inputObject) {
        final OWLNamedIndividual owlNamedIndividual = getIndividual(inputObject);
        final IRI iri = IRI.create(GEOSPARQLPREFIX, "asWKT");
        final OWLDataProperty spatialDataProperty = df.getOWLDataProperty(iri);
//        Methods first
        final Optional<Method> method = Arrays.stream(inputObject.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (method.isPresent()) {
            final Optional<Object> methodValue = accessMethodValue(method.get(), inputObject);

            if (methodValue.isPresent()) {
                final Optional<OWLLiteral> owlLiteral = parseOWLLiteralFromGeom(methodValue.get());
                if (owlLiteral.isPresent()) {
////                    Since it's a literal, we need to strip out the double quotes.
//                        final OWLLiteral wktLiteral = dfStatic.getOWLLiteral(methodValue.get().toString().replace("\"", ""), wktDatatype);
                    return Optional.of(df.getOWLDataPropertyAssertionAxiom(spatialDataProperty, owlNamedIndividual, owlLiteral.get()));
                }
            }
        }

//        Fields
        final Optional<Field> field = Arrays.stream(inputObject.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Spatial.class))
                .findFirst();

        if (field.isPresent()) {

            Object fieldValue = null;
            try {
                fieldValue = field.get().get(inputObject);
            } catch (IllegalAccessException e) {
                logger.warn("Cannot access field {}", field.get().getName(), e);
                return Optional.empty();
            }
            final Optional<OWLLiteral> owlLiteral = parseOWLLiteralFromGeom(fieldValue);
            if (owlLiteral.isPresent()) {
                return Optional.of(df.getOWLDataPropertyAssertionAxiom(spatialDataProperty, owlNamedIndividual, owlLiteral.get()));
            }
        }
        return Optional.empty();
    }

    static String filterMethodName(Method classMethod) {
        String name = classMethod.getName();
//        remove get and lowercase the first letter
        if (name.startsWith("get")) {
            final String firstLetter = name.substring(3, 4).toLowerCase();
            final String restOfLetters = name.substring(4);
            return firstLetter + restOfLetters;
        }

        return name;
    }

    @Override
    public String matchWithClassMember(Class<?> clazz, String classMember, @Nullable String languageTag) {
        if (languageTag == null) {
            return matchWithClassMember(clazz, classMember);
        }

//        See if we can match right off the language
//        Methods
        final Optional<String> annotatedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Fact.class))
                .filter(m -> m.getAnnotation(Fact.class).name().equals(classMember))
                .filter(m -> m.isAnnotationPresent(Language.class))
                .filter(m -> m.getAnnotation(Language.class).language().equalsIgnoreCase(languageTag))
                .map(m -> {
                    try {
                        if (ClassBuilder.isConstructorArgument(clazz,
                                filterMethodName(m),
                                m.getReturnType())) {
                            return filterMethodName(m);
                        }
                    } catch (MissingConstructorException e) {
                        e.printStackTrace();
                    }
                    return m.getAnnotation(Fact.class).name();
                })
//                .map(ClassParser::filterMethodName)
//                .map(m -> m.getAnnotation(Fact.class).name())
                .findAny();

        if (annotatedMethod.isPresent()) {
            return annotatedMethod.get();
        }

//        Fields
        final Optional<String> annotatedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Fact.class))
                .filter(f -> f.getAnnotation(Fact.class).name().equals(classMember))
                .filter(f -> f.isAnnotationPresent(Language.class))
                .filter(f -> f.getAnnotation(Language.class).language().equalsIgnoreCase(languageTag))
                .map(f -> {
                    try {
                        if (ClassBuilder.isConstructorArgument(clazz,
                                f.getName(),
                                f.getType())) {
                            return f.getName();
                        }
                    } catch (MissingConstructorException e) {
                        e.printStackTrace();
                    }
                    return f.getAnnotation(Fact.class).name();
                })
//                .map(Field::getName)
//                .map(f -> f.getAnnotation(Fact.class).name())
                .findAny();

        if (annotatedField.isPresent()) {
            return annotatedField.get();
        }

//        If we can't match on language annotation, try to look for the method/field without a language annotation
        final Optional<String> methodNoLanguage = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Fact.class))
                .filter(m -> m.getAnnotation(Fact.class).name().equals(classMember))
                .filter(m -> !m.isAnnotationPresent(Language.class))
                .map(m -> {
                    try {
                        if (ClassBuilder.isConstructorArgument(clazz,
                                filterMethodName(m),
                                m.getReturnType())) {
                            return filterMethodName(m);
                        }
                    } catch (MissingConstructorException e) {
                        e.printStackTrace();
                    }
                    return m.getAnnotation(Fact.class).name();
                })
//                .map(ClassParser::filterMethodName)
//                .map(m -> m.getAnnotation(Fact.class).name())
                .findAny();

        if (methodNoLanguage.isPresent()) {
            return methodNoLanguage.get();
        }

        final Optional<String> fieldNoLanguage = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Fact.class))
                .filter(f -> f.getAnnotation(Fact.class).name().equals(classMember))
                .filter(f -> !f.isAnnotationPresent(Language.class))
                .map(f -> {
                    try {
                        if (ClassBuilder.isConstructorArgument(clazz,
                                f.getName(),
                                f.getType())) {
                            return f.getName();
                        }
                    } catch (MissingConstructorException e) {
                        e.printStackTrace();
                    }
                    return f.getAnnotation(Fact.class).name();
                })
//                .map(Field::getName)
//                .map(f -> f.getAnnotation(Fact.class).name())
                .findAny();

//        If nothing returns, run the default matcher
        return fieldNoLanguage.orElse(matchWithClassMember(clazz, classMember));
    }

    @Override
    public String matchWithClassMember(Class<?> clazz, String classMember) {
//        Check for a matching field
        Field classField = null;
        try {
            classField = clazz.getDeclaredField(classMember);
        } catch (NoSuchFieldException e) {

        }

//        See if the member directly matches an existing constructor argument
        try {
            if (ClassBuilder.isConstructorArgument(clazz, classMember, null)) {
                return classMember;
            }
        } catch (MissingConstructorException e) {
            throw new RuntimeException("Cannot get constructor", e);
        }

        if (classField == null) {
            final Optional<Field> dataField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Fact.class))
                    .filter(f -> f.getAnnotation(Fact.class).name().equals(classMember))
                    .findFirst();

            if (dataField.isPresent()) {
                return dataField.get().getName();
            }

        } else {
            return classField.getName();
        }

//        Check for a matching method
        final Optional<String> matchingMethod = Arrays.stream(clazz.getDeclaredMethods())
                .map(ClassParser::filterMethodName)
                .filter(name -> name.equals(classMember))
                .findFirst();

        if (matchingMethod.isPresent()) {
            return matchingMethod.get();
        }

        final Optional<Method> annotatedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Fact.class))
                .filter(m -> m.getAnnotation(Fact.class).name().equals(classMember))
                .findFirst();
        if (annotatedMethod.isPresent()) {
            return filterMethodName(annotatedMethod.get());
        }

//        Spatial
        if (classMember.equals("asWKT")) {

////            Check for specified argument name
//            final Optional<Method> spatialMethod = Arrays.stream(clazz.getDeclaredMethods())
//                    .filter(m -> m.isAnnotationPresent(Spatial.class))
////                    .map(m -> m.getAnnotation(Spatial.class).name())
//                    .findFirst();
//
//            if (spatialMethod.isPresent()) {
//                return filterMethodName(spatialMethod.get());
//            }
//
////            if (!spatialMethod.orElse("").equals("")) {
////                return spatialMethod.orElse("");
////            }
//
//            final Optional<Field> spatialField = Arrays.stream(clazz.getDeclaredFields())
//                    .filter(f -> f.isAnnotationPresent(Spatial.class))
////                    .map(f -> f.getAnnotation(Spatial.class).name())
//                    .findFirst();
//
//            if (spatialField.isPresent()) {
//                return spatialField.get().getName();
//            }

//            if (!spatialField.orElse("").equals("")) {
//                return spatialField.orElse("");
//            }

//            TODO(nrobison): I think these things can go away.
            final Optional<Field> spatialField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Spatial.class))
                    .findFirst();

            if (spatialField.isPresent()) {
                return spatialField.get().getName();
            }

            final Optional<Method> spatialMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Spatial.class))
                    .findFirst();

            if (spatialMethod.isPresent()) {
                return filterMethodName(spatialMethod.get());
            }
        }

//        Temporal
//        Default
        if (TemporalParser.isDefault(clazz)) {
            final Optional<Field> temporalField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> (f.isAnnotationPresent(DefaultTemporal.class)))
                    .findFirst();

            if (temporalField.isPresent()) {
//                Check to see if we have a given temporal property
                final String annotationName = temporalField.get().getAnnotation(DefaultTemporal.class).name();
                if (annotationName.equals("")) {
                    return temporalField.get().getName();
                } else {
                    return temporalField.get().getName();
//                    return annotationName;
                }
            }

            final Optional<Method> temporalMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(f -> (f.isAnnotationPresent(DefaultTemporal.class)))
                    .findFirst();


            if (temporalMethod.isPresent()) {
                final String annotationName = temporalMethod.get().getAnnotation(DefaultTemporal.class).name();
                if (annotationName.equals("")) {
                    return filterMethodName(temporalMethod.get());
                } else {
                    return filterMethodName(temporalMethod.get());
//                    return annotationName;
                }
            }
//        TODO(nrobison): This should be better. String matching is nasty.
        } else if (classMember.toLowerCase().contains("start")) {
//            Check for start/end temporal names
            final Optional<Field> temporalField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> (f.isAnnotationPresent(StartTemporal.class)))
                    .findFirst();

            if (temporalField.isPresent()) {
                final String annotationName = temporalField.get().getAnnotation(StartTemporal.class).name();
                if (annotationName.equals("")) {
                    return temporalField.get().getName();
                } else {
                    return temporalField.get().getName();
//                    return annotationName;
                }
            }

            final Optional<Method> temporalMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(f -> (f.isAnnotationPresent(StartTemporal.class)))
                    .findFirst();
            if (temporalMethod.isPresent()) {
                final String annotationName = temporalMethod.get().getAnnotation(StartTemporal.class).name();
                if (annotationName.equals("")) {
                    return filterMethodName(temporalMethod.get());
                } else {
                    return filterMethodName(temporalMethod.get());
//                    return annotationName;
                }
            }

        } else {
            final Optional<Field> temporalField = Arrays.stream(clazz.getDeclaredFields())
                    .filter(f -> (f.isAnnotationPresent(EndTemporal.class)))
                    .findFirst();

            if (temporalField.isPresent()) {
                final String annotationName = temporalField.get().getAnnotation(EndTemporal.class).name();
                if (annotationName.equals("")) {
                    return temporalField.get().getName();
                } else {
                    return temporalField.get().getName();
//                    return annotationName;
                }
            }

            final Optional<Method> temporalMethod = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(f -> (f.isAnnotationPresent(EndTemporal.class)))
                    .findFirst();
            if (temporalMethod.isPresent()) {
                final String annotationName = temporalMethod.get().getAnnotation(EndTemporal.class).name();
                if (annotationName.equals("")) {
                    return filterMethodName(temporalMethod.get());
                } else {
                    return filterMethodName(temporalMethod.get());
//                    return temporalField.get().getName();
//                    return annotationName;
                }
            }
        }

        throw new RuntimeException("Cannot match field or method");
    }

    @Override
    public Optional<Class<@NonNull ?>> getFactDatatype(Class<?> clazz, String factName) {
//        Split String to get the actual fact name
        final String name = IRIUtils.extractTrestleIndividualName(factName);
        final String classMember;
        try {
            classMember = this.matchWithClassMember(clazz, name);
        } catch (RuntimeException e) {
            return Optional.empty();
        }

//        Try for methods first
        final Optional<Method> matchedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> ClassParser.filterFactMethod(method, false))
                .filter(method -> filterMethodName(method).equals(classMember))
                .findFirst();

        if (matchedMethod.isPresent()) {
            return Optional.of(matchedMethod.get().getReturnType());
        }
//        Now the fields
        final Optional<Field> matchedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> ClassParser.filterFactField(field, false))
                .filter(field -> field.getName().equals(classMember))
                .findFirst();

        if (matchedField.isPresent()) {
            return Optional.of(matchedField.get().getType());
        }
        return Optional.empty();
    }

    @Override
    public Optional<IRI> getFactIRI(Class<?> clazz, String factName) {
//        Split String to get the actual fact name
        final String name;
        final String[] splitName = factName.split("#");
        if (splitName.length < 2) {
            name = factName;
        } else {
            name = splitName[1];
        }
        final String classMember;
        try {
            classMember = this.matchWithClassMember(clazz, name);
        } catch (RuntimeException e) {
            return Optional.empty();
        }

//        Try for methods first
        final Optional<IRI> matchedMethod = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> ClassParser.filterFactMethod(method, false))
                .filter(method -> filterMethodName(method).equals(classMember))
                .map(method -> SpatialParser.filterDataSpatialName(method, this.ReasonerPrefix))
                .findFirst();

        if (matchedMethod.isPresent()) {
            return Optional.of(matchedMethod.get());
        }
//        Now the fields
        final Optional<IRI> matchedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> ClassParser.filterFactField(field, false))
                .filter(field -> field.getName().equals(classMember))
                .map(field -> SpatialParser.filterDataSpatialName(field, this.ReasonerPrefix))
                .findFirst();

        if (matchedField.isPresent()) {
            return Optional.of(matchedField.get());
        }
        return Optional.empty();

    }

    static Optional<Object> accessMethodValue(Method classMethod, Object inputObject) {
        @Nullable Object castReturn = null;
        try {
            final Class<?> returnType = TypeConverter.parsePrimitiveClass(classMethod.getReturnType());
            final Object invokedObject;
            invokedObject = classMethod.invoke(inputObject);
            logger.trace("Method {} has return type {}", classMethod.getName(), returnType);
            castReturn = returnType.cast(invokedObject);
        } catch (IllegalAccessException e) {
            logger.warn("Cannot access method {}", classMethod.getName(), e);
        } catch (InvocationTargetException e) {
            logger.error("Invocation failed on method {}", classMethod.getName(), e);
        } catch (ClassCastException e) {
            logger.error("Cannot cast method", e);
        }

        if (castReturn == null) {
            return Optional.empty();
        }

        return Optional.of(castReturn);
    }

    /**
     * Parse the name of a given field and return either the name, or the one declared in the annotation
     *
     * @param field - Field to parse name from
     * @return - String of parsed field name
     */
    static String getFieldName(Field field) {

//        Iterate through the various annotations and figure out if we need to get an annotated values
        if (field.isAnnotationPresent(Fact.class)) {
            final String fieldName = field.getAnnotation(Fact.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(Spatial.class)) {
            final String fieldName = field.getAnnotation(Spatial.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(DefaultTemporal.class)) {
            final String fieldName = field.getAnnotation(DefaultTemporal.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(StartTemporal.class)) {
            final String fieldName = field.getAnnotation(StartTemporal.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else if (field.isAnnotationPresent(EndTemporal.class)) {
            final String fieldName = field.getAnnotation(EndTemporal.class).name();
            if (fieldName.equals("")) {
                return field.getName();
            } else {
                return fieldName;
            }
        } else {
            return field.getName();
        }
    }

    /**
     * Parse the name of a given method and return either the filtered name, or the one declared in the annotation
     *
     * @param method - Method to parse name from
     * @return - String of filtered method name
     */
    static String getMethodName(Method method) {
        //        Iterate through the various annotations and figure out if we need to get an annotated values
        if (method.isAnnotationPresent(Fact.class)) {
            final String methodName = method.getAnnotation(Fact.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(Spatial.class)) {
            final String methodName = method.getAnnotation(Spatial.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(DefaultTemporal.class)) {
            final String methodName = method.getAnnotation(DefaultTemporal.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(StartTemporal.class)) {
            final String methodName = method.getAnnotation(StartTemporal.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else if (method.isAnnotationPresent(EndTemporal.class)) {
            final String methodName = method.getAnnotation(EndTemporal.class).name();
            if (methodName.equals("")) {
                return filterMethodName(method);
            } else {
                return methodName;
            }
        } else {
            return filterMethodName(method);
        }
    }
}

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
    public Integer getClassProjection(Class<?> clazz) {
        return 4326;
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
        Field classField;
        try {
            classField = clazz.getDeclaredField(classMember);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Cannot get class member", e);
        }

//        See if the member directly matches an existing constructor argument
        try {
            if (ClassBuilder.isConstructorArgument(clazz, classMember, null)) {
                return classMember;
            }
        } catch (MissingConstructorException e) {
            throw new IllegalStateException("Cannot get constructor", e);
        }

        return classField.getName();

//        Check for a matching method
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

        return matchedField.map(Field::getType);
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
            return matchedMethod;
        }
//        Now the fields

        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> ClassParser.filterFactField(field, false))
                .filter(field -> field.getName().equals(classMember))
                .map(field -> SpatialParser.filterDataSpatialName(field, this.ReasonerPrefix))
                .findFirst();

    }

    @Override
    public boolean isFactRelated(Class<?> clazz, String factName) {
        return false;
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
}

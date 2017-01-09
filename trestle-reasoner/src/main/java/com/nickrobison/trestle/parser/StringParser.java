package com.nickrobison.trestle.parser;

import com.nickrobison.trestle.annotations.Language;
import com.nickrobison.trestle.annotations.NoMultiLanguage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static com.nickrobison.trestle.parser.ClassParser.dfStatic;

/**
 * Created by nrobison on 12/5/16.
 */
public class StringParser {

    private static final Logger logger = LoggerFactory.getLogger(StringParser.class);

    /**
     * Build and OWLLiteral from a given input string, using the multi-language features
     * If the reasoner is not in multi-lang mode, the language tags are ignored the the string is returned using the XSD:String type
     * If the reasoner is in multi-lang mode, the string is returned using the RDF:PlainLiteral type with the given language tag, or default language tag
     * If no language tags are specified, and XSD:String type literal is returned and a warning is generated
     *
     * @param inputString        - String to build OWLLiteral from
     * @param languageTag        - Optional language tag, provided by the @Language annotation
     * @param multiLangMode      - Boolean whether or not the reasoner is in
     * @param defaultLanguageTag
     * @return
     */
    private static OWLLiteral parseMultiLangString(@NonNull String inputString, @Nullable String languageTag, boolean multiLangMode, @Nullable String defaultLanguageTag) {
//        If not in multi-lang mode, just return the string
        if (!multiLangMode) {
            return dfStatic.getOWLLiteral(inputString);
        }

//        If no language tag specified, try to use the default one, or generate a warning and return the string
        if (languageTag == null) {
            if (defaultLanguageTag == null) {
                logger.warn("No language tags specified, returning with type {}", OWL2Datatype.XSD_STRING);
                return dfStatic.getOWLLiteral(inputString);
            }
            logger.trace("Using default language tag {} for string {}", defaultLanguageTag, inputString);
            return dfStatic.getOWLLiteral(inputString, defaultLanguageTag);
        }

        return dfStatic.getOWLLiteral(inputString, languageTag);
    }

    static Optional<OWLLiteral> fieldValueToMultiLangString(@NonNull Object fieldValue, Field field, boolean multiLangMode, @Nullable String defaultLanguageTag) {
//        If it's not a string, return an empty optional and move on
        if (!(fieldValue instanceof String)) {
            return Optional.empty();
        }

//        If the field has a no-multi lang tag on it, disable the Multilanguage support
        if (field.isAnnotationPresent(NoMultiLanguage.class)) {
            multiLangMode = false;
        }

        @Nullable String languageTag = null;
        if (field.isAnnotationPresent(Language.class)) {
            languageTag = field.getAnnotation(Language.class).language();
        }
        return Optional.of(parseMultiLangString(((String) fieldValue), languageTag, multiLangMode, defaultLanguageTag));
    }

    static Optional<OWLLiteral> methodValueToMultiLangString(@NonNull Object methodValue, Method method, boolean multiLangMode, @Nullable String defaultLanguageTag) {
        if (!(methodValue instanceof String)) {
            return Optional.empty();
        }

//        If the field has a no-multi lang tag on it, disable the Multilanguage support
        if (method.isAnnotationPresent(NoMultiLanguage.class)) {
            multiLangMode = false;
        }

        @Nullable String languageTag = null;
        if (method.isAnnotationPresent(Language.class)) {
            languageTag = method.getAnnotation(Language.class).language();
        }

        return Optional.of(parseMultiLangString(((String) methodValue), languageTag, multiLangMode, defaultLanguageTag));
    }
}

package com.nickrobison.trestle.parser;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nrobison on 11/30/16.
 */
public class TrestleParser {

    private static final Logger logger = LoggerFactory.getLogger(TrestleParser.class);

    private final OWLDataFactory df;
    private final String ReasonerPrefix;
    public final ClassParser classParser;
    public final TemporalParser temporalParser;

    /**
     * Create the Trestle Parser class
     * @param df - OWLDataFactory to use for generating the IRIs
     * @param ReasonerPrefix - Prefix of the Trestle Reasoner
     */
    public TrestleParser(OWLDataFactory df, String ReasonerPrefix, boolean multiLangEnabled, String defaultLanguageCode) {
        this.df = df;
        this.ReasonerPrefix = ReasonerPrefix;

//        Create the sub parsers
        classParser = new ClassParser(df, ReasonerPrefix, multiLangEnabled, defaultLanguageCode);
        this.temporalParser = new TemporalParser(this.classParser);
    }
}

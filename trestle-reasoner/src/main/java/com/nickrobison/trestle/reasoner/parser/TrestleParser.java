package com.nickrobison.trestle.reasoner.parser;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.model.OWLDataFactory;

/**
 * Created by nrobison on 11/30/16.
 */
public class TrestleParser {

    private final OWLDataFactory df;
    private final String ReasonerPrefix;
    public final ClassParser classParser;
    public final TemporalParser temporalParser;

    /**
     * Create the Trestle Parser class
     *
     * @param df                  - {@link OWLDataFactory} to use for generating the IRIs
     * @param ReasonerPrefix      - Prefix of the Trestle Reasoner
     * @param multiLangEnabled    - {@code true} multi-language support is enabled and needs to be handled
     * @param defaultLanguageCode - Default language code to use in the absence of one provided by an {@link org.semanticweb.owlapi.model.OWLLiteral}
     */
    public TrestleParser(OWLDataFactory df, String ReasonerPrefix, boolean multiLangEnabled, String defaultLanguageCode) {

        this.df = df;
        this.ReasonerPrefix = ReasonerPrefix;

//        Create the sub parsers
        classParser = new ClassParser(df, ReasonerPrefix, multiLangEnabled, defaultLanguageCode);
        this.temporalParser = new TemporalParser(this.classParser);
    }
}

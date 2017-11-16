package com.nickrobison.trestle.reasoner.parser;

import org.semanticweb.owlapi.model.OWLDataFactory;

/**
 * Created by nrobison on 11/30/16.
 */
public class TrestleParser {

    private final OWLDataFactory df;
    private final String reasonerPrefix;
    public final IClassParser classParser;
    public final IClassBuilder classBuilder;
    public final TemporalParser temporalParser;

    /**
     * Create the Trestle Parser class
     *  @param df                  - {@link OWLDataFactory} to use for generating the IRIs
     * @param reasonerPrefix      - Prefix of the Trestle Reasoner
     * @param multiLangEnabled    - {@code true} multi-language support is enabled and needs to be handled
     * @param defaultLanguageCode - Default language code to use in the absence of one provided by an {@link org.semanticweb.owlapi.model.OWLLiteral}
     */
    public TrestleParser(OWLDataFactory df, String reasonerPrefix, boolean multiLangEnabled, String defaultLanguageCode) {

        this.df = df;
        this.reasonerPrefix = reasonerPrefix;

//        Create the sub parsers
        classParser = ClojureParserProvider.getParser(df, reasonerPrefix, multiLangEnabled, defaultLanguageCode);
        this.classBuilder = (IClassBuilder) classParser;
        this.temporalParser = new TemporalParser(this.classParser);
    }
}

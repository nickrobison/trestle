package com.nickrobison.trestle.reasoner.parser;

import javax.inject.Inject;

/**
 * Created by nrobison on 11/30/16.
 */
public class TrestleParser {

    public final IClassParser classParser;
    public final IClassBuilder classBuilder;
    public final TemporalParser temporalParser;
    public final IClassRegister classRegistry;
    public final ITypeConverter typeConverter;

    @Inject
    public TrestleParser(IClassParser parser, IClassBuilder builder, IClassRegister registry, ITypeConverter typeConverter) {
//        Create the sub parsers
        this.classParser = parser;
        this.classBuilder = builder;
        this.classRegistry = registry;
        this.temporalParser = new TemporalParser(this.classParser);
        this.typeConverter = typeConverter;
    }
}

package com.nickrobison.trestle.reasoner.parser;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Created by nickrobison on 1/2/18.
 */
public class TrestleParserModule extends PrivateModule {

    private final OWLDataFactory df;
    private final String defaultLanguageCode;
    private final boolean multiLangEnabled;

    public TrestleParserModule(boolean multiLangEnabled, String defaultLanguageCode) {
        this.df = OWLManager.getOWLDataFactory();
        this.defaultLanguageCode = defaultLanguageCode;
        this.multiLangEnabled = multiLangEnabled;
    }

    @Override
    protected void configure() {
//        Bind to the Java parser
        bind(IClassRegister.class)
                .to(ClassRegister.class)
                .in(Singleton.class);
        bind(IClassParser.class)
                .to(ClassParser.class)
                .in(Singleton.class);
        bind(IClassBuilder.class)
                .to(ClassBuilder.class)
                .in(Singleton.class);

        bind(TrestleParser.class).in(Singleton.class);
        expose(TrestleParser.class);
    }

    @Provides
    @Named("default-code")
    public String getDefaultLanguageCode() {
        return this.defaultLanguageCode;
    }

    @Provides
    @Named("multiLang")
    public boolean isMultiLangEnabled() {
        return this.multiLangEnabled;
    }
}

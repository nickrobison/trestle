package com.nickrobison.trestle.reasoner.parser;

import com.google.inject.PrivateModule;
import com.google.inject.name.Names;
import com.nickrobison.trestle.reasoner.parser.clojure.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * Created by nickrobison on 1/2/18.
 */
public class TrestleParserModule extends PrivateModule {

    private static final Logger logger = LoggerFactory.getLogger(TrestleParserModule.class);

    private final String defaultLanguageCode;
    private final Integer defaultProjection;
    private final boolean multiLangEnabled;
    private final boolean useClojure;

    public TrestleParserModule() {
        final Config config = ConfigFactory.load().getConfig("trestle");
        this.useClojure = config.getBoolean("useClojureParser");
        this.defaultLanguageCode = config.getString("defaultLanguage");
        this.defaultProjection = config.getInt("defaultProjection");
        this.multiLangEnabled = config.getBoolean("enableMultiLanguage");
    }

    public TrestleParserModule(boolean multiLangEnabled, String defaultLanguageCode) {
        final Config config = ConfigFactory.load().getConfig("trestle");
        this.useClojure = config.getBoolean("useClojureParser");
        this.defaultLanguageCode = defaultLanguageCode;
        this.defaultProjection = config.getInt("defaultProjection");
        this.multiLangEnabled = multiLangEnabled;
    }

    @Override
    protected void configure() {
//        If we're using the Clojure parser, bind to that provider
        if (useClojure) {
            logger.info("Creating Parser with Clojure backend");
            bind(ITypeConverter.class)
                    .toProvider(ClojureTypeConverterProvider.class)
                    .in(Singleton.class);
            bind(Object.class)
                    .annotatedWith(Names.named("clojureParser"))
                    .toProvider(ClojureProvider.class)
                    .in(Singleton.class);
            bind(IClassParser.class)
                    .toProvider(ClojureClassParserProvider.class);
            bind(IClassRegister.class)
                    .toProvider(ClojureClassRegistryProvider.class);
            bind(IClassBuilder.class)
                    .toProvider(ClojureClassBuilderProvider.class);
            //        Bind to the Java parser
        } else {
            logger.info("Creating Parser with Java backend");
            bind(IClassRegister.class)
                    .to(ClassRegister.class)
                    .in(Singleton.class);
            bind(IClassParser.class)
                    .to(ClassParser.class)
                    .in(Singleton.class);
            bind(IClassBuilder.class)
                    .to(ClassBuilder.class)
                    .in(Singleton.class);
            bind(ITypeConverter.class)
                    .to(TypeConverter.class)
                    .in(Singleton.class);
        }



        bind(Boolean.class)
                .annotatedWith(MultiLangEnabled.class)
                .toInstance(this.multiLangEnabled);
        bind(String.class)
                .annotatedWith(DefaultLanguageCode.class)
                .toInstance(this.defaultLanguageCode);
        bind(Integer.class)
                .annotatedWith(DefaultProjection.class)
                .toInstance(this.defaultProjection);

        bind(TrestleParser.class).in(Singleton.class);
        bind(FactFactory.class).in(Singleton.class);
        expose(TrestleParser.class);
        expose(FactFactory.class);
    }
}

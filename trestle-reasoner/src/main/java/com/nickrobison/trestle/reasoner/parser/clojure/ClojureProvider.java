package com.nickrobison.trestle.reasoner.parser.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.semanticweb.owlapi.apibinding.OWLManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class ClojureProvider implements Provider<Object> {


    private final String reasonerPrefix;
    private final boolean multiLangEnabled;
    private final String defaultLanguageCode;

    @Inject
    ClojureProvider(@Named("reasonerPrefix") String reasonerPrefix,
                    @Named("multiLang") boolean multiLangEnabled,
                    @Named("default-code") String defaultLanguageCode) {

        this.reasonerPrefix = reasonerPrefix;
        this.multiLangEnabled = multiLangEnabled;
        this.defaultLanguageCode = defaultLanguageCode;
    }

    @Override
    public Object get() {
        return ClojureProvider.buildClojureParser(this.reasonerPrefix, this.multiLangEnabled, this.defaultLanguageCode);
    }

    public static Object buildClojureParser(String prefix, boolean multiEnabled, String defaultCode) {
        final IFn require = Clojure.var("clojure.core", "require");

        require.invoke(Clojure.read("com.nickrobison.trestle.reasoner.parser.parser"));
//        final IFn newParserFn = Clojure.var("com.nickrobison.trestle.reasoner.parser", "->ClojureClassParser");
        final IFn newParserFn = Clojure.var("com.nickrobison.trestle.reasoner.parser.parser", "make-parser");

//        require.invoke(Clojure.read("clojure.tools.nrepl.server"));
//        IFn server = Clojure.var("clojure.tools.nrepl.server", "start-server");
//        server.invoke();
        return newParserFn.invoke(OWLManager.getOWLDataFactory(), prefix,
                multiEnabled, defaultCode);
    }
}

package com.nickrobison.trestle.reasoner.parser.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.reasoner.parser.DefaultLanguageCode;
import com.nickrobison.trestle.reasoner.parser.DefaultProjection;
import com.nickrobison.trestle.reasoner.parser.MultiLangEnabled;
import org.semanticweb.owlapi.apibinding.OWLManager;

import javax.inject.Inject;
import javax.inject.Provider;

public class ClojureProvider implements Provider<Object> {


    private final String reasonerPrefix;
    private final boolean multiLangEnabled;
    private final String defaultLanguageCode;
    private final Integer defaultProjection;

    @Inject
    ClojureProvider(@ReasonerPrefix String reasonerPrefix,
                    @MultiLangEnabled boolean multiLangEnabled,
                    @DefaultLanguageCode String defaultLanguageCode,
                    @DefaultProjection Integer defaultProjection) {

        this.reasonerPrefix = reasonerPrefix;
        this.multiLangEnabled = multiLangEnabled;
        this.defaultLanguageCode = defaultLanguageCode;
        this.defaultProjection = defaultProjection;
    }

    @Override
    public Object get() {
        return ClojureProvider.buildClojureParser(this.reasonerPrefix, this.multiLangEnabled,
                this.defaultLanguageCode, this.defaultProjection);
    }

    public static Object buildClojureParser(String prefix, boolean multiEnabled, String defaultCode, Integer defaultProjection) {
        final IFn require = Clojure.var("clojure.core", "require");

        require.invoke(Clojure.read("com.nickrobison.trestle.reasoner.parser.parser"));
        final IFn newParserFn = Clojure.var("com.nickrobison.trestle.reasoner.parser.parser", "make-parser");

        return newParserFn.invoke(OWLManager.getOWLDataFactory(), prefix,
                multiEnabled, defaultCode, defaultProjection);
    }
}

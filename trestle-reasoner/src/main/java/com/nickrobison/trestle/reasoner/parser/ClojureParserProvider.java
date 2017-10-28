package com.nickrobison.trestle.reasoner.parser;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;

public class ClojureParserProvider {


    public static IClassParser getParser(OWLDataFactory df, String prefix, boolean multiLang, String defaultCode) {
        final IFn require = Clojure.var("clojure.core", "require");

        require.invoke(Clojure.read("com.nickrobison.trestle.reasoner.parser.parser"));
//        final IFn newParserFn = Clojure.var("com.nickrobison.trestle.reasoner.parser", "->ClojureClassParser");
        final IFn newParserFn = Clojure.var("com.nickrobison.trestle.reasoner.parser.parser", "make-parser");

//        require.invoke(Clojure.read("clojure.tools.nrepl.server"));
//        IFn server = Clojure.var("clojure.tools.nrepl.server", "start-server");
//        server.invoke();
        return (IClassParser) newParserFn.invoke(df, prefix, multiLang, defaultCode);
    }


    public static IClassParser getParser() {
        return ClojureParserProvider.getParser(OWLManager.getOWLDataFactory(), "http://nickrobison.com/test#", true, "en");
    }
}

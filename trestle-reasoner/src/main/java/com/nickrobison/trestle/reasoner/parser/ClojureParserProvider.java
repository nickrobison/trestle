package com.nickrobison.trestle.reasoner.parser;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.semanticweb.owlapi.apibinding.OWLManager;

public class ClojureParserProvider {


    public static IClassParser getParser() {
        final IFn require = Clojure.var("clojure.core", "require");

        require.invoke(Clojure.read("com.nickrobison.trestle.reasoner.parser"));
        final IFn newParserFn = Clojure.var("com.nickrobison.trestle.reasoner.parser", "->ClojureClassParser");
        return (IClassParser) newParserFn.invoke(OWLManager.getOWLDataFactory(), "http://nickrobison.com/test#", true, "en");
    }
}

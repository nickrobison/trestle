package com.nickrobison.trestle.reasoner.parser.clojure;

import com.nickrobison.trestle.reasoner.parser.IClassParser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Created by nickrobison on 1/2/18.
 */
public class ClojureClassParserProvider implements Provider<IClassParser> {

    private final Object clojureParser;

    @Inject
    ClojureClassParserProvider(@Named("clojureParser") Object parser) {
        this.clojureParser = parser;
    }
    @Override
    @Singleton
    public IClassParser get() {
        return (IClassParser) this.clojureParser;
    }
}

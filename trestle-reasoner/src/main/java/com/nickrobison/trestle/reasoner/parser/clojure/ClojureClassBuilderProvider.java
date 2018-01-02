package com.nickrobison.trestle.reasoner.parser.clojure;

import com.nickrobison.trestle.reasoner.parser.IClassBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Created by nickrobison on 1/2/18.
 */
public class ClojureClassBuilderProvider implements Provider<IClassBuilder> {

    private final Object clojureParser;

    @Inject
    ClojureClassBuilderProvider(@Named("clojureParser") Object parser) {
        this.clojureParser = parser;
    }

    @Override
    public IClassBuilder get() {
        return (IClassBuilder) this.clojureParser;
    }
}

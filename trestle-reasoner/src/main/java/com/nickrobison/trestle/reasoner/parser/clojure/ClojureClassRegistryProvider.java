package com.nickrobison.trestle.reasoner.parser.clojure;

import com.nickrobison.trestle.reasoner.parser.IClassRegister;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Created by nickrobison on 1/2/18.
 */
public class ClojureClassRegistryProvider implements Provider<IClassRegister> {

    private final Object clojureParser;

    @Inject
    ClojureClassRegistryProvider(@Named("clojureParser") Object parser) {
        this.clojureParser = parser;
    }

    @Override
    public IClassRegister get() {
        return (IClassRegister) this.clojureParser;
    }
}

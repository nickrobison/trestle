package com.nickrobison.trestle.reasoner.debug;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by nickrobison on 3/19/18.
 */
public class ClojureREPLProvider implements Provider<IDebugREPL> {

    private static final Logger logger = LoggerFactory.getLogger(ClojureREPLProvider.class);
    private static final String NAMESPACE = "com.nickrobison.trestle.reasoner.debug.repl";
    private final int port;

    @Inject
    ClojureREPLProvider(@REPLPort int port) {
        this.port = port;
    }

    @Override
    public IDebugREPL get() {
        logger.info("Creating Clojure debug REPL on port {}", this.port);
        final IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read(NAMESPACE));
        final IFn clojureREPLFn = Clojure.var(NAMESPACE, "make-clojure-repl");
        return (IDebugREPL) clojureREPLFn.invoke(this.port);
    }
}

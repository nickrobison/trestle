package com.nickrobison.trestle.reasoner.parser.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.nickrobison.trestle.reasoner.parser.ITypeConverter;
import com.nickrobison.trestle.reasoner.parser.TypeUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;

import javax.inject.Provider;

/**
 * Created by nickrobison on 3/5/18.
 */
public class ClojureTypeConverterProvider implements Provider<ITypeConverter> {

    private final OWLDataFactory df;

    ClojureTypeConverterProvider() {
        this.df = OWLManager.getOWLDataFactory();
    }

    @Override
    public ITypeConverter get() {
        return buildClojureTypeConverter(this.df);
    }

    public static ITypeConverter buildClojureTypeConverter(OWLDataFactory df) {
        final IFn require = Clojure.var("clojure.core", "require");

        require.invoke(Clojure.read("com.nickrobison.trestle.reasoner.parser.types.converter"));
        final IFn newConverterFn = Clojure.var("com.nickrobison.trestle.reasoner.parser.types.converter", "make-type-converter");

        return (ITypeConverter) newConverterFn.invoke(df, TypeUtils.buildDatatype2ClassMap(), TypeUtils.buildClassMap());
    }
}

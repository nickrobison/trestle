package com.nickrobison.trestle.reasoner.parser;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public class TrestleParserProvider implements Provider<TrestleParser> {

    private final TrestleParser parser;

    @Inject
    public TrestleParserProvider(@Named("reasonerPrefix") String reasonerPrefix) {
        final Config config = ConfigFactory.load().getConfig("trestle");
        this.parser = new TrestleParser(OWLManager.getOWLDataFactory(), reasonerPrefix, config.getBoolean("enableMultiLanguage"), config.getString("defaultLanguage"));
    }

    @Override
    public TrestleParser get() {
        return this.parser;
    }
}

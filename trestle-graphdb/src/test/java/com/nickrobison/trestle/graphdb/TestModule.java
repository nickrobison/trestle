package com.nickrobison.trestle.graphdb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.ontology.ConnectionProperties;
import com.nickrobison.trestle.ontology.annotations.OntologyName;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.nickrobison.trestle.ontology.OntologyBuilder.createDefaultPrefixManager;
import static com.nickrobison.trestle.ontology.OntologyBuilder.loadOntology;

public class TestModule extends AbstractModule {

    public static final String CONFIG_PATH = "ontology.graphdb";
    @Override
    protected void configure() {
        install(new GraphDBOntologyModule());
    }

    @Provides
    @OntologyName
    public String provideName() {
        return "trestle";
    }

    @Provides
    Config provideConfig() {
        return ConfigFactory.load(ConfigFactory.parseResources("test.configuration.conf")).getConfig("trestle");
    }

    @Provides
    OWLOntology provideOntology(Config config) {
        final InputStream is = provideInputStream(config);
        return loadOntology(Optional.empty(), Optional.of(is));
    }

    @Provides
    ConnectionProperties properties(Config config) {
        final Config path = config.getConfig(CONFIG_PATH);
        return new ConnectionProperties(path.getString("connectionString"), path.getString("username"), path.getString("password"));
    }

    @Provides
    DefaultPrefixManager providePrefixManager() {
        return createDefaultPrefixManager();
    }

    private InputStream provideInputStream(Config config) {
        final IRI iri = IRI.create(config.getString("ontology.location"));
        final InputStream inputStream;
        try {
            if (!iri.isAbsolute()) {
                final Path cwd = Paths.get("");
                final Path absPath = Paths.get(cwd.toAbsolutePath().toString(), config.getString("ontology.location"));
                inputStream = absPath.toUri().toURL().openConnection().getInputStream();
            } else {
                inputStream = iri.toURI().toURL().openConnection().getInputStream();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return inputStream;
    }
}

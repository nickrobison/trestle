package com.nickrobison.trestle.reasoner;

import com.nickrobison.trestle.reasoner.caching.TrestleCache;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by nrobison on 8/25/16.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class TrestleBuilder {

    private static final Logger logger = LoggerFactory.getLogger(TrestleBuilder.class);

    Optional<String> connectionString = Optional.empty();
    String username;
    String password;
    final Set<Class> inputClasses;
    Optional<String> ontologyName = Optional.empty();
    Optional<TrestleCache> sharedCache = Optional.empty();
    Optional<IRI> ontologyIRI = Optional.empty();
    Optional<String> reasonerPrefix = Optional.empty();
    boolean initialize = false;
    boolean caching = true;
    boolean metrics = true;
    boolean tracking = false;
    final TrestlePrefixManager pm;

    /**
     * Builder pattern for Trestle Reasoner
     */
    public TrestleBuilder() {
        this.username = "";
        this.password = "";
        this.inputClasses = new HashSet<>();
        pm = new TrestlePrefixManager();
    }

    /**
     * Builder pattern for Trestle Reasoner
     *
     * @param reasonerPrefix - String of reasoner prefix
     */
    public TrestleBuilder(String reasonerPrefix) {
        this.username = "";
        this.password = "";
        this.inputClasses = new HashSet<>();
        pm = new TrestlePrefixManager(reasonerPrefix);
        this.reasonerPrefix = Optional.of(reasonerPrefix);
    }

    /**
     * Connection parameters for underlying triple store to connect to.
     * Based on the connection string, Trestle will build the correct underlying ontology
     * Without specifying a connection string, Trestle will utilize a local Jena TDB store
     *
     * @param connectionString - jdbc connection string for triple store
     * @param username         - Username of connection
     * @param password         - Password of connection
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withDBConnection(String connectionString, String username, String password) {
        this.connectionString = Optional.of(connectionString);
        this.username = username;
        this.password = password;
        return this;
    }

    /**
     * Setup Trestle with specific ontology
     * If no ontology is specified, it loads the vendored ontology
     *
     * @param iri - {@link IRI} of ontology to load
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withOntology(IRI iri) {
        this.ontologyIRI = Optional.of(iri);
        return this;
    }

    /**
     * Setup Trestle with the specified ontology
     *
     * @param iriString - String location of ontology to load
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withOntology(String iriString) {
        this.ontologyIRI = Optional.of(IRI.create(iriString));
        return this;
    }

    /**
     * Set the prefix of the reasoner.
     * If no prefix is specified, it defaults to using the trestle prefix
     *
     * @param iri - String representing prefix to use for Reasoner
     * @return - {@link TrestleBuilder}
     */
    @SuppressWarnings({"method.invocation.invalid"})
    public TrestleBuilder withPrefix(String iri) {
        this.reasonerPrefix = Optional.of(iri);
        this.pm.setDefaultPrefix(iri);
        return this;
    }

    /**
     * Add ontology prefix and URI string to expand to
     *
     * @param prefix - String of ontology prefix
     * @param uri    - String of URI to expand prefix to
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder addPrefix(String prefix, String uri) {
        this.pm.addPrefix(prefix, uri);
        return this;
    }

    /**
     * A list of initial classes to verify and load into trestle.
     *
     * @param inputClass - Vararg list of classes to load and verify
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withInputClasses(Class... inputClass) {
        this.inputClasses.addAll(Arrays.asList(inputClass));
        return this;
    }

    /**
     * Enable tracking of whether or not two objects have had their spatial and temporal properties computed.
     *
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder trackObjectRelations() {
        this.tracking = true;
        return this;
    }

    /**
     * Disable caching
     *
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withoutCaching() {
        caching = false;
        return this;
    }

    /**
     * Disable runtime metrics
     *
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withoutMetrics() {
        metrics = false;
        return this;
    }

    /**
     * Setup Trestle with a preexisting shared cache
     *
     * @param cache - TrestleCache to use
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withSharedCache(TrestleCache cache) {
        this.sharedCache = Optional.of(cache);
        return this;
    }

    /**
     * Set the ontology name
     *
     * @param name - String of ontology name
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder withName(String name) {
//            FIXME(nrobison): Oracle seems to throw errors when using '-' in the name, so maybe parse that out?p
        this.ontologyName = Optional.of(name);
        return this;
    }

    /**
     * Initialize a new ontology on creation. Will override any existing model
     *
     * @return - {@link TrestleBuilder}
     */
    public TrestleBuilder initialize() {
        this.initialize = true;
        return this;
    }

    /**
     * Build the Trestle Reasoner
     *
     * @return - new TrestleReasoner
     */
    public TrestleReasoner build() {
        try {
            return new TrestleReasonerImpl(this);
        } catch (OWLOntologyCreationException e) {
            logger.error("Cannot build trestle", e);
            throw new RuntimeException("Cannot build trestle", e);
        }
    }
}

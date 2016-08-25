package com.nickrobison.trestle;

import com.nickrobison.trestle.caching.TrestleCache;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.parser.ClassRegister;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

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
    boolean initialize = false;
    boolean caching = true;

    @Deprecated
    public TrestleBuilder(IRI iri) {
        this.username = "";
        this.password = "";
        this.inputClasses = new HashSet<>();
    }

    /**
     * Builder pattern for Trestle Reasoner
     */
    public TrestleBuilder() {
        this.username = "";
        this.password = "";
        this.inputClasses = new HashSet<>();
    }

    /**
     * Connection parameters for underlying triple store to connect to.
     * Based on the connection string, Trestle will build the correct underlying ontology
     * Without specifying a connection string, Trestle will utilize a local Jena TDB store
     *
     * @param connectionString - jdbc connection string for triple store
     * @param username         - Username of connection
     * @param password         - Password of connection
     * @return - TrestleBuilder
     */
    public TrestleBuilder withDBConnection(String connectionString, String username, String password) {
        this.connectionString = Optional.of(connectionString);
        this.username = username;
        this.password = password;
        return this;
    }

    /**
     * A list of initial classes to verify and load into trestle.
     *
     * @param inputClass - Vararg list of classes to load and verify
     * @return - TrestleBuilder
     */
    public TrestleBuilder withInputClasses(Class... inputClass) {
        this.inputClasses.addAll(Arrays.asList(inputClass));
        return this;
    }

    /**
     * Disable caching
     *
     * @return - TrestleBuilder
     */
    public TrestleBuilder withoutCaching() {
        caching = false;
        return this;
    }

    /**
     * Setup trestle with a preexisting shared cache
     *
     * @param cache - TrestleCache to use
     * @return - TrestleBuilder
     */
    public TrestleBuilder withSharedCache(TrestleCache cache) {
        this.sharedCache = Optional.of(cache);
        return this;
    }

    /**
     * Set the ontology name
     *
     * @param name - String of ontology name
     * @return - TrestleBuilder
     */
    public TrestleBuilder withName(String name) {
//            FIXME(nrobison): Oracle seems to throw errors when using '-' in the name, so maybe parse that out?p
        this.ontologyName = Optional.of(name);
        return this;
    }

    /**
     * Initialize a new ontology on creation. Will override any existing model
     *
     * @return - TrestleBuilder
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
            return new TrestleReasoner(this);
        } catch (OWLOntologyCreationException e) {
            logger.error("Cannot build trestle", e);
            throw new RuntimeException("Cannot build trestle", e);
        }
    }
}

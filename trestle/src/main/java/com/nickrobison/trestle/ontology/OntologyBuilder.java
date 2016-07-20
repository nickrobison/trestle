package com.nickrobison.trestle.ontology;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.*;

/**
 * Created by nrobison on 6/21/16.
 */
// FIXME(nrobison): Work to remove this, I feel like my optionals should fix the nullness, right?
@SuppressWarnings("nullness")
public class OntologyBuilder {
    private Optional<IRI> iri = Optional.empty();
    private Optional<String> connectionString = Optional.empty();
    private Optional<String> username = Optional.empty();
    private Optional<String> password = Optional.empty();
    private Optional<String> ontologyName = Optional.empty();
    private Optional<DefaultPrefixManager> pm = Optional.empty();

    public OntologyBuilder() {
    }

    /**
     * Loads an initial base ontology from the given IRI
     * @param iri - IRI of the ontology to load
     * @return OntologyBuilder
     */
    public OntologyBuilder fromIRI(IRI iri) {
        this.iri = Optional.of(iri);
        return this;
    }

    /**
     * Connects to ontology database, if this isn't set, the Builder returns a LocalOntology, otherwise it returns the correct database ontology
     * @param connectionString - Connection string of database to load
     * @param username - Username to connect with
     * @param password - User password
     * @return - OntologyBuilder
     */
    public OntologyBuilder withDBConnection(String connectionString, String username, String password) {
        this.connectionString = Optional.of(connectionString);
        this.username = Optional.of(username);
        this.password = Optional.of(password);
        return this;
    }

    /**
     * Sets the name of the ontology model, if null, parses the IRI to get the base name
     * @param ontologyName - Name of the model
     * @return - OntologyBuilder
     */
    public OntologyBuilder name(String ontologyName) {
        this.ontologyName = Optional.of(ontologyName);
        return this;
    }

    /**
     * Sets a custom prefix manager, otherwise a default one is generated
     * @param pm - DefaultPrefixManger, custom prefix manager
     * @return - OntologyBuilder
     */
    public OntologyBuilder withPrefixManager(DefaultPrefixManager pm) {
        this.pm = Optional.of(pm);
        return this;
    }

    /**
     * Builds and returns the correct ontology (either local or database backed)
     * @return - ITrestleOntology for the correct underlying ontology configuration
     * @throws OWLOntologyCreationException
     */
//    TODO(nrobison): Catch the ontology builder exception and return an empty optional instead
    public Optional<ITrestleOntology> build() throws OWLOntologyCreationException {
        final OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology owlOntology;
        if (this.iri.isPresent()) {
            owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(this.iri.get());
        } else {
            owlOntology = owlOntologyManager.createOntology();
        }

//            If there's a connection string, then we need to return a database Ontology
        if (connectionString.isPresent() && connectionString.get().contains("oracle")) {
            return Optional.of(new OracleOntology(
                    this.ontologyName.orElse(extractNamefromIRI(this.iri.orElse(IRI.create("local_ontology")))),
                    owlOntology,
                    pm.orElse(createDefaultPrefixManager()),
                    classify(owlOntology, new ConsoleProgressMonitor()),
                    connectionString.get(),
                    username.orElse(""),
                    password.orElse("")
            ));
        } else if (connectionString.isPresent() && connectionString.get().contains("postgresql")) {
            return Optional.of(new SesameOntology(
                    this.ontologyName.orElse(extractNamefromIRI(this.iri.orElse(IRI.create("local_ontology")))),
                    owlOntology,
                    pm.orElse(createDefaultPrefixManager()),
                    connectionString.get(),
                    username.orElse(""),
                    password.orElse("")
            ));
        }

        else {
            return Optional.of(new LocalOntology(
                    this.ontologyName.orElse(extractNamefromIRI(this.iri.orElse(IRI.create("local_ontology")))),
                    owlOntology,
                    pm.orElse(createDefaultPrefixManager()),
                    classify(owlOntology, new ConsoleProgressMonitor())
            ));
        }
    }

    private static String extractNamefromIRI(IRI iri) {
        return iri.getShortForm();
    }

    private DefaultPrefixManager createDefaultPrefixManager() {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        pm.setDefaultPrefix("http://nickrobison.com/dissertation/trestle.owl#");
//        TODO(nrobison): This should be broken into its own thing. Maybe a function to add prefixes?
        pm.setPrefix("main_geo:", "http://nickrobison.com/dissertation/main_geo.owl#");
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        pm.setPrefix("owl:", "http://www.w3.org/2002/07/owl#");
//        Jena doesn't use the normal geosparql prefix, so we need to define a separate spatial class
        pm.setPrefix("spatial:", "http://www.jena.apache.org/spatial#");
        pm.setPrefix("geosparql:", "http://www.opengis.net/ont/geosparql#");
        pm.setPrefix("trestle:", "http://nickrobison.com/dissertation/trestle.owl#");

//        Add any defined prefixes
        return pm;
    }

    private static PelletReasoner classify(final OWLOntology ontology, final ReasonerProgressMonitor progressMonitor) {
        final PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology, new SimpleConfiguration(progressMonitor));

        reasoner.precomputeInferences(
                InferenceType.CLASS_ASSERTIONS,
                InferenceType.DATA_PROPERTY_ASSERTIONS,
                InferenceType.DISJOINT_CLASSES,
                InferenceType.SAME_INDIVIDUAL,
                InferenceType.CLASS_HIERARCHY,
                InferenceType.OBJECT_PROPERTY_HIERARCHY,
                InferenceType.OBJECT_PROPERTY_ASSERTIONS,
                InferenceType.DIFFERENT_INDIVIDUALS
        );

        return reasoner;
    }

}

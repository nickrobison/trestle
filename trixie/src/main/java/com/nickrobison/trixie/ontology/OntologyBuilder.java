package com.nickrobison.trixie.ontology;

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

import java.util.Optional;

/**
 * Created by nrobison on 6/21/16.
 */ //    FIXME(nrobison): Remove the duplicates check
@SuppressWarnings("Duplicates")
public class OntologyBuilder {
    private Optional<IRI> iri = Optional.empty();
    private Optional<String> connectionString = Optional.empty();
    private Optional<String> username = Optional.empty();
    private Optional<String> password = Optional.empty();
    private Optional<String> ontologyName = Optional.empty();
    private Optional<DefaultPrefixManager> pm = Optional.empty();

    public OntologyBuilder() {
    }

    public OntologyBuilder fromIRI(IRI iri) {
        this.iri = Optional.of(iri);
        return this;
    }

    public OntologyBuilder withDBConnection(String connectionString, String username, String password) {
        this.connectionString = Optional.of(connectionString);
        this.username = Optional.of(username);
        this.username = Optional.of(password);
        return this;
    }

    public OntologyBuilder name(String ontologyName) {
        this.ontologyName = Optional.of(ontologyName);
        return this;
    }

    public OntologyBuilder withPrefixManager(DefaultPrefixManager pm) {
        this.pm = Optional.of(pm);
        return this;
    }

    public Optional<ITrixieOntology> build() throws OWLOntologyCreationException {
        final OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology owlOntology;
        if (this.iri.isPresent()) {
            owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(this.iri.get());
        } else {
            owlOntology = owlOntologyManager.createOntology();
        }

//            If there's a connection string, then we need to return a database Ontology
        if (connectionString.isPresent()) {
            return Optional.of(new OracleOntology(
                    owlOntology,
                    pm.orElse(createDefaultPrefixManager()),
                    classify(owlOntology, new ConsoleProgressMonitor()),
                    connectionString.get(),
                    username.orElse(""),
                    password.orElse("")
            ));
        } else {
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

    private static DefaultPrefixManager createDefaultPrefixManager() {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        pm.setPrefix("main_geo:", "http://nickrobison.com/dissertation/main_geo.owl#");
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        pm.setPrefix("owl:", "http://www.w3.org/2002/07/owl#");
        pm.setPrefix("spatial:", "http://www.jena.apache.org/spatial#");
        pm.setPrefix("geosparql:", "http://www.opengis.net/ont/geosparql#");
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

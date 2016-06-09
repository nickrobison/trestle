package com.nickrobison.trixie.ontology;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;

import java.util.Optional;

/**
 * Created by nrobison on 5/23/16.
 */
public class Builder {

    private final static Logger logger = LoggerFactory.getLogger(Builder.class);

    private final IRI iri;
    private Optional<String> connectionString = Optional.empty();
    private Optional<String> username = Optional.empty();
    private Optional<String> password = Optional.empty();
//    private Optional<DefaultPrefixManager> pm = Optional.empty();

    public Builder(final IRI iri) {
        this.iri = iri;
    }

    public Builder(final IRI iri, final String connectionString, String username, String password) {
        this.iri = iri;
        this.connectionString = Optional.of(connectionString);
        this.username = Optional.of(username);
        this.password = Optional.of(password);
    }

    public Optional<ITrixieOntology> build() throws OWLOntologyCreationException {
        final OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        final OWLOntology owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(iri);
        return Optional.of(new OracleOntology(
                owlOntology,
                defaultPrefixManager(),
                classify(owlOntology, new ConsoleProgressMonitor()),
                connectionString.orElse(""),
                username.orElse(""),
                password.orElse("")
        ));

    }

    private static DefaultPrefixManager defaultPrefixManager() {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        pm.setPrefix("main_geo:", "http://nickrobison.com/dissertation/main_geo.owl#");
        pm.setPrefix("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        pm.setPrefix("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        pm.setPrefix("owl", "http://www.w3.org/2002/07/owl#");
        return pm;
    }

    private static FaCTPlusPlusReasoner classify(final OWLOntology ontology, final ReasonerProgressMonitor prog) {
        FaCTPlusPlusReasoner reasoner = new FaCTPlusPlusReasoner(
                ontology,
                new SimpleConfiguration(prog),
                BufferingMode.BUFFERING);

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

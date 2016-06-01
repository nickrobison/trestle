package com.nickrobison.trixie.ontology;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasoner;

import java.util.Optional;

/**
 * Created by nrobison on 5/23/16.
 */
public class Builder {

    private final static Logger logger = Logger.getLogger(Builder.class);

    private final IRI iri;
//    private Optional<DefaultPrefixManager> pm = Optional.empty();

    public Builder(final IRI iri) {
        this.iri = iri;
    }

    public Optional<IOntology> build() throws OWLOntologyCreationException {
        final OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        final OWLOntology owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(iri);
        return Optional.of(new Ontology(
                owlOntology,
                defaultPrefixManager(),
                classify(owlOntology, new ConsoleProgressMonitor())
        ));

    }

    private static DefaultPrefixManager defaultPrefixManager() {
        DefaultPrefixManager pm = new DefaultPrefixManager();
        pm.setPrefix("main_geo:", "http://nickrobison.com/dissertation/main_geo.owl#");
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

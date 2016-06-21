package com.nickrobison.trixie;

import com.nickrobison.trixie.ontology.ITrixieOntology;
import com.nickrobison.trixie.ontology.OntologyBuilder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

/**
 * Created by nrobison on 5/17/16.
 */
public class TrixieReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrixieReasoner.class);

    public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException {

        OWLOntology trixieOntology;
        DefaultPrefixManager pm;

//        Try to build the ontology
//        final IRI iri = IRI.create(TrixieReasoner.class.getResourceAsStream("main_geo.owl").toString());
//        final URL resource = TrixieReasoner.getCla.getResource("main_geo.owl");
        logger.debug("Running Trixie");
        final URL resource = TrixieReasoner.class.getClassLoader().getResource("main_geo.owl");
        if (resource == null) {
            logger.error("Can't load resource");
        } else {

            final IRI iri = IRI.create(resource);
            final Optional<ITrixieOntology> ontology = new OntologyBuilder()
                    .fromIRI(iri)
                    .build();
            if (!ontology.isPresent()) {
                logger.error("OracleOntology missing");
            }
            final ITrixieOntology rootOntology = ontology.get();
            trixieOntology = rootOntology.getUnderlyingOntology();
            pm = rootOntology.getUnderlyingPrefixManager();
            final OWLDataFactory df = OWLManager.getOWLDataFactory();
            final IRI geoIRI = IRI.create("main_geo:", "GAULRegion");
            final OWLClass gaulObject = df.getOWLClass(geoIRI.toString(), pm);
            final Set<OWLSubClassOfAxiom> geoObjAxioms = trixieOntology.getSubClassAxiomsForSubClass(gaulObject);

//            Try to add some individuals.
            final IRI burambi_iri = IRI.create("main_geo:", "burambi_1");
            final OWLNamedIndividual burambi_1 = df.getOWLNamedIndividual(burambi_iri.toString(), pm);
            final AddAxiom burambiAxiom = new AddAxiom(trixieOntology, df.getOWLClassAssertionAxiom(gaulObject, burambi_1));
            rootOntology.applyChange(burambiAxiom);

//            Try to read it back?
            final Set<OWLNamedIndividual> gaulInstances = rootOntology.getInstances(gaulObject, true);
            if (!rootOntology.isConsistent()) {
                logger.error("OracleOntology is inconsistent");
            }
//            ontology.get().getUnderlyingOntology().getOWLOntologyManager().saveOntology(trixieOntology);
            rootOntology.writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), false);
//            trixieOntology.saveOntology();

//            Try to load the ontology into Oracle
//            We need an RDF/XML ontology to actually work with Jena(?)
            final URL rdfOntology = TrixieReasoner.class.getClassLoader().getResource("main_geo.rdf");
//            rootOntology.initializeOracleOntology(IRI.create(rdfOntology));

        }

    }
}

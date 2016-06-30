package com.nickrobison.trestle;

import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
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
@SuppressWarnings("dereference.of.nullable")
public class TrestleReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrestleReasoner.class);

    public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException {

        OWLOntology trixieOntology;
        DefaultPrefixManager pm;

//        Try to build the ontology
//        final IRI iri = IRI.create(TrestleReasoner.class.getResourceAsStream("main_geo.owl").toString());
//        final URL resource = TrestleReasoner.getCla.getResource("main_geo.owl");
        logger.debug("Running Trixie");
        final URL resource = TrestleReasoner.class.getClassLoader().getResource("main_geo.owl");
        if (resource == null) {
            logger.error("Can't load resource");
        } else {

            final IRI iri = IRI.create(resource);
            final Optional<ITrestleOntology> ontology = new OntologyBuilder()
                    .fromIRI(iri)
                    .build();
            if (!ontology.isPresent()) {
                logger.error("OracleOntology missing");
            }
            final ITrestleOntology rootOntology = ontology.get();
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
//            final URL rdfOntology = TrestleReasoner.class.getClassLoader().getResource("main_geo.rdf");
//            rootOntology.initializeOracleOntology(IRI.create(rdfOntology));

        }

    }
}

package com.nickrobison.trestle;

import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.parser.ClassParser;
import com.nickrobison.trestle.parser.ClassRegister;
import com.nickrobison.trestle.parser.TemporalParser;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by nrobison on 5/17/16.
 */
public class TrestleReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrestleReasoner.class);

    private final ITrestleOntology ontology;
    private final Set<Class> registeredClasses;
    private final OWLDataFactory df;
    //    Seems gross?
    private static final OWLClass datasetClass = OWLManager.getOWLDataFactory().getOWLClass(IRI.create("trestle:", "dataset"));
    public static final String PREFIX = "trestle:";
    private static final IRI temporalClassIRI = IRI.create(PREFIX, "Temporal_Object");
    private static final IRI temporalValidFromIRI = IRI.create(PREFIX, "valid_from");
    private static final IRI temporalValidToIRI = IRI.create(PREFIX, "valid_to");
    private static final IRI temporalExistsFromIRI = IRI.create(PREFIX, "exists_from");
    private static final IRI temporalExistsToIRI = IRI.create(PREFIX, "exists_to");
    private static final IRI temporalValidAtIRI = IRI.create(PREFIX, "valid_at");
    private static final IRI temporalExistsAtIRI = IRI.create(PREFIX, "exists_at");
    private static final IRI hasTemporalIRI = IRI.create(PREFIX, "has_temporal");
    private static final IRI temporalOfIRI = IRI.create(PREFIX, "temporal_of");
    private static final IRI hasFactIRI = IRI.create(PREFIX, "has_fact");
    private static final IRI factOfIRI = IRI.create(PREFIX, "fact_of");
    private static final IRI temporalDatatypeIRI = IRI.create("http://www.w3.org/2001/XMLSchema#dateTime");

    @SuppressWarnings("dereference.of.nullable")
    private TrestleReasoner(TrestleBuilder builder) throws OWLOntologyCreationException {

        final URL ontologyResource = TrestleReasoner.class.getClassLoader().getResource("trestle.owl");
        logger.info("Loading ontology from {}", ontologyResource);
        if (ontologyResource == null) {
            logger.error("Cannot load trestle ontology from resources");
            throw new RuntimeException("Cannot load ontology");
        }

//        Parse the listed input classes
        this.registeredClasses = builder.inputClasses;

        logger.info("Connecting to ontology {} at {}", builder.ontologyName.orElse("trestle"), builder.connectionString.orElse("localhost"));
        OntologyBuilder ontologyBuilder = new OntologyBuilder()
                .fromIRI(IRI.create(ontologyResource))
                .name(builder.ontologyName.orElse("trestle"));
        if (builder.connectionString.isPresent()) {
            ontologyBuilder = ontologyBuilder.withDBConnection(builder.connectionString.get(),
                    builder.username,
                    builder.password);
        }

        ontology = ontologyBuilder.build().orElseThrow(() -> new RuntimeException("Cannot build ontology"));
        logger.debug("Ontology connected");
        if (builder.initialize) {
            logger.info("Initializing ontology");
            this.ontology.initializeOntology();
        }
        df = OWLManager.getOWLDataFactory();
    }

    /**
     * Shutdown the ontology and potentially delete
     * @param delete - delete the ontology on shutdown?
     */
    public void shutdown(boolean delete) {
        logger.info("Shutting down ontology");
        this.ontology.close(delete);
    }

    public ITrestleOntology getUnderlyingOntology() {
        return this.ontology;
    }

    public Set<OWLNamedIndividual> getInstances(Class inputClass) {
        final OWLClass owlClass = ClassParser.GetObjectClass(inputClass);
        return this.ontology.getInstances(owlClass, true);
    }

    public void writeObjectAsConcept(Object inputObject) throws TrestleClassException {

//        Is class in registry?
        final Class aClass = inputObject.getClass();
        if (!this.registeredClasses.contains(aClass)) {
            throw new UnregisteredClassException(aClass);
        }

//        Write the class
        final OWLClass owlClass = ClassParser.GetObjectClass(inputObject);

        ontology.associateOWLClass(owlClass, datasetClass);
//        Write the individual
        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputObject);
        ontology.createIndividual(owlNamedIndividual, owlClass);
//        Write the temporal
        final Optional<List<TemporalObject>> temporalObjects = TemporalParser.GetTemporalObjects(inputObject);
        if (temporalObjects.isPresent()) {
            temporalObjects.get().forEach(temporal -> {
                try {
                    writeTemporalWithAssociation(temporal, owlNamedIndividual);
                } catch (MissingOntologyEntity e) {
                    logger.error("Individual {} missing in ontology", owlNamedIndividual, e);
                }
            });
        }

//        Write the data properties
        final Optional<List<OWLDataPropertyAssertionAxiom>> dataProperties = ClassParser.GetDataProperties(inputObject);
        if (dataProperties.isPresent()) {
            dataProperties.get().forEach(property -> {
                try {
                    ontology.writeIndividualDataProperty(property);
                } catch (MissingOntologyEntity e) {
                    logger.error("Individual {} missing in ontology", property.getSubject(), e);
                }
            });
        }
//        Write the object properties
    }

    public void registerClass(Class inputClass) {
        ClassRegister.ValidateClass(inputClass);
        this.registeredClasses.add(inputClass);
    }

    private void writeTemporalWithAssociation(TemporalObject temporal, OWLNamedIndividual individual) throws MissingOntologyEntity {
//        Write the object
        final IRI temporalIRI = temporal.getIDAsIRI();
        ontology.createIndividual(temporalIRI, temporalClassIRI);

//        Write the properties
        if (temporal.isInterval()) {
            if (temporal.isValid()) {
//                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        temporalValidFromIRI,
                        temporal.asInterval().getFromTime().toString(),
                        temporalDatatypeIRI);

//                Write to, if exists
                final Optional<LocalDateTime> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            temporalValidToIRI,
                            toTime.get().toString(),
                            temporalDatatypeIRI);
                }
            } else {
                //                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        temporalExistsFromIRI,
                        temporal.asInterval().getFromTime().toString(),
                        temporalDatatypeIRI);

//                Write to, if exists
                final Optional<LocalDateTime> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            temporalExistsToIRI,
                            toTime.get().toString(),
                            temporalDatatypeIRI);
                }
            }
        } else {
//            Is point
            if (temporal.isValid()) {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        temporalValidAtIRI,
                        temporal.asPoint().getPointTime().toString(),
                        temporalDatatypeIRI);
            } else {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        temporalExistsAtIRI,
                        temporal.asPoint().getPointTime().toString(),
                        temporalDatatypeIRI);
            }
        }

//        Associate with individual
        ontology.writeIndividualObjectProperty(
                individual.getIRI(),
                hasTemporalIRI,
                temporalIRI);
    }



    public static class TrestleBuilder {

        private Optional<String> connectionString = Optional.empty();
        private String username;
        private String password;
        private final Set<Class> inputClasses;
        private Optional<String> ontologyName = Optional.empty();
        private boolean initialize = false;

        @Deprecated
        public TrestleBuilder(IRI iri) {
            this.username = "";
            this.password = "";
            this.inputClasses = new HashSet<>();
        }

        public TrestleBuilder() {
            this.username = "";
            this.password = "";
            this.inputClasses = new HashSet<>();
        }

        public TrestleBuilder withDBConnection(String connectionString, String username, String password) {
            this.connectionString = Optional.of(connectionString);
            this.username = username;
            this.password = password;
            return this;
        }

        public TrestleBuilder withInputClasses(Class... inputClass) {
            this.inputClasses.addAll(Arrays.asList(inputClass));
//            validate the classes
            this.inputClasses.forEach(ClassRegister::ValidateClass);
            return this;
        }

        public TrestleBuilder withName(String name) {
            this.ontologyName = Optional.of(name);
            return this;
        }

        public TrestleBuilder initialize() {
            this.initialize = true;
            return this;
        }

        public TrestleReasoner build() {
            try {
                return new TrestleReasoner(this);
            } catch (OWLOntologyCreationException e) {
                logger.error("Cannot build trestle", e);
                throw new RuntimeException("Cannot build trestle", e);
            }
        }
    }


//    public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException {
//
//        OWLOntology trixieOntology;
//        DefaultPrefixManager pm;
//
////        Try to build the ontology
////        final IRI iri = IRI.create(TrestleReasoner.class.getResourceAsStream("main_geo.owl").toString());
////        final URL resource = TrestleReasoner.getCla.getResource("main_geo.owl");
//        logger.debug("Running Trixie");
//        final URL resource = TrestleReasoner.class.getClassLoader().getResource("main_geo.owl");
//        if (resource == null) {
//            logger.error("Can't load resource");
//        } else {
//
//            final IRI iri = IRI.create(resource);
//            final Optional<ITrestleOntology> ontology = new OntologyBuilder()
//                    .fromIRI(iri)
//                    .build();
//            if (!ontology.isPresent()) {
//                logger.error("OracleOntology missing");
//            }
//            final ITrestleOntology rootOntology = ontology.get();
//            trixieOntology = rootOntology.getUnderlyingOntology();
//            pm = rootOntology.getUnderlyingPrefixManager();
//            final OWLDataFactory df = OWLManager.getOWLDataFactory();
//            final IRI geoIRI = IRI.create("main_geo:", "GAULRegion");
//            final OWLClass gaulObject = df.getOWLClass(geoIRI.toString(), pm);
//            final Set<OWLSubClassOfAxiom> geoObjAxioms = trixieOntology.getSubClassAxiomsForSubClass(gaulObject);
//
////            Try to add some individuals.
//            final IRI burambi_iri = IRI.create("main_geo:", "burambi_1");
//            final OWLNamedIndividual burambi_1 = df.getOWLNamedIndividual(burambi_iri.toString(), pm);
//            final AddAxiom burambiAxiom = new AddAxiom(trixieOntology, df.getOWLClassAssertionAxiom(gaulObject, burambi_1));
//            rootOntology.applyChange(burambiAxiom);
//
////            Try to read it back?
//            final Set<OWLNamedIndividual> gaulInstances = rootOntology.getInstances(gaulObject, true);
//            if (!rootOntology.isConsistent()) {
//                logger.error("OracleOntology is inconsistent");
//            }
////            ontology.get().getUnderlyingOntology().getOWLOntologyManager().saveOntology(trixieOntology);
//            rootOntology.writeOntology(IRI.create(new File("/Users/nrobison/Desktop/test.owl")), false);
////            trixieOntology.saveOntology();
//
////            Try to load the ontology into Oracle
////            We need an RDF/XML ontology to actually work with Jena(?)
////            final URL rdfOntology = TrestleReasoner.class.getClassLoader().getResource("main_geo.rdf");
////            rootOntology.initializeOracleOntology(IRI.create(rdfOntology));
//
//        }
}

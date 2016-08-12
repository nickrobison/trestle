package com.nickrobison.trestle;

import com.nickrobison.trestle.common.StaticIRI;
import com.nickrobison.trestle.exceptions.MissingOntologyEntity;
import com.nickrobison.trestle.exceptions.TrestleClassException;
import com.nickrobison.trestle.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.OntologyBuilder;
import com.nickrobison.trestle.parser.*;
import com.nickrobison.trestle.types.TemporalScope;
import com.nickrobison.trestle.types.TemporalType;
import com.nickrobison.trestle.types.temporal.IntervalTemporal;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.*;

import static com.nickrobison.trestle.common.StaticIRI.*;

/**
 * Created by nrobison on 5/17/16.
 */
public class TrestleReasoner {

    private static final Logger logger = LoggerFactory.getLogger(TrestleReasoner.class);

    private final ITrestleOntology ontology;
    private final Set<Class> registeredClasses;
    private final OWLDataFactory df;
    //    Seems gross?
    private static final OWLClass datasetClass = OWLManager.getOWLDataFactory().getOWLClass(IRI.create("trestle:", "Dataset"));

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
     *
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

    public void writeOntology(URI filePath, boolean validate) {
        try {
            ontology.writeOntology(IRI.create(filePath), validate);
        } catch (OWLOntologyStorageException e) {
            logger.error("Could not write ontology to {}", filePath, e);
        }
    }

    /**
     * Write a java object as a TS_Concept
     *
     * @param inputObject
     * @throws TrestleClassException
     */
    public void writeObjectAsConcept(Object inputObject) throws TrestleClassException {

        writeObject(inputObject, TemporalScope.EXISTS);
    }

    /**
     * Write a java object as an TS_Fact
     *
     * @param inputObject
     * @throws TrestleClassException
     */
    public void writeObjectAsFact(Object inputObject) throws TrestleClassException {
        writeObject(inputObject, TemporalScope.VALID);
    }

    void writeObject(Object inputObject, TemporalScope scope) throws TrestleClassException {

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
                    writeTemporalWithAssociation(temporal, owlNamedIndividual, scope);
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

    public void writeFactWithRelation(Object inputFact, double relation, Object relatedFact) {

//        Check to see if both objects exist
        final OWLNamedIndividual owlNamedIndividual = ClassParser.GetIndividual(inputFact);
        final OWLNamedIndividual relatedFactIndividual = ClassParser.GetIndividual(relatedFact);
        if (!ontology.containsResource(owlNamedIndividual)) {
            logger.debug("Fact {] doesn't exist, adding", owlNamedIndividual);
            try {
                writeObjectAsFact(inputFact);
            } catch (TrestleClassException e) {
                logger.error("Could not write object {}", owlNamedIndividual, e);
            }
        } else {
            logger.debug("Fact {} already exists.", owlNamedIndividual);
        }

        if (!ontology.containsResource(relatedFactIndividual)) {
            logger.debug("Related Fact {} doesn't exist, adding", relatedFactIndividual);
            try {
                writeObjectAsFact(relatedFact);
            } catch (TrestleClassException e) {
                logger.error("Could not write object {}", relatedFactIndividual, e);
            }
        } else {
            logger.debug("Fact {} exists", relatedFactIndividual);
        }

//        See if they're already related
        if (checkObjectRelation(owlNamedIndividual, relatedFactIndividual)) {
//            If they are, move on. We don't support updates, yet.
            logger.info("{} and {} are already related, skipping.", owlNamedIndividual, relatedFactIndividual);
            return;
        }

//        If not, store them.
//        Write the concept relation
        final IRI conceptIRI = IRI.create(PREFIX,
                String.format("related-%s-%s",
                        owlNamedIndividual.getIRI().getShortForm(),
                        relatedFactIndividual.getIRI().getShortForm()));
        ontology.createIndividual(conceptIRI, conceptRelationIRI);

//        Write the relation strength
        try {
            ontology.writeIndividualDataProperty(conceptIRI, relationStrengthIRI, Double.toString(relation), OWL2Datatype.XSD_DOUBLE.getIRI());
        } catch (MissingOntologyEntity e) {
            logger.error("Cannot write property {} in individual {}", relationStrengthIRI, conceptIRI, e);
        }

//        Write the relationOf property
        try {
            ontology.writeIndividualObjectProperty(conceptIRI, relationOfIRI, owlNamedIndividual.getIRI());
        } catch (MissingOntologyEntity e) {
            logger.error("Missing individual {}", conceptIRI, e);
        }

//        Write the hasRelation property
        try {
            ontology.writeIndividualObjectProperty(conceptIRI, hasRelationIRI, relatedFactIndividual.getIRI());
        } catch (MissingOntologyEntity e) {
            logger.error("Missing individual {}", conceptIRI, e);
        }


    }

    @SuppressWarnings("argument.type.incompatible")
    public <T> T readAsObject(Class<T> clazz, String objectID) throws TrestleClassException, MissingOntologyEntity {
        return readAsObject(clazz, IRI.create(PREFIX, objectID.replaceAll("\\s+", "_")));
    }

    //    FIXME(nrobison): Get rid of this warning, not sure why it exists
    @SuppressWarnings("argument.type.incompatible")
    public <T> T readAsObject(Class<T> clazz, IRI individualIRI) throws TrestleClassException, MissingOntologyEntity {
//        Contains class?
        if (!this.registeredClasses.contains(clazz)) {
            throw new UnregisteredClassException(clazz);
        }

//        Figure out its name
        if (!ontology.containsResource(individualIRI)) {
            throw new MissingOntologyEntity("Can't find individual ", individualIRI);
        }


        final ConstructorArguments constructorArguments = new ConstructorArguments();
        final Optional<List<OWLDataProperty>> dataProperties = ClassBuilder.getPropertyMembers(clazz);
        if (dataProperties.isPresent()) {
            final Set<OWLDataPropertyAssertionAxiom> propertiesForIndividual = ontology.getDataPropertiesForIndividual(individualIRI, dataProperties.get());
            propertiesForIndividual.forEach(property -> {
                final Class<?> javaClass = ClassBuilder.lookupJavaClassFromOWLDatatype(property, clazz);
                final Object literalValue = ClassBuilder.extractOWLLiteral(javaClass, property.getObject());
                constructorArguments.addArgument(
                        ClassParser.matchWithClassMember(clazz, property.getProperty().asOWLDataProperty().getIRI().getShortForm()),
                        javaClass,
                        literalValue);
            });
//            Get the temporals
//            Get the temporal objects to figure out the correct return type
            final Optional<List<TemporalObject>> temporalObjectTypes = TemporalParser.GetTemporalObjects(clazz);

            final Class<? extends Temporal> baseTemporalType = TemporalParser.GetTemporalType(clazz);

            final Optional<Set<OWLObjectPropertyAssertionAxiom>> individualObjectProperty = ontology.getIndividualObjectProperty(individualIRI, hasTemporalIRI);
            Optional<TemporalObject> temporalObject = Optional.empty();
            if (individualObjectProperty.isPresent()) {
//                There can only be 1 temporal, so just grab the first one.
                final Optional<OWLObjectPropertyAssertionAxiom> first = individualObjectProperty.get().stream().findFirst();
                if (!first.isPresent()) {
                    throw new RuntimeException(String.format("Missing temporal for individual %s", individualIRI));
                }
                final Set<OWLDataPropertyAssertionAxiom> TemporalProperties = ontology.getAllDataPropertiesForIndividual(first.get().getObject().asOWLNamedIndividual());
                temporalObject = TemporalObjectBuilder.buildTemporalFromProperties(TemporalProperties, TemporalParser.IsDefault(clazz), baseTemporalType);
            }

            if (!temporalObject.isPresent()) {
                throw new RuntimeException("Cannot restore temporal from ontology");
            }

//            Add the temporal to the constructor args
            final TemporalObject temporal = temporalObject.get();
            if (temporal.isInterval()) {
                final IntervalTemporal intervalTemporal = temporal.asInterval();
                constructorArguments.addArgument(
                        ClassParser.matchWithClassMember(clazz, intervalTemporal.getStartName()),
                        intervalTemporal.getBaseTemporalType(),
                        intervalTemporal.getFromTime());
                if (!intervalTemporal.isDefault() & intervalTemporal.getToTime().isPresent()) {
                    constructorArguments.addArgument(
                            ClassParser.matchWithClassMember(clazz, intervalTemporal.getEndName()),
                            intervalTemporal.getBaseTemporalType(),
                            intervalTemporal.getToTime().get());
                }
            } else {
                constructorArguments.addArgument(
                        ClassParser.matchWithClassMember(clazz, temporal.asPoint().getParameterName()),
                        temporal.asPoint().getBaseTemporalType(),
                        temporal.asPoint().getPointTime());
            }
        }
        return ClassBuilder.ConstructObject(clazz, constructorArguments);
    }

    public void registerClass(Class inputClass) throws TrestleClassException {
        ClassRegister.ValidateClass(inputClass);
        this.registeredClasses.add(inputClass);
    }

    private void writeTemporalWithAssociation(TemporalObject temporal, OWLNamedIndividual individual, @Nullable TemporalScope overrideTemporalScope) throws MissingOntologyEntity {
//        Write the object
        final IRI temporalIRI = temporal.getIDAsIRI();
        ontology.createIndividual(temporalIRI, StaticIRI.temporalClassIRI);
        TemporalScope scope = temporal.getScope();
        TemporalType type = temporal.getType();

        if (overrideTemporalScope != null) {
            scope = overrideTemporalScope;
        }

//        Write the properties using the scope and type variables set above
        if (type == TemporalType.INTERVAL) {
            if (scope == TemporalScope.VALID) {
//                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalValidFromIRI,
                        temporal.asInterval().getFromTime().toString(),
                        StaticIRI.temporalDatatypeIRI);

//                Write to, if exists
                final Optional<LocalDateTime> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            StaticIRI.temporalValidToIRI,
                            toTime.get().toString(),
                            StaticIRI.temporalDatatypeIRI);
                }
            } else {
                //                Write from
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsFromIRI,
                        temporal.asInterval().getFromTime().toString(),
                        StaticIRI.temporalDatatypeIRI);

//                Write to, if exists
                final Optional<LocalDateTime> toTime = temporal.asInterval().getToTime();
                if (toTime.isPresent()) {
                    ontology.writeIndividualDataProperty(
                            temporalIRI,
                            StaticIRI.temporalExistsToIRI,
                            toTime.get().toString(),
                            StaticIRI.temporalDatatypeIRI);
                }
            }
        } else {
//            Is point
            if (scope == TemporalScope.VALID) {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalValidAtIRI,
                        temporal.asPoint().getPointTime().toString(),
                        StaticIRI.temporalDatatypeIRI);
            } else {
                ontology.writeIndividualDataProperty(
                        temporalIRI,
                        StaticIRI.temporalExistsAtIRI,
                        temporal.asPoint().getPointTime().toString(),
                        StaticIRI.temporalDatatypeIRI);
            }
        }

//        Associate with individual
        ontology.writeIndividualObjectProperty(
                individual.getIRI(),
                StaticIRI.hasTemporalIRI,
                temporalIRI);
    }

    private boolean checkObjectRelation(OWLNamedIndividual firstIndividual, OWLNamedIndividual secondIndividual) {

//        This should get all the Concept Relations and the individuals related to the first individual, and it's symmetric
        final Optional<Set<OWLObjectPropertyAssertionAxiom>> relatedToProperties = ontology.getIndividualObjectProperty(firstIndividual, hasRelationIRI);
        if (relatedToProperties.isPresent()) {
            final Optional<OWLIndividual> isRelated = relatedToProperties.get()
                    .stream()
                    .map(OWLPropertyAssertionAxiom::getObject)
                    .filter(p -> p.equals(secondIndividual))
                    .findFirst();

            if (isRelated.isPresent()) {
                return true;
            }
        }

        return false;
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
//            this.inputClasses.addAll(Arrays.asList(inputClass));
////            validate the classes
            Arrays.stream(inputClass)
                    .forEach(clazz -> {
                        try {
                            ClassRegister.ValidateClass(clazz);
                            this.inputClasses.add(clazz);
                        } catch (TrestleClassException e) {
                            logger.error("Cannot validate class {}", clazz, e);
                        }
                    });
            return this;
        }

        public TrestleBuilder withName(String name) {
//            FIXME(nrobison): Oracle seems to throw errors when using '-' in the name, so maybe parse that out?p
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
}

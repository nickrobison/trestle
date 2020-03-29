package com.nickrobison.trestle.reasoner.engines.object;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.ontology.ReasonerPrefix;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.reasoner.parser.IClassRegister;
import com.nickrobison.trestle.reasoner.parser.TrestleParser;
import com.nickrobison.trestle.transactions.TrestleTransaction;
import com.nickrobison.trestle.types.temporal.TemporalObject;
import com.nickrobison.trestle.types.temporal.TemporalObjectBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.Set;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;

/**
 * Created by nickrobison on 2/13/18.
 */
@Metriced
@Singleton
public class ObjectEngineUtils {

    private static final Logger logger = LoggerFactory.getLogger(ObjectEngineUtils.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();
    private static final String BLANK_TEMPORAL_ID = "blank";

    private final IClassRegister registry;
    private final ITrestleOntology ontology;
    private final String reasonerPrefix;

    @Inject
    public ObjectEngineUtils(TrestleParser trestleParser,
                             ITrestleOntology ontology,
                             @ReasonerPrefix String reasonerPrefix) {
        this.registry = trestleParser.classRegistry;
        this.ontology = ontology;
        this.reasonerPrefix = reasonerPrefix;
    }

    /**
     * Get the adjusted {@link Temporal} for a given individual
     * If temporal occurs AFTER the existence interval of the object, then we retrieve the LATEST state of the object
     * If it occurs BEFORE, we return the earliest state of the object
     *
     * @param individual         - {@link String} individual ID
     * @param atTemporal         - {@link Temporal} temporal to adjust to
     * @param trestleTransaction - {@link Nullable} {@link TrestleTransaction}
     * @return - {@link Temporal}
     */
    @SuppressWarnings({"squid:S3655"})
    public Temporal getAdjustedQueryTemporal(String individual, OffsetDateTime atTemporal, @Nullable TrestleTransaction trestleTransaction) {
        final TrestleTransaction tt = this.ontology.createandOpenNewTransaction(trestleTransaction);
        try {
            final Set<OWLDataPropertyAssertionAxiom> temporalsForIndividual = this.ontology.getTemporalsForIndividual(df.getOWLNamedIndividual(IRI.create(individual)));
            final Optional<TemporalObject> individualExistsTemporal = TemporalObjectBuilder.buildTemporalFromProperties(temporalsForIndividual, null, BLANK_TEMPORAL_ID);
            final TemporalObject temporalObject = individualExistsTemporal.orElseThrow(() -> new RuntimeException(String.format("Unable to get exists temporals for %s", individual)));
            final int compared = temporalObject.compareTo(atTemporal);
            final Temporal adjustedIntersection;
            if (compared == -1) { // Intersection is after object existence, get the latest version
                if (temporalObject.isInterval()) {
//                            we need to do a minus one precision unit, because the intervals are exclusive on the end {[)}
                    adjustedIntersection = (Temporal) temporalObject.asInterval().getAdjustedToTime(-1).get();
                } else {
                    adjustedIntersection = temporalObject.asPoint().getPointTime();
                }
            } else if (compared == 0) { // Intersection is during existence, continue
                adjustedIntersection = atTemporal;
            } else { // Intersection is before object existence, get earliest version
                adjustedIntersection = temporalObject.getIdTemporal();
            }
            return adjustedIntersection;
        } finally {
            this.ontology.returnAndCommitTransaction(tt);
        }
    }

    /**
     * Get the registered {@link Class} by the String id of the {@link OWLClass}
     *
     * @param datasetClassID - {@link String} ID of dataset
     * @return - {@link Class} registered with reasoner
     */
    public Class<?> getRegisteredClass(String datasetClassID) {
        //        Lookup class
        final OWLClass individualClass = df.getOWLClass(parseStringToIRI(reasonerPrefix, datasetClassID));
        try {
            return this.registry.lookupClass(individualClass);
        } catch (UnregisteredClassException e) {
            throw new IllegalArgumentException(String.format("Cannot find matching class for: %s", individualClass));
        }
    }

    /**
     * Determine if a given individual exists in the {@link com.nickrobison.trestle.ontology.ITrestleOntology}
     *
     * @param individualIRI - {@link IRI} resource to check for
     * @return - {@code true} individual exists in ontology. {@code false} individual does not exist
     */
    @Timed
    public boolean checkExists(IRI individualIRI) {
        return ontology.containsResource(individualIRI);
    }


    /**
     * Determines if a given Java class is registered with the reasoner
     *
     * @param clazz - {@link Class} class to check
     * @return - {@code true} class is registered, {@code false} class is not registered
     */
    boolean checkRegisteredClass(Class<?> clazz) {
        return this.registry.isRegistered(clazz);
    }
}

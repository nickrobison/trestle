package com.nickrobison.trestle.reasoner.engines.object;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.ontology.ITrestleOntology;
import com.nickrobison.trestle.reasoner.annotations.metrics.Metriced;
import com.nickrobison.trestle.reasoner.exceptions.UnregisteredClassException;
import com.nickrobison.trestle.reasoner.parser.IClassRegister;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import static com.nickrobison.trestle.common.IRIUtils.parseStringToIRI;

/**
 * Created by nickrobison on 2/13/18.
 */
@Metriced
public class ObjectEngineUtils {

    private static final Logger logger = LoggerFactory.getLogger(ObjectEngineUtils.class);
    private static final OWLDataFactory df = OWLManager.getOWLDataFactory();

    private final IClassRegister registry;
    private final ITrestleOntology ontology;
    private final String reasonerPrefix;

    @Inject
    public ObjectEngineUtils(IClassRegister registry, ITrestleOntology ontology, @Named("reasonerPrefix") String reasonerPrefix) {
        this.registry = registry;
        this.ontology = ontology;
        this.reasonerPrefix = reasonerPrefix;
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

    /**
     * Get the registered {@link Class} by the String id of the {@link OWLClass}
     *
     * @param datasetClassID - {@link String} ID of dataset
     * @return - {@link Class} registered with reasoner
     */
    Class<?> getRegisteredClass(String datasetClassID) {
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
     * @param individualIRI - {@link IRI} resource to check for
     * @return - {@code true} individual exists in ontology. {@code false} individual does not exist
     */
    @Timed
    boolean checkExists(IRI individualIRI) {
        return ontology.containsResource(individualIRI);
    }
}

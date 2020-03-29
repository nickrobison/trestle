package com.nickrobison.trestle.reasoner.engines.relations;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nickrobison on 7/21/18.
 */
public class RelationTrackerImpl implements RelationTracker {

    private final static Logger logger = LoggerFactory.getLogger(RelationTrackerImpl.class);

    private final Set<Integer> cache;

    RelationTrackerImpl() {
        this.cache = ConcurrentHashMap.newKeySet();
    }

    @Override
    public boolean hasRelation(OWLNamedIndividual subject, OWLNamedIndividual object) {
        return hasRelation(subject.getIRI(), object.getIRI());
    }

    @Override
    public boolean hasRelation(IRI subject, IRI object) {
        final int hash = subject.hashCode() ^ object.hashCode();

        return this.cache.contains(hash);
    }

    @Override
    public void addRelation(OWLNamedIndividual subject, OWLNamedIndividual object) {
        addRelation(subject.getIRI(), object.getIRI());
    }

    @Override
    public void addRelation(IRI subject, IRI object) {
        final int hash = subject.hashCode() ^ object.hashCode();

        this.cache.add(hash);
    }

    @Override
    public void removeComputedRelations(OWLNamedIndividual object) {
        logger.warn("Removing relations is not implemented yet");
    }

    @Override
    public void removeComputedRelations(IRI object) {
        logger.warn("Removing relations is not implemented yet");
    }
}

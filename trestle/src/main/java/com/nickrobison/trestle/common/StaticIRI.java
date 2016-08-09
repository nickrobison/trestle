package com.nickrobison.trestle.common;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * Created by nrobison on 8/1/16.
 */
public class StaticIRI {
    public static final String PREFIX = "http://nickrobison.com/dissertation/trestle.owl#";
    public static final IRI factOfIRI = IRI.create(PREFIX, "fact_of");
    public static final IRI hasFactIRI = IRI.create(PREFIX, "has_fact");
    public static final IRI temporalOfIRI = IRI.create(PREFIX, "temporal_of");
    public static final IRI hasTemporalIRI = IRI.create(PREFIX, "has_temporal");
    public static final IRI temporalExistsAtIRI = IRI.create(PREFIX, "exists_at");
    public static final IRI temporalValidAtIRI = IRI.create(PREFIX, "valid_at");
    public static final IRI temporalExistsToIRI = IRI.create(PREFIX, "exists_to");
    public static final IRI temporalExistsFromIRI = IRI.create(PREFIX, "exists_from");
    public static final IRI temporalValidToIRI = IRI.create(PREFIX, "valid_to");
    public static final IRI temporalValidFromIRI = IRI.create(PREFIX, "valid_from");
    public static final IRI temporalClassIRI = IRI.create(PREFIX, "Temporal_Object");
    public static final IRI relatedToIRI = IRI.create(PREFIX, "related_to");
    public static final IRI relationOfIRI = IRI.create(PREFIX, "relation_of");
    public static final IRI temporalDatatypeIRI = OWL2Datatype.XSD_DATE_TIME.getIRI();

}

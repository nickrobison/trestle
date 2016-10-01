package com.nickrobison.trestle.common;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * Created by nrobison on 8/1/16.
 */
public class StaticIRI {
    public static final String PREFIX = "http://nickrobison.com/dissertation/trestle.owl#";
    public static final String GEOSPARQLPREFIX = "http://www.opengis.net/ont/geosparql#";
    public static final String XSDPREFIX = "http://www.w3.org/2001/XMLSchema#";
//    Relations
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
    public static final IRI validTimeIRI = IRI.create(PREFIX, "valid_time");
    public static final IRI validTimeOfIRI = IRI.create(PREFIX, "valid_time_of");
    public static final IRI hasRelationIRI = IRI.create(PREFIX, "has_relation");
    public static final IRI relationOfIRI = IRI.create(PREFIX, "relation_of");
//    Classes
    public static final IRI conceptRelationIRI = IRI.create(PREFIX, "Concept_Relation");
    public static final IRI datasetClassIRI = IRI.create(PREFIX, "Dataset");
    public static final IRI factClassIRI = IRI.create(PREFIX, "Fact");
    public static final IRI temporalClassIRI = IRI.create(PREFIX, "Temporal_Object");
//    Properties
    public static final IRI relationStrengthIRI = IRI.create(PREFIX, "Relation_Strength");
    public static final IRI dateTimeDatatypeIRI = IRI.create(XSDPREFIX, "dateTime");
    public static final IRI dateDatatypeIRI = IRI.create(XSDPREFIX, "date");
    public static final IRI UUIDDatatypeIRI = IRI.create(PREFIX, "UUID");
    public static final IRI WKTDatatypeIRI = IRI.create(GEOSPARQLPREFIX, "wktLiteral");

}

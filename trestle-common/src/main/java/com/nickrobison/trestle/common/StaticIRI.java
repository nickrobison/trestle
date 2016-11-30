package com.nickrobison.trestle.common;

import org.semanticweb.owlapi.model.IRI;

/**
 * Created by nrobison on 8/1/16.
 */
public class StaticIRI {
    public static final String TRESTLE_PREFIX = "http://nickrobison.com/dissertation/trestle.owl#";
    public static final String GEOSPARQLPREFIX = "http://www.opengis.net/ont/geosparql#";
    public static final String XSDPREFIX = "http://www.w3.org/2001/XMLSchema#";
//    Relations
    public static final IRI factOfIRI = IRI.create(TRESTLE_PREFIX, "fact_of");
    public static final IRI hasFactIRI = IRI.create(TRESTLE_PREFIX, "has_fact");
    public static final IRI temporalOfIRI = IRI.create(TRESTLE_PREFIX, "temporal_of");
    public static final IRI hasTemporalIRI = IRI.create(TRESTLE_PREFIX, "has_temporal");
    public static final IRI temporalExistsAtIRI = IRI.create(TRESTLE_PREFIX, "exists_at");
    public static final IRI temporalValidAtIRI = IRI.create(TRESTLE_PREFIX, "valid_at");
    public static final IRI temporalExistsToIRI = IRI.create(TRESTLE_PREFIX, "exists_to");
    public static final IRI temporalExistsFromIRI = IRI.create(TRESTLE_PREFIX, "exists_from");
    public static final IRI temporalValidToIRI = IRI.create(TRESTLE_PREFIX, "valid_to");
    public static final IRI temporalValidFromIRI = IRI.create(TRESTLE_PREFIX, "valid_from");
    public static final IRI validTimeIRI = IRI.create(TRESTLE_PREFIX, "valid_time");
    public static final IRI validTimeOfIRI = IRI.create(TRESTLE_PREFIX, "valid_time_of");
    public static final IRI hasRelationIRI = IRI.create(TRESTLE_PREFIX, "has_relation");
    public static final IRI relationOfIRI = IRI.create(TRESTLE_PREFIX, "relation_of");
    public static final IRI relatedToIRI = IRI.create(TRESTLE_PREFIX, "relatedTo");
    public static final IRI databaseTimeIRI = IRI.create(TRESTLE_PREFIX, "database_time");
    public static final IRI databaseTimeOfIRI = IRI.create(TRESTLE_PREFIX, "database_time_of");
//    Classes
    public static final IRI conceptRelationIRI = IRI.create(TRESTLE_PREFIX, "Relation");
    public static final IRI spatialRelationIRI = IRI.create(TRESTLE_PREFIX, "Spatial_Relation");
    public static final IRI temporalRelationIRI = IRI.create(TRESTLE_PREFIX, "Temporal_Relation");
    public static final IRI semanticRelationIRI = IRI.create(TRESTLE_PREFIX, "Semantic_Relation");
    public static final IRI datasetClassIRI = IRI.create(TRESTLE_PREFIX, "Dataset");
    public static final IRI factClassIRI = IRI.create(TRESTLE_PREFIX, "Fact");
    public static final IRI temporalClassIRI = IRI.create(TRESTLE_PREFIX, "Temporal_Object");
//    Properties
    public static final IRI relationStrengthIRI = IRI.create(TRESTLE_PREFIX, "Relation_Strength");
    public static final IRI dateTimeDatatypeIRI = IRI.create(XSDPREFIX, "dateTime");
    public static final IRI dateDatatypeIRI = IRI.create(XSDPREFIX, "date");
    public static final IRI UUIDDatatypeIRI = IRI.create(TRESTLE_PREFIX, "UUID");
    public static final IRI WKTDatatypeIRI = IRI.create(GEOSPARQLPREFIX, "wktLiteral");

}

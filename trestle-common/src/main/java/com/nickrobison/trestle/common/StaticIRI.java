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
    public static final IRI existsTimeIRI = IRI.create(TRESTLE_PREFIX, "exists_time");
    public static final IRI temporalExistsAtIRI = IRI.create(TRESTLE_PREFIX, "exists_at");
    public static final IRI temporalValidAtIRI = IRI.create(TRESTLE_PREFIX, "valid_at");
    public static final IRI temporalExistsToIRI = IRI.create(TRESTLE_PREFIX, "exists_to");
    public static final IRI temporalExistsFromIRI = IRI.create(TRESTLE_PREFIX, "exists_from");
    public static final IRI temporalValidToIRI = IRI.create(TRESTLE_PREFIX, "valid_to");
    public static final IRI temporalValidFromIRI = IRI.create(TRESTLE_PREFIX, "valid_from");
    public static final IRI temporalDatabaseToIRI = IRI.create(TRESTLE_PREFIX, "database_to");
    public static final IRI temporalDatabaseFromIRI = IRI.create(TRESTLE_PREFIX, "database_from");
    public static final IRI temporalStartIRI = IRI.create(TRESTLE_PREFIX, "start_temporal");
    public static final IRI temporalEndIRI = IRI.create(TRESTLE_PREFIX, "end_temporal");
    public static final IRI temporalAtIRI = IRI.create(TRESTLE_PREFIX, "at_temporal");
    public static final IRI validTimeIRI = IRI.create(TRESTLE_PREFIX, "valid_time");
    public static final IRI validTimeOfIRI = IRI.create(TRESTLE_PREFIX, "valid_time_of");
    public static final IRI hasRelationIRI = IRI.create(TRESTLE_PREFIX, "has_relation");
    public static final IRI relationOfIRI = IRI.create(TRESTLE_PREFIX, "relation_of");
    public static final IRI relatedToIRI = IRI.create(TRESTLE_PREFIX, "related_to");
    public static final IRI relatedByIRI = IRI.create(TRESTLE_PREFIX, "related_by");
    public static final IRI hasCollectionIRI = IRI.create(TRESTLE_PREFIX, "has_collection");
    public static final IRI collectionOfIRI = IRI.create(TRESTLE_PREFIX, "collection_of");
    public static final IRI databaseTimeIRI = IRI.create(TRESTLE_PREFIX, "database_time");
    public static final IRI databaseTimeOfIRI = IRI.create(TRESTLE_PREFIX, "database_time_of");
    public static final IRI hasOverlapIRI = IRI.create(TRESTLE_PREFIX, "has_overlap");
    public static final IRI overlapOfIRI = IRI.create(TRESTLE_PREFIX, "overlap_of");
    public static final IRI componentOfIRI = IRI.create(TRESTLE_PREFIX, "component_of");
    public static final IRI mergedIRI = IRI.create(TRESTLE_PREFIX, "merged");
    public static final IRI mergeOfIRI = IRI.create(TRESTLE_PREFIX, "merge_of");
    public static final IRI splitIRI = IRI.create(TRESTLE_PREFIX, "split");
    public static final IRI splitOfIRI = IRI.create(TRESTLE_PREFIX, "split_of");
    public static final IRI hasComponetIRI = IRI.create(TRESTLE_PREFIX, "has_component");
    public static final IRI mergedIntoIRI = IRI.create(TRESTLE_PREFIX, "merged_into");
    public static final IRI mergedFromIRI = IRI.create(TRESTLE_PREFIX, "merged_from");
    public static final IRI splitFromIRI = IRI.create(TRESTLE_PREFIX, "split_from");
    public static final IRI splitIntoIRI = IRI.create(TRESTLE_PREFIX, "split_into");
//    Classes
    public static final IRI trestleCollectionIRI = IRI.create(TRESTLE_PREFIX, "Trestle_Collection");
    public static final IRI trestleObjectIRI = IRI.create(TRESTLE_PREFIX, "Trestle_Object");
    public static final IRI trestleRelationIRI = IRI.create(TRESTLE_PREFIX, "Trestle_Relation");
    public static final IRI spatialRelationIRI = IRI.create(TRESTLE_PREFIX, "Spatial_Relation");
    public static final IRI temporalRelationIRI = IRI.create(TRESTLE_PREFIX, "Temporal_Relation");
    public static final IRI semanticRelationIRI = IRI.create(TRESTLE_PREFIX, "Semantic_Relation");
    public static final IRI eventRelationIRI = IRI.create(TRESTLE_PREFIX, "Event_Relation");
    public static final IRI componentRelationIRI = IRI.create(TRESTLE_PREFIX, "Component_Relation");
    public static final IRI trestleOverlapIRI = IRI.create(TRESTLE_PREFIX, "Trestle_Overlap");
    public static final IRI datasetClassIRI = IRI.create(TRESTLE_PREFIX, "Dataset");
    public static final IRI factClassIRI = IRI.create(TRESTLE_PREFIX, "Fact");
    public static final IRI temporalClassIRI = IRI.create(TRESTLE_PREFIX, "Temporal_Object");
    public static final IRI temporalPropertyIRI = IRI.create(TRESTLE_PREFIX, "Temporal_Property");
    public static final IRI trestleEventIRI = IRI.create(TRESTLE_PREFIX, "Trestle_Event");
    public static final IRI temporalObjectIRI = IRI.create(TRESTLE_PREFIX, "Temporal_Object");
    public static final IRI existsTemporalObjectIRI = IRI.create(TRESTLE_PREFIX, "Exists_Object");
    public static final IRI validTemporalObjectIRI = IRI.create(TRESTLE_PREFIX, "Valid_Object");
    public static final IRI databaseTemporalObjectIRI = IRI.create(TRESTLE_PREFIX, "Database_Object");
    public static final IRI intervalTemporalObjectIRI = IRI.create(TRESTLE_PREFIX, "Interval_Object");
    public static final IRI pointTemporalObjectIRI = IRI.create(TRESTLE_PREFIX, "Point_Object");
    public static final IRI spatialUnionIRI = IRI.create(TRESTLE_PREFIX, "SpatialUnion");

//    Properties
    public static final IRI relationStrengthIRI = IRI.create(TRESTLE_PREFIX, "Relation_Strength");
    public static final IRI sOverlapIRI = IRI.create(TRESTLE_PREFIX, "SOverlap");
    public static final IRI tOverlapIRI = IRI.create(TRESTLE_PREFIX, "TOverlap");
    public static final IRI dateTimeDatatypeIRI = IRI.create(XSDPREFIX, "dateTime");
    public static final IRI dateDatatypeIRI = IRI.create(XSDPREFIX, "date");
    public static final IRI UUIDDatatypeIRI = IRI.create(TRESTLE_PREFIX, "UUID");
    public static final IRI WKTDatatypeIRI = IRI.create(GEOSPARQLPREFIX, "wktLiteral");

}

BEGIN
SEM_APIS.CREATE_ENTAILMENT(
'owl_rix_gaul7',
SEM_Models('hadoop_gaul7'),
SEM_Rulebases('OWLPRIME'));
END;

BEGIN
SEM_APIS.LOOKUP_ENTAILMENT(
    SEM_Models('hadoop_gaul7'),
      SEM_RULEBASES('OWLPRIME'));
END;

SELECT DISTINCT f
FROM TABLE(SEM_MATCH(
               '{ ?m rdf:type :GAUL .
               ?m :ADM2_Code ?c .
               ?m :has_relation ?r .
               ?r rdf:type :Concept_Relation .
               ?r :Relation_Strength ?s .
               ?r :has_relation ?f .
               ?f rdf:type :GAUL
               FILTER( ?m = :test_muni4 && ?s >= .3) }',
               SEM_Models('trestle_test2'),
               SEM_RuleBases('OWLPRIME'),
               SEM_ALIASES(SEM_ALIAS('', 'http://nickrobison.com/dissertation/trestle.owl#')),
               null));


SELECT m, gaulName, tStart, tEnd
  FROM TABLE(SEM_MATCH(
      '{?m rdf:type :gaul-test .
      ?m :objectName ?gaulName .
      ?m ogc:asWKT ?wkt .
      ?m :has_temporal ?t .
      { ?t :valid_from ?tStart} UNION {?t :exists_from ?tStart} .
      OPTIONAL{{ ?t :valid_to ?tEnd} UNION {?t :exists_to ?tEnd}} .
      FILTER( (?tStart < "2007-01-01T23:59:59Z"^^xsd:dateTime && ?tEnd >= "2007-01-01T23:59:59Z"^^xsd:dateTime) && ogcf:sfIntersects(?wkt, "POLYGON ((31.08333950117128 -25.57202871446725, 31.08333950117128 -24.57695170392678, 33.8656270988277 -24.57695170392678, 33.8656270988277 -25.57202871446725, 31.08333950117128 -25.57202871446725))"^^ogc:wktLiteral))
      }',
    SEM_Models('hadoop_gaul5'),
    SEM_RULEBASES('OWLPRIME'),
    SEM_ALIASES(SEM_ALIAS('', 'http://nickrobison.com/dissertation/trestle.owl#')),
    null,
    null,
    'HINT0={ LEADING(?wkt)}'));

-- Check the temporals
SELECT *
  FROM TABLE (SEM_MATCH(
    '{?m rdf:type ?t FILTER(?m = :d77aa0fa-6bdd-4fd2-a47a-8ee6c9c17463)}',
    SEM_Models('hadoop_gaul5'),
    SEM_RULEBASES('OWLPRIME'),
  SEM_ALIASES(SEM_ALIAS('', 'http://nickrobison.com/dissertation/trestle.owl#')),
  null));



-- Validate Admin stuff for the semantic graph

SELECT * FROM mdsys.sem_model$;
BEGIN
sem_apis.drop_sem_model('api_test');
END;
DROP TABLE api_test_tpl;
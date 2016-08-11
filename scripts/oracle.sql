BEGIN
SEM_APIS.CREATE_ENTAILMENT(
'owl_rix_trestle',
SEM_Models('trestle_test2'),
SEM_Rulebases('OWLPRIME'));
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


-- Validate Admin stuff for the semantic graph

SELECT * FROM mdsys.sem_model$;
BEGIN
sem_apis.drop_sem_model('hadoop_test');
END;
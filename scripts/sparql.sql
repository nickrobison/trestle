PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
"SELECT * FROM SEM_MATCH(
{?m rdf:type ?type . ?type rdfs:subClassOf ?class}
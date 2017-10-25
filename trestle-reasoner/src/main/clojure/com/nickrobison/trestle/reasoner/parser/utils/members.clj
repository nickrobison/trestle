(ns com.nickrobison.trestle.reasoner.parser.utils.members
  (:import (org.semanticweb.owlapi.model IRI)
           (com.nickrobison.trestle.common StaticIRI)
           (com.nickrobison.trestle.reasoner.annotations Spatial Fact))
  (:require [com.nickrobison.trestle.reasoner.parser.utils.predicates :as pred]))

(defn build-iri
  "Build the property IRI for the Member"
  [member prefix]
  (if (pred/hasAnnotation? member Fact)
    (IRI/create prefix (.name (.getAnnotation member Fact)))
    (if (pred/hasAnnotation? member Spatial)
      ; If spatial, use the GEOSPARQL prefix
      (IRI/create StaticIRI/GEOSPARQLPREFIX "asWKT")
      ; Otherwise, use the member-name
      (IRI/create prefix (pred/filter-member-name member)))))

; Build the OWLDataProperties

(defn build-data-property
  "Build the OWLDataProperty for the member"
  [member prefix df]
  (.getOWLDataProperty df (build-iri member prefix)))
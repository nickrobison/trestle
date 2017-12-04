(ns com.nickrobison.trestle.reasoner.parser.utils.members
  (:import (org.semanticweb.owlapi.model IRI OWLDataFactory)
           (com.nickrobison.trestle.common StaticIRI)
           (com.nickrobison.trestle.reasoner.annotations Spatial Fact)
           (java.lang.invoke MethodHandles)
           (java.lang.reflect Constructor Method Field)
           (com.nickrobison.trestle.reasoner.exceptions InvalidClassException InvalidClassException$State))
  (:require [com.nickrobison.trestle.reasoner.parser.utils.predicates :as pred]
            [clojure.string :as string]
            [clojure.core.reducers :as r]))

; Filter functions
(defn filter-member-name-iri
  "Filter a member based on matching the name or the IRI"
  [name member]
  (or
    (= name (:name member))
    (= name (.toString ^IRI (:iri member)))))

(defn filter-and-get
  "Apply the given filter to the list of members and extract the specified key"
  [members fn key]
  (->> members
       (filter fn)
       (map #(get % key))
       first))

(defn ^IRI build-iri
  "Build the property IRI for the Member"
  [member prefix]
  (if (pred/hasAnnotation? member Fact)
    ; Reflection is ok here, we're trying to paper over Field/Method differences
    (IRI/create prefix (.name ^Fact (.getAnnotation member Fact)))
    (if (pred/hasAnnotation? member Spatial)
      ; If spatial, use the GEOSPARQL prefix
      (IRI/create StaticIRI/GEOSPARQLPREFIX "asWKT")
      ; Otherwise, use the member-name
      (IRI/create prefix (pred/filter-member-name member)))))

; Reflection helpers

; Method Handle methods
(defmulti make-handle
          "Make a method handle for a given Class Member"
          class)
(defmethod make-handle Field [field] (.unreflectGetter (MethodHandles/lookup) field))
(defmethod make-handle Method [method] (.unreflect (MethodHandles/lookup) method))
(defmethod make-handle Constructor [constructor] (.unreflectConstructor (MethodHandles/lookup) constructor))

; Build the OWLDataProperties

(defn build-data-property
  "Build the OWLDataProperty for the member"
  [member prefix ^OWLDataFactory df]
  (.getOWLDataProperty df (build-iri member prefix)))

; Individual ID
(defn replace-several
  "Apply replacement rules to input string"
  [id & replacements]
  (let [replacement-list (partition 2 replacements)]
    (r/reduce #(apply string/replace %1 %2) id replacement-list)))

(defn normalize-id
  "Apply normalization rules to Individual ID"
  [id]
  (replace-several id #"\s+" "_"))

(defn get-from-array
  "Get specified key from array of maps"
  [key coll]
  (->> coll
       (map #(key %))
       (into ())))


; Temporal validations
(defn ensure-temporal-count
  "Verifies that the temporal count for a given type is satisfied"
  [comp-fn num type member accessor
   members ^String class-name
   ^InvalidClassException$State state ^String message]
  (if (comp-fn (count (filter (fn [m]
                                (= (get m accessor) type)) members)) num)
    ; If true, return the member
    member
    (throw (InvalidClassException.
             class-name state message))))
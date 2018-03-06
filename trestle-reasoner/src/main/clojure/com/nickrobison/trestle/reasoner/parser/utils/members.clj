(ns com.nickrobison.trestle.reasoner.parser.utils.members
  (:import (org.semanticweb.owlapi.model IRI OWLDataFactory)
           (com.nickrobison.trestle.common StaticIRI)
           (com.nickrobison.trestle.reasoner.annotations Spatial Fact TrestleDataProperty)
           (java.lang.invoke MethodHandles MethodHandle)
           (java.lang.reflect Constructor Method Field)
           (com.nickrobison.trestle.reasoner.exceptions InvalidClassException InvalidClassException$State))
  (:require [com.nickrobison.trestle.reasoner.parser.utils.predicates :as pred]
            [com.nickrobison.trestle.reasoner.parser.spatial :as spatial]
            [clojure.string :as string]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :as log]))

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

(declare filter-java-member-name)
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
      (IRI/create prefix (filter-java-member-name member)))))

; Reflection helpers

; Method Handle methods
(defmulti make-handle
          "Make a method handle for a given Class Member"
          class)
(defmethod make-handle Field [field] (.unreflectGetter (MethodHandles/lookup) field))
(defmethod make-handle Method [method] (.unreflect (MethodHandles/lookup) method))
(defmethod make-handle Constructor [constructor] (.unreflectConstructor (MethodHandles/lookup) constructor))

(defn invoker
  "Invoke method handle"
  ; We need to use invokeWithArguments to work around IDEA-154967
  ([^MethodHandle handle object]
   (try
     (log/debugf "Invoking method handle %s on %s" handle object)
     (.invokeWithArguments handle (object-array [object]))
     (catch Exception e
       (log/error "Problem invoking" e))))
  ([^MethodHandle handle object & args]
   (try
     (log/debugf "Invoking method handle %s on %s with args %s"
                 handle
                 object
                 args)
     (.invokeWithArguments handle (object-array [object args]))
     (catch Exception e
       (log/error "Problem invoking %s on %s" handle object e)))))

(defn invoke-constructor
  "Invoke Constructor Method Handle"
  ; We need to use invokeWithArguments to work around IDEA-154967
  ([^MethodHandle handle]
   (try
     (log/debugf "Invoking constructor %s" handle)
     (.invokeWithArguments handle (object-array []))
     (catch Exception e
       (log/error "Problem invoking" e))))
  ([^MethodHandle handle & args]
   (try
     (log/debugf "Invoking constructor %s with args %s" handle args)
     ; I honestly have no idea why we need to do first, but that's how it is
     (.invokeWithArguments handle (object-array (first args)))
     (catch Exception e
       (log/error "Problem invoking" e)))))

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

; Member name and constructor parameter methods

(defn trim-method-name
  "Filter the method name by removing extra characters"
  [method]
  (let [name (.getName ^Method method)]
    (if (string/starts-with? name "get")
      ; Strip off the get and lower case the first letter
      (str (string/lower-case (subs name 3 4)) (subs name 4))
      ; If not, just return the name
      name)))


; Filter member name to strip out unwanted characters
(defmulti filter-java-member-name
          "Filter name of Java class member.
          If the member annotation has the name() property set, we use that,
          otherwise we use the filtered name from the member.

          For fields, we just return the field name, as is.
          For method, we strip of the 'get' and lowercase the first letter."
          class)
(defmethod filter-java-member-name Field [^Field field]
  ;Just return the field name
  (.getName field))
(defmethod filter-java-member-name Method [^Method method]
  ; Trim the method name and return it
  (trim-method-name method))


(defmulti filter-constructor-name
          "Filter the member to determine the appropriate name for the constructor
          This allows us to to special casing for mult-lang members, since we can't use the fact name due to language overloading"
          (fn [member type] type))
(defmethod filter-constructor-name ::pred/language
  [member type]
  ; If we're a language, we need to use the java member name
  (filter-java-member-name member))
(defmethod filter-constructor-name :default
  [member type]
  (if-let [data-annotation (pred/get-common-annotation member TrestleDataProperty)]
    ; Reflection is ok here, we're trying to paper over annotation differences
    (if (not= "" (.name data-annotation))
      (.name data-annotation)
      (filter-java-member-name member))
    (filter-java-member-name member)))

; Spatial methods
(defn get-projection
  "Get the Projection from the spatial member, or use the default"
  [member defaultProjection]
  (let [annotation (pred/get-annotation member Spatial)
        projection (.projection ^Spatial annotation)]
    (spatial/validate-spatial-projection
                              (if (= projection 0)
                                ; If we haven't specified a projection, use the default
                                defaultProjection
                                ; Otherwise, return the annotated projection
                                projection))))

(ns com.nickrobison.trestle.reasoner.parser
  (:import [IClassParser]
           (com.nickrobison.trestle.reasoner.parser IClassParser TypeConverter)
           (org.semanticweb.owlapi.model IRI OWLClass OWLDataFactory OWLNamedIndividual OWLDataPropertyAssertionAxiom)
           (java.lang.annotation Annotation)
           (java.lang.reflect InvocationTargetException Field Method Modifier)
           (java.lang.invoke MethodHandles)
           (com.nickrobison.trestle.reasoner.annotations IndividualIdentifier DatasetClass)
           (java.util Optional List))
  (:require [clojure.core.match :refer [match]]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [com.nickrobison.trestle.reasoner.com.nickrobison.trestle.reasoner.parser.utils.predicates :as pred]))





(defn accessMethodValue [classMethod inputObject]
  (. (TypeConverter/parsePrimitiveClass (.getReturnType classMethod)) cast
     (try
       (log/trace (str "Attempting to invoke on: " (.getName classMethod)))
       (.invoke classMethod inputObject nil)
       (catch IllegalAccessError e
         (log/error "Cannot access method " classMethod e))
       (catch InvocationTargetException e
         (log/error "Invocation failed on" classMethod e)))))

(defn invoker
  "Invoke method handle"
  ; We need to use invokeWithArguments to work around IDEA-154967
  ([handle object] (.invokeWithArguments handle (object-array [object])))
  ([handle object & args]
   (.invokeWithArguments handle (object-array [object args]))))

(defn annotationFilter
  "Filters a list of members for a given annotation"
  [members annotation]
  (filter #(pred/hasAnnotation? % annotation) members))

(defn parseIndividualFields [fields inputObject]
  (when-first [field
               (filter #(pred/hasAnnotation? % IndividualIdentifier) fields)]
    (.toString
      (try
        (. field get inputObject)
        (catch IllegalStateException e (println "Problem!"))))))

(defn parseIndividualMethods [methods inputObject]
  (when-first [method (annotationFilter methods IndividualIdentifier)]
    (.toString
      (accessMethodValue method inputObject))
    )
  )

; Method Handle methods
(defmulti make-handle
          "Make a method handle for a given Class Member"
          class)
(defmethod make-handle Field [field] (.unreflectGetter (MethodHandles/lookup) field))
(defmethod make-handle Method [method] (.unreflect (MethodHandles/lookup) method))

(defn build-member
  "Build member from class methods/fields"
  [member df prefix]
  (let [iri (pred/build-iri member prefix)]
    {
     :name (pred/filter-member-name member)
     :iri iri
     :data-property (.getOWLDataProperty df iri)
     :handle (make-handle member)
     :type (pred/member-type member)}))


(defn member-reducer
  "Build the Class Member Map"
  [acc member]
  (let [members (get acc :members)
        type (get member :type)]
    (match [type]
           [::pred/identifier] (merge acc {:members (conj members member) :identifier member})
           [::pred/spatial] (merge acc {:members (conj members member) :spatial member})
           :else (assoc acc :members (conj members member)))))


(defrecord ClojureClassParser [^OWLDataFactory df,
                               ^String reasonerPrefix,
                               ^boolean multiLangEnabled,
                               ^String defaultLanguageCode
                               classRegistry]
  IClassParser
  (isMultiLangEnabled [this] (multiLangEnabled))
  (getDefaultLanguageCode [this] (if (= defaultLanguageCode "") nil defaultLanguageCode))
  (^OWLClass getObjectClass [this ^Class clazz] (. df getOWLClass
                                                   (IRI/create reasonerPrefix
                                                               (if (.isAnnotationPresent clazz DatasetClass)
                                                                 (.name (.getDeclaredAnnotation clazz DatasetClass))
                                                                 (.getName clazz)))))
  (^OWLClass getObjectClass [this ^Object inputObject] (.getObjectClass this (.getClass inputObject)))
  (^OWLNamedIndividual getIndividual [this ^Object inputObject]
    (. df getOWLNamedIndividual (IRI/create reasonerPrefix
                                            (let [
                                                  field (parseIndividualFields
                                                          (seq (.getDeclaredFields (.getClass inputObject))) inputObject)
                                                  method (parseIndividualMethods
                                                           (.getDeclaredMethods (.getClass inputObject)) inputObject)]
                                              (match [field method]
                                                     [nil _] method
                                                     [_ nil] field
                                                     :else "Nothing!")))))
  (parseClass ^Object [this ^Class clazz]
    (->> (concat (.getDeclaredFields clazz) (.getDeclaredMethods clazz))
         ; Filter out non-necessary members
         (r/filter pred/filter-member)
         ; Build members
         (r/map #(build-member % df reasonerPrefix))
         (r/reduce member-reducer {})))
  (getFacts ^Optional ^List ^ OWLDataPropertyAssertionAxiom [this inputObject filterSpatial]
    (let [parsedClass (.parseClass this (.getClass inputObject))
          individual (.getIndividual this inputObject)]
      (Optional/of
           (->> (get parsedClass :members)
                ; Filter spatial
                (r/filter (fn [member]
                            (if (true? filterSpatial)
                              (complement (= (get member :type) ::pred/spatial))
                              true)))
                ; Build the assertion axiom
                (r/map (fn [member]
                         (.getOWLDataPropertyAssertionAxiom df
                                                            (get member :data-property)
                                                            individual
                                                            (.getOWLLiteral df
                                                                            (invoker
                                                                              (get member :handle) inputObject)))))
                (into ()))))))

(defn make-parser
  "Creates a new ClassParser"
  [df reasonerPrefix multiLangEnabled defaultLanguageCode]
  (->ClojureClassParser df reasonerPrefix multiLangEnabled defaultLanguageCode {}))

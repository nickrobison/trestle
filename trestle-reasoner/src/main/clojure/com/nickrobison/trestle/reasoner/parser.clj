(ns com.nickrobison.trestle.reasoner.parser
  (:import [IClassParser]
           (com.nickrobison.trestle.reasoner.parser IClassParser TypeConverter)
           (org.semanticweb.owlapi.model IRI OWLClass OWLDataFactory OWLNamedIndividual)
           (com.nickrobison.trestle.reasoner.annotations DatasetClass IndividualIdentifier Fact Spatial Language NoMultiLanguage Ignore)
           (java.lang.annotation Annotation)
           (java.lang.reflect InvocationTargetException Field Method Modifier)
           (java.lang.invoke MethodHandles))
  (:require [clojure.core.match :refer [match]]
            [clojure.tools.logging :as log]))


(defn hasAnnotation? [member annotation] (.isAnnotationPresent member annotation))

(defn accessMethodValue [classMethod inputObject]
  (. (TypeConverter/parsePrimitiveClass (.getReturnType classMethod)) cast
     (try
       (log/trace (str "Attempting to invoke on: " (.getName classMethod)))
       (. classMethod (.invoke inputObject nil))
       (catch IllegalAccessError e
         (log/error "Cannot access method " classMethod e))
       (catch InvocationTargetException e
         (log/error "Invocation failed on" classMethod e))))
  )

(defn annotationFilter
  "Filters a list of members for a given annotation"
  [members annotation]
  (filter #(hasAnnotation? % annotation) members))

(defn parseIndividualFields [fields inputObject]
  (when-first [field
               (filter #(hasAnnotation? % IndividualIdentifier) fields)]
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

; Filter Predicates
(defn get-member-name [member] (.getName member))
(defn public? [entity] (Modifier/isPublic (.getModifiers entity)))
(defn ignore? [entity] (hasAnnotation? entity Ignore))
(defn fact? [entity] (hasAnnotation? entity Fact))
(defn spatial? [entity] (hasAnnotation? entity Spatial))
(defn language? [entity] (hasAnnotation? entity Language))
(defn noMultiLanguage? [entity] (hasAnnotation? entity NoMultiLanguage))
(defn identifier? [entity] (hasAnnotation? entity IndividualIdentifier))
(defn noAnnotations? [entity] (= (count (.getAnnotations entity)) 0))
(defn ebean? [entity] (clojure.string/includes? (.getName entity) "_ebean"))
(defn noParams? [entity] (= (.getParameterCount entity) 0))
(defn internal?
  "Are the methods internal methods? e.g. hashCode, Equals, etc"
  [entity] (or
             (= (get-member-name entity) "hashCode")
             (= (get-member-name entity) "equals")
             (= (get-member-name entity) "toString")
             (= (get-member-name entity) "wait")
             (= (get-member-name entity) "notify")
             (= (get-member-name entity) "notifyAll")
             (= (get-member-name entity) "getClass")))
(defn void-return? [entity] (= (.getReturnType entity) Void))
(defn include? [entity] ((some-fn fact?
                             spatial?
                             identifier?
                             language?
                             noMultiLanguage?
                             noAnnotations?) entity))


; Method Handle methods
(defmulti make-handle
          "Make a method handle for a given Class Member"
          class)
(defmethod make-handle Field [field] (.unreflectGetter (MethodHandles/lookup) field))
(defmethod make-handle Method [method] (.unreflect (MethodHandles/lookup) method))

(defmulti filter-members
  "Filter Class Members to only include the ones we need"
  class)
(defmethod filter-members Field [field]
  ((every-pred
     (complement ignore?)
     public?
     include?
     (complement ebean?)) field))
(defmethod filter-members Method [method]
  ((every-pred
     (complement ignore?)
     public?
     include?
     noParams?
     (complement internal?)
     (complement ebean?)
     (complement void-return?)) method))




(defn get-methods
  "Get all the methods for a class and return them as a method handle"
  [clazz]
  (into [] (map #(assoc {} :name (.getName %) :handle (make-handle %)) (filter filter-members (.getMethods clazz)))))

(defn get-fields
  "Get all the fields for a class and return them as a method handle"
  [clazz]
  (into [] (map #(assoc {} :name (.getName %) :handle (make-handle %)) (filter filter-members (.getFields clazz)))))


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
    (assoc {} :members (concat (get-methods clazz) (get-fields clazz)))))

(defn make-parser
  "Creates a new ClassParser"
  [df reasonerPrefix multiLangEnabled defaultLanguageCode]
  (->ClojureClassParser df reasonerPrefix multiLangEnabled defaultLanguageCode {}))

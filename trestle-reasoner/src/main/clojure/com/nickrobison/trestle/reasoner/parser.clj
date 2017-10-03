(ns com.nickrobison.trestle.reasoner.parser
  (:import [IClassParser]
           (com.nickrobison.trestle.reasoner.parser IClassParser TypeConverter)
           (org.semanticweb.owlapi.model IRI OWLClass OWLDataFactory OWLNamedIndividual)
           (com.nickrobison.trestle.reasoner.annotations DatasetClass IndividualIdentifier)
           (java.lang.annotation Annotation)
           (java.lang.reflect InvocationTargetException))
  (:require [clojure.core.match :refer [match]]
            [clojure.tools.logging :as log])
  )


(defn hasAnnotation? [member annotation] (. member isAnnotationPresent annotation))

(defn accessMethodValue [classMethod inputObject]
  (. (TypeConverter/parsePrimitiveClass (. classMethod getReturnType)) cast
     (try
       (log/trace (str "Attempting to invoke on: " (.getName classMethod)))
       (. classMethod (invoke inputObject nil))
       (catch IllegalAccessError e
         (log/error "Cannot access method " classMethod e))
       (catch InvocationTargetException e
         (log/error "Invocation failed on" classMethod e))))
  )

(defn parseIndividualFields [fields inputObject]
  (when-first [field
               (filter #(. % isAnnotationPresent IndividualIdentifier) fields)]
    (.toString
      (try
        (. field get inputObject)
        (catch IllegalStateException e (println "Problem!")
                                       ))))
  )

(defn parseIndividualMethods [methods inputObject]
  (when-first [method
               (filter #(hasAnnotation? % IndividualIdentifier) methods)]
    (.toString
      (accessMethodValue method inputObject))
    )
  )


(defrecord ClojureClassParser [^OWLDataFactory df,
                               ^String reasonerPrefix,
                               ^boolean multiLangEnabled,
                               ^String defaultLanguageCode]
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
                                                     :else "Nothing!"))))
    )
  )
(ns com.nickrobison.trestle.reasoner.parser.types.converter
  (:require [clojure.tools.logging :as log])
  (:import (com.nickrobison.trestle.reasoner.parser ITypeConverter)
           (org.semanticweb.owlapi.model IRI OWLDataFactory)
           (java.util Map)))


(defrecord ClojureTypeConverter
  [^OWLDataFactory df
   owl-to-java-map
   java-to-owl-map
   classConstructors]
  ITypeConverter
  (registerTypeConstructor [_ constructor]
    (let [javaClass (.getJavaType constructor)
          name (.getConstructorName constructor)
          datatype (.getOWLDatatype constructor)
          owlDatatype (.getOWLDatatype df (IRI/create datatype))]
      (if (contains? java-to-owl-map javaClass)
        (log/warnf "Overwriting mapping of Java class %s with %s"
                  javaClass name)
        (log/infof "Registering Type Constructor %s for %s and %s"
                  name
                  javaClass datatype)
        )
      (assoc owl-to-java-map owlDatatype javaClass)
      (assoc java-to-owl-map javaClass owlDatatype)))
  )

(defn make-type-converter
  [df ^Map owl-to-java-map ^Map java-to-owl-map]
  (->ClojureTypeConverter
      df
      ; We have to convert our HashMaps into Associated maps
      (zipmap (.keySet owl-to-java-map) (.values owl-to-java-map))
      (zipmap (.keySet java-to-owl-map) (.values java-to-owl-map))
      {}))
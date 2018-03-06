(ns com.nickrobison.trestle.reasoner.parser.types.converter
  (:require [clojure.tools.logging :as log])
  (:import (com.nickrobison.trestle.reasoner.parser ITypeConverter)
           (org.semanticweb.owlapi.model IRI OWLDataFactory OWLLiteral)
           (java.util Map)
           (org.semanticweb.owlapi.vocab OWL2Datatype)))


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
  (getDatatypeFromJavaClass
    [_ javaTypeClass]
    (if-let [datatype (get owl-to-java-map javaTypeClass)]
      datatype
      (log/errorf "Unsupported Java type %s" javaTypeClass)
      (.getDatatype (OWL2Datatype/XSD_STRING) df)))
  (getDatatypeFromAnnotation
    [this annotation returnType]
    (let [datatype (.datatype annotation)]
      (if (.equals datatype OWL2Datatype/XSD_ANY_URI))
      (.getDatatypeFromJavaClass this returnType)
      (.getDatatype datatype df)))
  (lookupJavaClassFromOWLDatatype
    [_ dataProperty classToVerify]
    (let [datatype (.getDatatype ^OWLLiteral (.getObject dataProperty))]
      ; If the datatype is built int
      (if (.isBuiltIn datatype)
        ()
        ; Check to see if it's a spatial type, based on the IRI
        (let [iri (.getShortForm (.getIRI datatype))]
          (if (or
                (.equals iri "wktLiteral")
                (.equals iri "Geometry"))
            ; If we have a nil class, use String as the spatial type
            (if (nil? classToVerify)
              (class String)
              ; Get spatial class
              (class Integer))
            ; If we're not spatial, try to lookup the class from the registry,
            ;otherwise, use a string
            (if-let [matchedClass (get owl-to-java-map datatype)]
              matchedClass
              (class String)))))))
  (lookupJavaClassFromOWLDataProperty
    [_ classToVerify property]
    (let [datatype ()]))
  )

(defn make-type-converter
  [df ^Map owl-to-java-map ^Map java-to-owl-map]
  (->ClojureTypeConverter
    df
    ; We have to convert our HashMaps into Associated maps
    (zipmap (.keySet owl-to-java-map) (.values owl-to-java-map))
    (zipmap (.keySet java-to-owl-map) (.values java-to-owl-map))
    {}))
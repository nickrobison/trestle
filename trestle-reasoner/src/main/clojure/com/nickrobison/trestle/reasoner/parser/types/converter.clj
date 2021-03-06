(ns com.nickrobison.trestle.reasoner.parser.types.converter
  (:require [clojure.tools.logging :as log]
            [com.nickrobison.trestle.reasoner.parser.spatial :refer [wkt-to-geom, reproject]]
            [com.nickrobison.trestle.reasoner.parser.spatial :as spatial])
  (:import (com.nickrobison.trestle.reasoner.parser ITypeConverter TypeUtils TypeConstructor)
           (org.semanticweb.owlapi.model IRI OWLDataFactory OWLLiteral)
           (java.util Map)
           (org.semanticweb.owlapi.vocab OWL2Datatype)))


(defrecord ClojureTypeConverter
  [^OWLDataFactory df
   typeMap]
  ITypeConverter
  (registerTypeConstructor [_ constructor]
    (let [javaClass (.getJavaType constructor)
          name (.getConstructorName constructor)
          datatype (.getOWLDatatype constructor)
          owlDatatype (.getOWLDatatype df (IRI/create datatype))]
      (if (contains? (:java-to-owl @typeMap) javaClass)
        (do
          (log/warnf "Overwriting mapping of Java class %s with %s"
                     javaClass name)
          (swap! typeMap #(-> %
                              (assoc-in [:owl-to-java owlDatatype] javaClass)
                              (assoc-in [:java-to-owl javaClass] owlDatatype)
                              (assoc-in [:constructors (.getTypeName javaClass)] constructor))))
        (do
          (log/infof "Registering Type Constructor %s for %s and %s"
                     name
                     javaClass datatype)
          (swap! typeMap #(-> %
                              (assoc-in [:owl-to-java owlDatatype] javaClass)
                              (assoc-in [:java-to-owl javaClass] owlDatatype)
                              (assoc-in [:constructors (.getTypeName javaClass)] constructor))))
        )))
  (getDatatypeFromJavaClass
    [_ javaTypeClass]
    (log/tracef "Trying to get type %s" javaTypeClass)
    (if-let [datatype (get (:java-to-owl @typeMap) javaTypeClass)]
      datatype
      (log/spyf :error (str "Unsupported Java type: " javaTypeClass)
                (.getDatatype (OWL2Datatype/XSD_STRING) df))))
  (getDatatypeFromAnnotation
    [this annotation returnType]
    (let [datatype (.datatype annotation)]
      (if (.equals datatype OWL2Datatype/XSD_ANY_URI)
        (.getDatatypeFromJavaClass this returnType)
        (.getDatatype datatype df))))
  (lookupJavaClassFromOWLDatatype
    [this dataProperty javaReturnType]
    (let [datatype (.getDatatype ^OWLLiteral (.getObject dataProperty))]
      ; If the datatype is built-in
      (if (.isBuiltIn datatype)
        (if (nil? javaReturnType)
          ; If we don't know what the return type is, try and lookup a registered mapping,
          ; using the given datatype. Throw an exception if we can't match anything
          (if-let [javaClass (get
                               (:owl-to-java @typeMap)
                               (.getDatatype (.getBuiltInDatatype datatype) df))]
            javaClass
            (throw (IllegalArgumentException. (str "Unsupported OWLDatatype: " datatype))))
          ; If we know what the return type is, do the inverse lookup to make sure we get the correct primitive type
          ; (I think this is wrong)
          (if-let [registeredType (get (:owl-to-java @typeMap) (.getDatatypeFromJavaClass this javaReturnType))]
            registeredType
            (throw (IllegalArgumentException. (str "Unsupported OWLDatatype: " datatype))))
          )
        ; Check to see if it's a spatial type, based on the IRI
        (let [iri (.getShortForm (.getIRI datatype))]
          (if (.equals iri "wktLiteral")
            ; If we have a nil class, use String as the spatial type
            (if (nil? javaReturnType)
              String
              ; Get spatial class
              javaReturnType)
            ; If we're not spatial, try to lookup the class from the registry,
            ;otherwise, use a string as a last resort
            (if-let [matchedClass (get (:owl-to-java @typeMap) datatype)]
              matchedClass
              String))))))
  (lookupJavaClassFromOWLDataProperty
    [this classToVerify property]
    (let [datatype (.getDatatypeFromJavaClass this classToVerify)
          javaClass (get (:owl-to-java @typeMap) datatype)]
      (if (nil? javaClass)
        (if (.equals (.getShortForm (.getIRI (.asOWLDataProperty property))) "asWKT")
          String
          (throw (IllegalStateException. (str "Unsupported data property: " (.asOWLDataProperty property)))))
        javaClass)))
  (extractOWLLiteral
    [_ javaClass literal]
    ; Check to see if the literal is a built-in type that we understand
    (let [extractedLiteral (TypeUtils/rawLiteralConversion javaClass literal)]
      ; We can't do if-let, because the result might be false, so we need to do an explicit nil check
      (if (nil? extractedLiteral)
        (let [extractedLiteral (.getLiteral literal)]
          ; If we don't have a built-in it might be spatial
          (if-let [spatialValue (wkt-to-geom javaClass extractedLiteral)]
            spatialValue
            ; If we don't have a spatial value, check for something from our type constructors
            (if-let [constructor ^TypeConstructor (get (:constructors @typeMap) (.getTypeName javaClass))]
              (.cast javaClass (.constructType constructor (.getLiteral literal)))
              (throw (ClassCastException. (str "Unsupported cast of: " javaClass)))
              )))
        ; If we have a String, it might be a WKT, so check if it's spatial and if so, parse it
        (if (and (instance? String extractedLiteral)
                 (spatial/literal-is-spatial? df literal))
          (wkt-to-geom javaClass extractedLiteral)
          ; Otherwise, just return the built-in literal
          extractedLiteral)
        )))
  (reprojectSpatial
    [_ spatialObject srid]
    ; If the SRID is 0, then we can't reproject, so just return
    (if (= srid 0)
      spatialObject
      (reproject spatialObject srid)))
  )

(defn make-type-converter
  [df ^Map owl-to-java-map ^Map java-to-owl-map]
  (->ClojureTypeConverter
    df
    (atom {
           :owl-to-java  (zipmap (.keySet owl-to-java-map) (.values owl-to-java-map))
           :java-to-owl  (zipmap (.keySet java-to-owl-map) (.values java-to-owl-map))
           :constructors {}
           })))

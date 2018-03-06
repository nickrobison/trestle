(ns com.nickrobison.trestle.reasoner.parser.types.converter
  (:require [clojure.tools.logging :as log]
            [com.nickrobison.trestle.reasoner.parser.spatial :refer  [wkt-to-geom]])
  (:import (com.nickrobison.trestle.reasoner.parser ITypeConverter TypeUtils TypeConstructor)
           (org.semanticweb.owlapi.model IRI OWLDataFactory OWLLiteral)
           (java.util Map Optional)
           (org.semanticweb.owlapi.vocab OWL2Datatype)))


(defrecord ClojureTypeConverter
  [^OWLDataFactory df
   owl-to-java-map
   java-to-owl-map
   class-constructors]
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
      (assoc java-to-owl-map javaClass owlDatatype)
      (assoc class-constructors (.getTypeName javaClass) constructor)))
  (getDatatypeFromJavaClass
    [_ javaTypeClass]
    (if-let [datatype (get java-to-owl-map javaTypeClass)]
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
                               owl-to-java-map
                               (.getDatatype (.getBuiltInDatatype datatype) df))]
            javaClass
            (throw (IllegalArgumentException. (str "Unsupported OWLDatatype: " datatype))))
          ; If we know what the return type is, do the inverse lookup to make sure we get the correct primitive type
          ; (I think this is wrong)
          (if-let [registeredType (get owl-to-java-map (.getDatatypeFromJavaClass this javaReturnType))]
            registeredType
            (throw (IllegalArgumentException. (str "Unsupported OWLDatatype: " datatype))))
          )
        ; Check to see if it's a spatial type, based on the IRI
        (let [iri (.getShortForm (.getIRI datatype))]
          (if (.equals iri "wktLiteral")
            ; If we have a nil class, use String as the spatial type
            (if (nil? javaReturnType)
              (class String)
              ; Get spatial class
              javaReturnType)
            ; If we're not spatial, try to lookup the class from the registry,
            ;otherwise, use a string as a last resort
            (if-let [matchedClass (get owl-to-java-map datatype)]
              matchedClass
              String))))))
  (lookupJavaClassFromOWLDataProperty
    [this classToVerify property]
    (let [datatype (.getDatatypeFromJavaClass this classToVerify)
          javaClass (get owl-to-java-map datatype)]
      (if (nil? javaClass)
        (if (.equals (.getShortForm (.getIRI (.asOWLDataProperty property))) "asWKT")
          String
          (throw (IllegalStateException. (str "Unsupported data property: " (.asOWLDataProperty property)))))
        javaClass)))
  (extractOWLLiteral
    [_ javaClass literal]
    (if-let [extractedLiteral (TypeUtils/rawLiteralConversion javaClass literal)]
      extractedLiteral
      (let [extractedLiteral (.getLiteral literal)]
        ; Check to see if we have a spatial value
        (if-let [spatialValue (wkt-to-geom javaClass extractedLiteral)]
          spatialValue
          ; If we don't have a spatial value, check for something from our type constructors
          (if-let [constructor ^TypeConstructor (get class-constructors (.getTypeName javaClass))]
            (.cast javaClass (.constructType constructor (.getLiteral literal)))
            (throw (ClassCastException. (str "Unsupported cast of: " javaClass))))))))
  )

(defn make-type-converter
  [df ^Map owl-to-java-map ^Map java-to-owl-map]
  (->ClojureTypeConverter
    df
    ; We have to convert our HashMaps into Associated maps
    (zipmap (.keySet owl-to-java-map) (.values owl-to-java-map))
    (zipmap (.keySet java-to-owl-map) (.values java-to-owl-map))
    {}))
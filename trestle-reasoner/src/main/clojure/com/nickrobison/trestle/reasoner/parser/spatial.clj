(ns com.nickrobison.trestle.reasoner.parser.spatial
  (:import (org.opengis.referencing NoSuchAuthorityCodeException)
           (com.nickrobison.trestle.reasoner.exceptions InvalidClassException InvalidClassException$State)
           (org.geotools.referencing CRS)
           (org.semanticweb.owlapi.model OWLDataFactory OWLLiteral)
           (com.nickrobison.trestle.common StaticIRI CommonSpatialUtils)
           (com.nickrobison.trestle.reasoner.parser TypeUtils)))

; More state in the file?
(def crs-uri-map (atom {}))

(defmulti wkt-to-geom
          "Convert a WKT representation to the underlying javaClass
          We can't use Protocols for this, because we can't call them on classes,
          so we have to switch on the class type using multi-dispatch"
          (fn [type ^String wkt] type))
; For string values, which might be spatial
(defmethod wkt-to-geom String
  [_ wkt]
  ; If we can match against the WKT literal, return that.
  ; Otherwise, return nil
  (if-let [matches (re-matches CommonSpatialUtils/wktRegex wkt)]
    (nth matches 2)
    nil))
; Default method for non spatial values
(defmethod wkt-to-geom :default
  [_ _] nil)

(defprotocol SpatialParserProtocol
  "Protocol for registering various spatial object models"
  (wkt-from-geom ^String [spatialObject] "Get the WKT representation from the spatial object"))

(defn validate-spatial-projection
  "Validate the given Projection to make sure it matches something we can understand"
  [projection]
  (let [crs (str "EPSG:" projection)]
    (try
      ; Try to decode the CRS, we don't care about the value, just whether or not it excepts
      (let [_ (CRS/decode crs)]
        projection)
      (catch NoSuchAuthorityCodeException _
        (throw (InvalidClassException.
                 "This Class"
                 InvalidClassException$State/INVALID
                 "Spatial"))))))

(defn projection-to-uri
  "Convert an EPSG projection into an opengis URI"
  [projection]
  (if-let [uri (get @crs-uri-map projection)]
    uri
    (let [uri (str "http://www.opengis.net/def/crs/EPSG/8.9.2/" projection)]
      (do
        (swap! crs-uri-map assoc projection uri)
        uri))))

(defn build-projected-wkt
  "Append the CRS projection URI to the WKT string"
  ^String
  [uri wkt]
  (str "<" uri "> " wkt))

(defn split-wkt
  "Split a WKT into its SRID and WKT Literal"
  [wkt]
  (let [groups (re-find (re-matcher CommonSpatialUtils/wktRegex wkt))]
    {
     :srid (Integer/parseInt (nth groups 1))
     :wkt  (nth groups 2)
     }))

(extend-type String
  SpatialParserProtocol
  (wkt-from-geom [spatialObject]
    spatialObject))

(defn literal-is-spatial?
  "Is the literal value a Spatial value? Based on its datatype"
  [^OWLDataFactory df ^OWLLiteral literal]
  (= (.getDatatype literal) (.getOWLDatatype df StaticIRI/WKTDatatypeIRI)))

(defn object-to-projected-wkt
  "Takes a spatial object and returns the projected WKT form"
  ^String
  [spatialObject srid]
  (build-projected-wkt
    (projection-to-uri srid)
    (wkt-from-geom spatialObject)))

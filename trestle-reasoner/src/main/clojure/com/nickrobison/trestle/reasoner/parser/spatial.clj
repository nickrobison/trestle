(ns com.nickrobison.trestle.reasoner.parser.spatial
  (:import (org.opengis.referencing NoSuchAuthorityCodeException)
           (com.nickrobison.trestle.reasoner.exceptions InvalidClassException InvalidClassException$State)
           (org.geotools.referencing CRS)
           (org.semanticweb.owlapi.model OWLDataFactory)
           (com.nickrobison.trestle.common StaticIRI)))

(defmulti wkt-to-geom
          "Convert a WKT representation to the underlying javaClass
          We can't use Protocols for this, because we can't call them on classes,
          so we have to switch on the class type using multi-dispatch"
          (fn [type ^String wkt] type))
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
        crs)
      (catch NoSuchAuthorityCodeException _
        (throw (InvalidClassException.
                 "This Class"
                 InvalidClassException$State/INVALID
                 "Spatial"))))))

(extend-type java.lang.String
  SpatialParserProtocol
  (wkt-from-geom [spatialObject] spatialObject)
  (literal-from-geom [spatialObject ^OWLDataFactory df] (.getOWLLiteral df
                                                                        spatialObject
                                                                        (.getOWLDatatype df StaticIRI/WKTDatatypeIRI))))

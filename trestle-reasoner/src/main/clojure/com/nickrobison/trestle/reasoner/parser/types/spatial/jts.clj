(ns com.nickrobison.trestle.reasoner.parser.types.spatial.jts
  (:require [com.nickrobison.trestle.reasoner.parser.spatial :refer [SpatialParserProtocol wkt-to-geom split-wkt reproject]]
            [clojure.tools.logging :as log])
  (:import (org.semanticweb.owlapi.model OWLDataFactory)
           (com.nickrobison.trestle.common StaticIRI)
           (org.locationtech.jts.io WKTWriter WKTReader)
           (org.locationtech.jts.geom Geometry PrecisionModel GeometryFactory)
           (org.geotools.referencing CRS)
           (org.geotools.geometry.jts JTS)
           (org.opengis.referencing.operation MathTransform)))

; I'm not entirely sure this is the best way to manage sharing these objects.
; But seems ok for now.
(def transformations (atom {}))

(defn- create-factory
  "Create Geometry factory with the default precision and the given SRID"
  [srid]
  (GeometryFactory. (PrecisionModel.) srid))

(defn get-transformation
  "Gets a transformation between two SRIDs"
  ^MathTransform
  [source target]
  (let [sourceCRS (CRS/decode (str "EPSG:" source) true)
        targetCRS (CRS/decode (str "EPSG:" target) true)]
    ^MathTransform
    (CRS/findMathTransform sourceCRS targetCRS)))

(defn- get-reader
  "Simple abstraction class for getting a cached WKT Reader from the atom"
  ^WKTReader
  [srid]
  (WKTReader. (create-factory srid)))

(extend-type Geometry
  SpatialParserProtocol
  (wkt-from-geom [spatialObject] (.write (WKTWriter.) spatialObject))
  (wkt-from-geom [spatialObject sourceSRID]
    (let [srid (.getSRID spatialObject)]
      ; If the SRID is 0, use the provided srid, otherwise, use the one from the geom

      (.write (WKTWriter.) (if (= 0 srid)
                       (JTS/transform spatialObject (get-transformation sourceSRID 4326))
                       (JTS/transform spatialObject (get-transformation srid 4326))
                       ))))

   (reproject [spatialObject srid]
     (let [transformed (JTS/transform spatialObject
                                      (get-transformation
                                        (.getSRID spatialObject) srid))]
       (do
         (.setSRID transformed srid)
         transformed))))

(defmethod wkt-to-geom Geometry
  [_ ^String wkt]
  (let [wktProperties (split-wkt wkt)]
    (.read (get-reader 4326) ^String (:wkt wktProperties))))
    ;(.read (get-reader ^Integer (:srid wktProperties)) ^String (:wkt wktProperties))))

(ns com.nickrobison.trestle.reasoner.parser.types.spatial.jts
  (:require [com.nickrobison.trestle.reasoner.parser.spatial :refer [SpatialParserProtocol wkt-to-geom split-wkt]]
            [clojure.tools.logging :as log])
  (:import (org.semanticweb.owlapi.model OWLDataFactory)
           (com.nickrobison.trestle.common StaticIRI)
           (com.vividsolutions.jts.io WKTWriter WKTReader)
           (com.vividsolutions.jts.geom Geometry PrecisionModel GeometryFactory)))

; I'm not entirely sure this is the best way to manage sharing these objects.
; But seems ok for now.
(def ^WKTWriter writer (WKTWriter.))
(def readers (atom {}))

(defn- create-factory
  "Create Geometry factory with the default precision and the given SRID"
  [srid]
  (GeometryFactory. (PrecisionModel.) srid))

(defn- get-reader
  "Simple abstraction class for getting a cached WKT Reader from the atom"
  ^WKTReader
  [srid]
  (if-let [reader (get @readers srid)]
    reader
    (let [factory (create-factory srid)
          newReader (WKTReader. factory)]
      (do
        (log/debugf "Creating new reader for CRS %s", srid)
        (swap! readers assoc srid newReader)
        newReader))))

(extend-type Geometry
  SpatialParserProtocol
  (wkt-from-geom [spatialObject] (.write writer spatialObject)))

(defmethod wkt-to-geom Geometry
  [_ ^String wkt]
  (let [wktProperties (split-wkt wkt)]
    (.read (get-reader ^Integer (:srid wktProperties)) ^String (:wkt wktProperties))))

(ns com.nickrobison.trestle.reasoner.parser.types.spatial.jts
  (:require [com.nickrobison.trestle.reasoner.parser.spatial :refer [SpatialParserProtocol wkt-to-geom]])
  (:import (org.semanticweb.owlapi.model OWLDataFactory)
           (com.nickrobison.trestle.common StaticIRI)
           (com.vividsolutions.jts.io WKTWriter WKTReader)
           (com.vividsolutions.jts.geom Geometry)))

(extend-type Geometry
  SpatialParserProtocol
  (wkt-from-geom [spatialObject] (.write (WKTWriter.) spatialObject)))

(defmethod wkt-to-geom Geometry
  [_ ^String wkt]
  (.read (WKTReader.) wkt))

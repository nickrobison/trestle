(ns com.nickrobison.trestle.reasoner.parser.types.spatial.esri
  (:require [com.nickrobison.trestle.reasoner.parser.spatial :refer [SpatialParserProtocol]])
  (:import (com.esri.core.geometry Geometry GeometryEngine Polygon Geometry$Type Polyline Point MultiPoint Line Envelope)
           (com.nickrobison.trestle.common StaticIRI)
           (org.semanticweb.owlapi.model OWLDataFactory)))


(defn- to-wkt
  "Parse ESRI geom to WKT"
  ^String
  [object]
  (GeometryEngine/geometryToWkt object 0))

(defn- from-wkt
  [wkt geomClass]
  (GeometryEngine/geometryFromWkt wkt 0 geomClass))

(defn- to-literal
  [spatialObject df]
  (.getOWLLiteral df
                  (to-wkt spatialObject)
                  (.getOWLDatatype df StaticIRI/WKTDatatypeIRI)))

; I think technically we could get away with not explicitly overloading every subclass of Geometry,
; but at least this way we can do the WKT -> geom process without a static map. So that's nice
(extend-protocol SpatialParserProtocol
  Polygon
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (to-literal spatialObject df))
  (wkt-to-geom [wkt] (from-wkt wkt Geometry$Type/Polygon))
  Envelope
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (to-literal spatialObject df))
  (wkt-to-geom [wkt] (from-wkt wkt Geometry$Type/Envelope))
  Polyline
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (to-literal spatialObject df))
  (wkt-to-geom [wkt] (from-wkt wkt Geometry$Type/Polyline))
  Line
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (to-literal spatialObject df))
  (wkt-to-geom [wkt] (from-wkt wkt Geometry$Type/Line))
  MultiPoint
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (to-literal spatialObject df))
  (wkt-to-geom [wkt] (from-wkt wkt Geometry$Type/MultiPoint))
  Point
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (to-literal spatialObject df))
  (wkt-to-geom [wkt] (from-wkt wkt Geometry$Type/Point))
  )

(extend-type Geometry
  SpatialParserProtocol
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (.getOWLLiteral df
                                                                        (to-wkt spatialObject)
                                                                        (.getOWLDatatype df StaticIRI/WKTDatatypeIRI)))
  (wkt-to-geom [wkt geomClass] (from-wkt wkt geomClass)))

(ns com.nickrobison.trestle.reasoner.parser.types.spatial.esri
  (:require [com.nickrobison.trestle.reasoner.parser.spatial :refer [SpatialParserProtocol wkt-to-geom]])
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
  [spatialObject ^OWLDataFactory df]
  (.getOWLLiteral df
                  (to-wkt spatialObject)
                  (.getOWLDatatype df StaticIRI/WKTDatatypeIRI)))

(defmethod wkt-to-geom Polygon
          [_ wkt] (from-wkt wkt Geometry$Type/Polygon))
(defmethod wkt-to-geom Envelope
  [_ wkt] (from-wkt wkt Geometry$Type/Envelope))
(defmethod wkt-to-geom Polyline
  [_ wkt] (from-wkt wkt Geometry$Type/Polyline))
(defmethod wkt-to-geom Line
  [_ wkt] (from-wkt wkt Geometry$Type/Line))
(defmethod wkt-to-geom MultiPoint
  [_ wkt] (from-wkt wkt Geometry$Type/MultiPoint))
(defmethod wkt-to-geom Point
  [_ wkt] (from-wkt wkt Geometry$Type/Point))

(extend-type Geometry
  SpatialParserProtocol
  (wkt-from-geom [spatialObject] (to-wkt spatialObject))
  (literal-from-geom [spatialObject ^OWLDataFactory df] (to-literal spatialObject df))
  (wkt-to-geom [wkt geomClass] (from-wkt wkt geomClass)))

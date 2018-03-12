(ns com.nickrobison.trestle.reasoner.parser.types.spatial.esri
  (:require [com.nickrobison.trestle.reasoner.parser.spatial :refer [SpatialParserProtocol wkt-to-geom split-wkt]])
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
  (let [wktProperties (split-wkt wkt)]
    (GeometryEngine/geometryFromWkt (:wkt wktProperties) 0 geomClass)))

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
  (wkt-from-geom [spatialObject srid]
    (to-wkt spatialObject))
  (reproject [spatialObject srid]
    spatialObject))

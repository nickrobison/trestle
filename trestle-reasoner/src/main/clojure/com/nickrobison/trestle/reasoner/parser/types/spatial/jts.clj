(ns com.nickrobison.trestle.reasoner.parser.types.spatial.jts
  (:require [com.nickrobison.trestle.reasoner.parser.spatial :refer [SpatialParserProtocol]])
  (:import (org.semanticweb.owlapi.model OWLDataFactory)
           (com.nickrobison.trestle.common StaticIRI)
           (com.vividsolutions.jts.io WKTWriter)
           (com.vividsolutions.jts.geom Geometry)))

(extend-type Geometry
  SpatialParserProtocol
  (literal-from-geom [spatialObject ^OWLDataFactory df] (.getOWLLiteral df
                                                                     (.write (WKTWriter.) spatialObject)
                                                                        (.getOWLDatatype df StaticIRI/WKTDatatypeIRI)))
  )

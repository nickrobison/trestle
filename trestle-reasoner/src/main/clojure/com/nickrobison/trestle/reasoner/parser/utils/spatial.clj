(ns com.nickrobison.trestle.reasoner.parser.utils.spatial
  (:import (org.opengis.referencing NoSuchAuthorityCodeException)
           (com.nickrobison.trestle.reasoner.exceptions InvalidClassException InvalidClassException$State)
           (org.geotools.referencing CRS)))

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

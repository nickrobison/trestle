(ns trestle.compiler.core
  (:require [clojure.spec.alpha :as s]))

(s/def
  ::version
  (s/and number? #(>= 1 %) pos?))

(s/def ::dataset-name string?)

(s/def ::name string?)
(s/def ::type string?)
(s/def ::spatial boolean?)
(s/def ::identifier boolean?)

(s/def ::fact
  (s/keys :req-un [ ::name
                   ::type
                   (or ::spatial ::identifier)]))

(s/def ::facts (s/coll-of ::fact))

(s/def ::dataset
  (s/keys :req-un [::version ::name ::facts]))
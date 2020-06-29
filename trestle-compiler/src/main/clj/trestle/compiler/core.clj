(ns trestle.compiler.core
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as c]))

(s/def
  ::version
  (s/and pos-int? #(>= 1 %)))

(s/def ::dataset string?)
(s/def ::name string?)
(s/def ::type string?)
(s/def ::spatial boolean?)
(s/def ::identifier boolean?)
(s/def ::temporal string?)

; This isn't working, I want to enforce that we can only have a single qualifier on each fact
; Not sure how to do that
; I'd also like to ensure we have a maximum of 1 ID or spatial value
(s/def ::fact-qualifiers (s/or :spatial ::spatial :identifier ::identifier :temporal ::temporal))

;(s/def ::fact-qualifiers (s/or
;                           (s/keys :req-un [:spatial ::spatial])
;                           (s/keys :req-un [:identifier ::identifier])
;                           (s/keys :req-un [:temporal ::temporal])))

(s/def ::fact-params (s/keys
                       :req-un [::name ::type]
                       :opt-un [::fact-qualifiers]))

(s/def ::fact (s/merge ::fact-qualifiers ::fact-params))

(s/def ::facts (s/coll-of ::fact))

(s/def :dataset/spec
  (s/keys :req-un [::version ::dataset ::facts]))

(defn parse-json [file]
  (c/parse-stream (clojure.java.io/reader file) true))

(s/fdef parse-json
        :args (s/cat :file string?)
        :ret ::dataset)
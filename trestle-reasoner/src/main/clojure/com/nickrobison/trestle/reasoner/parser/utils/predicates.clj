(ns com.nickrobison.trestle.reasoner.parser.utils.predicates
  (:import (java.lang.reflect Modifier Method Field)
           (com.nickrobison.trestle.reasoner.annotations Ignore Fact Spatial Language NoMultiLanguage IndividualIdentifier)
           (org.semanticweb.owlapi.model IRI)
           (com.nickrobison.trestle.common StaticIRI))
  (:require [clojure.string :as string]
            [clojure.core.match :refer [match]]))

(keyword 'field)
(keyword 'method)
(keyword 'spatial)
(keyword 'identifier)
(keyword 'language)

(defn get-annotation
  "Get the specified annotation on the class member"
  [member annotation]
  (.getAnnotation member annotation))

(defn get-member-name [member] (.getName member))

(defn hasAnnotation? [member annotation] (.isAnnotationPresent member annotation))
(defn public? [entity] (Modifier/isPublic (.getModifiers entity)))
(defn ignore? [entity] (hasAnnotation? entity Ignore))
(defn fact? [entity] (hasAnnotation? entity Fact))
(defn spatial? [entity] (hasAnnotation? entity Spatial))
(defn language? [entity] (hasAnnotation? entity Language))
(defn noMultiLanguage? [entity] (hasAnnotation? entity NoMultiLanguage))
(defn identifier? [entity] (hasAnnotation? entity IndividualIdentifier))
(defn noAnnotations? [entity] (= (count (.getAnnotations entity)) 0))
(defn ebean? [entity] (string/includes? (.getName entity) "_ebean"))
(defn noParams? [entity] (= (.getParameterCount entity) 0))
(defn internal?
  "Are the methods internal methods? e.g. hashCode, Equals, etc"
  [entity] (or
             (= (get-member-name entity) "hashCode")
             (= (get-member-name entity) "equals")
             (= (get-member-name entity) "toString")
             (= (get-member-name entity) "wait")
             (= (get-member-name entity) "notify")
             (= (get-member-name entity) "notifyAll")
             (= (get-member-name entity) "getClass")))
(defn void-return? [entity] (= (.getReturnType entity) Void))
(defn include? [entity] ((some-fn fact?
                                  spatial?
                                  identifier?
                                  language?
                                  noMultiLanguage?
                                  noAnnotations?) entity))

(defn member-type [member] (let [
                                 s (spatial? member)
                                 l (language? member)
                                 i (identifier? member)
                                 f (include? member)]
                             (match [s l i f]
                                    [true _ _ _] ::spatial
                                    [_ true _ _] ::language
                                    [_ _ true _] ::identifier
                                    :else ::fact)))


(defmulti filter-member
          "Filter Class Member to only include the ones we need"
          class)
(defmethod filter-member Field [field]
  ((every-pred
     (complement ignore?)
     public?
     include?
     (complement ebean?)) field))
(defmethod filter-member Method [method]
  ((every-pred
     (complement ignore?)
     public?
     include?
     noParams?
     (complement internal?)
     (complement ebean?)
     (complement void-return?)) method))

; Filter member name to strip out unwanted characters
(defmulti filter-member-name
          "Filter name of class member"
          class)
(defmethod filter-member-name Field [field]
  ; Just return the field name
  (.getName field))
(defmethod filter-member-name Method [method]
  (let [name (.getName method)]
    (if (string/starts-with? name "get")
      ; Strip off the get and lower case the first letter
      (str (string/lower-case (subs name 3 4)) (subs name 4))
      ; If not, just return the name
      name)))
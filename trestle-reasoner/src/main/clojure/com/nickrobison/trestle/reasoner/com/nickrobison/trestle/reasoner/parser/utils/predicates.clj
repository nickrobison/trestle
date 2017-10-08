(ns com.nickrobison.trestle.reasoner.com.nickrobison.trestle.reasoner.parser.utils.predicates
  (:import (java.lang.reflect Modifier Method Field)
           (com.nickrobison.trestle.reasoner.annotations Ignore Fact Spatial Language NoMultiLanguage IndividualIdentifier))
  (:require [clojure.string :as string]
            [clojure.core.match :refer [match]]))

(keyword 'field)
(keyword 'method)
(keyword 'spatial)
(keyword 'identifier)
(keyword 'language)


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
                              i (identifier? member)]
                          (match [s l i]
                                 [true _ _] ::spatial
                                 [_ true _] ::language
                                 [_ _ true] ::identifier
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

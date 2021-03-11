(ns com.nickrobison.trestle.reasoner.parser.utils.predicates
  (:import (java.lang.reflect Modifier Method Field Constructor)
           (com.nickrobison.trestle.reasoner.annotations Ignore Fact Spatial Language NoMultiLanguage IndividualIdentifier TrestleCreator Related ObjectProperty)
           (com.nickrobison.trestle.reasoner.annotations.temporal DefaultTemporal StartTemporal EndTemporal)
           (com.nickrobison.trestle.types TemporalType))
  (:require [clojure.string :as string]
            [clojure.core.match :refer [match]]))
; We can disable reflection warning, because Reflection is what we want
(set! *warn-on-reflection* false)

(keyword 'field)
(keyword 'method)
(keyword 'spatial)
(keyword 'identifier)
(keyword 'language)
(keyword 'temporal)
(keyword 'start)
(keyword 'end)
(keyword 'at)
(keyword 'default)
(keyword 'point)
(keyword 'interval)

(defn get-annotation
  "Get the specified annotation on the class member"
  [member annotation]
  ; Reflection is ok here
  (.getAnnotation member annotation))

(defn get-member-name
  "Get name of class member"
  [member]
  ; Reflection is ok here
  (.getName member))

(defn get-temporal-type
  "Get the TemporalType of the member"
  [member]
  (if-let [default ^DefaultTemporal (get-annotation member DefaultTemporal)]
    (if (= (.type default) (TemporalType/POINT))
      ::point
      ::interval)
    ::interval)
  )

(defmulti get-member-return-type
          "Get the Java class return type of the field/method"
          class)
(defmethod get-member-return-type Field [^Field field] (.getType field))
(defmethod get-member-return-type Method [^Method method] (.getReturnType method))

(defn hasAnnotation?
  "Does the class member have the specified annotation?"
  [member annotation]
  ; Reflection is ok here
  (.isAnnotationPresent member annotation))
(defn public?
  "Is the class member marked as public?"
  [entity]
  (Modifier/isPublic (.getModifiers entity)))
(defn ignore? [entity] (hasAnnotation? entity Ignore))
(defn fact? [entity] (hasAnnotation? entity Fact))
(defn spatial? [entity] (hasAnnotation? entity Spatial))
(defn language? [entity] (hasAnnotation? entity Language))
(defn property? [entity] (hasAnnotation? entity ObjectProperty))
(defn noMultiLanguage? [entity] (hasAnnotation? entity NoMultiLanguage))
(defn identifier? [entity] (hasAnnotation? entity IndividualIdentifier))
(defn temporal? [entity] (or
                           (hasAnnotation? entity DefaultTemporal)
                           (hasAnnotation? entity StartTemporal)
                           (hasAnnotation? entity EndTemporal)))
(defn isDefault? [entity] (hasAnnotation? entity DefaultTemporal))
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
(defn related? [entity] (hasAnnotation? entity Related))
(defn void-return? [entity] (= (.getReturnType entity) Void))
(defn string-return? [entity] (= (get-member-return-type entity) String))
(defn include? [entity] ((some-fn fact?
                                  spatial?
                                  identifier?
                                  language?
                                  temporal?
                                  related?
                                  property?
                                  noMultiLanguage?
                                  noAnnotations?) entity))

(defn ignore-member? [entity]
  "Ignore the member if it's marked as ignore and is not spatial or identifier"
  ((every-pred
     ignore?
     (or spatial? identifier?))
   entity))

(defn member-type [member defaultLanguageCode]
  (let [
        s (spatial? member)
        ; Is a language code if is annotated as language
        ; But only if it's a string type
        l (and (string-return? member)
               (language? member))
        i (identifier? member)
        t (temporal? member)
        p (property? member)]
    (match [s t i l p]
           [true _ _ _ _] ::spatial
           [_ true _ _ _] ::temporal
           [_ _ true _ _] ::identifier
           [_ _ _ true _] ::language
           [_ _ _ _ true] ::property
           :else ::fact)))


(defmulti filter-member
          "Filter Class Member to only include the ones we need"
          class)
(defmethod filter-member Field [field]
  ((every-pred
     (complement ignore-member?)
     public?
     include?
     (complement ebean?)) field))
(defmethod filter-member Method [method]
  ((every-pred
     (complement ignore-member?)
     public?
     include?
     noParams?
     (complement internal?)
     (complement ebean?)
     (complement void-return?)) method))

(defn get-common-annotation
  "We use a common annotation type to normalize handling facts, spatials and temporals.
  This allows us to return that underlying annotation and do things with it."
  [member annotation]
  (->> (.getDeclaredAnnotations member)
       (filter #(.isAnnotationPresent (.annotationType %) annotation))
       (first)))

; Temporal utils
(defn get-temporal-position
  "Are we start or end temporal?"
  [member]
  (if (or
        (and
          (hasAnnotation? member DefaultTemporal)
          (= (.type (get-annotation member DefaultTemporal)) (TemporalType/INTERVAL)))
        (hasAnnotation? member StartTemporal))
    ::start
    (if (hasAnnotation? member EndTemporal)
      ::end
      ::at)))

; Constructor filters
(defn trestle-creator?
  "Is this constructor the TrestleCreator"
  [constructor]
  (hasAnnotation? constructor TrestleCreator))

(defn multi-arg-constructor?
  "Is this constructor a multi-arg one?"
  [^Constructor constructor]
  (> (.getParameterCount constructor) 0))

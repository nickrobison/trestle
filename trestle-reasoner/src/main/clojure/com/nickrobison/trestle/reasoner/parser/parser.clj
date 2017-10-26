(ns com.nickrobison.trestle.reasoner.parser.parser
  (:import [IClassParser]
           (com.nickrobison.trestle.reasoner.parser IClassParser TypeConverter StringParser)
           (org.semanticweb.owlapi.model IRI OWLClass OWLDataFactory OWLNamedIndividual OWLDataPropertyAssertionAxiom)
           (java.lang.annotation Annotation)
           (java.lang.reflect InvocationTargetException Field Method Modifier)
           (java.lang.invoke MethodHandles)
           (com.nickrobison.trestle.reasoner.annotations IndividualIdentifier DatasetClass Fact NoMultiLanguage Language)
           (java.util Optional List))
  (:require [clojure.core.match :refer [match]]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [com.nickrobison.trestle.reasoner.parser.utils.predicates :as pred]
            [com.nickrobison.trestle.reasoner.parser.utils.members :as m]))




(defn accessMethodValue [classMethod inputObject]
  (. (TypeConverter/parsePrimitiveClass (.getReturnType classMethod)) cast
     (try
       (log/trace (str "Attempting to invoke on: " (.getName classMethod)))
       (.invoke classMethod inputObject nil)
       (catch IllegalAccessError e
         (log/error "Cannot access method " classMethod e))
       (catch InvocationTargetException e
         (log/error "Invocation failed on" classMethod e)))))


(defn invoker
  "Invoke method handle"
  ; We need to use invokeWithArguments to work around IDEA-154967
  ([handle object]
   (try
     (log/debugf "Invoking method handle %s on %s" handle object)
     (.invokeWithArguments handle (object-array [object]))
     (catch Exception e
       (log/error "Problem invoking" e))))
  ([handle object & args]
   (try
     (log/debugf "Invoking method handle %s on %s with args %s"
                 handle
                 object
                 args)
     (.invokeWithArguments handle (object-array [object args]))
     (catch Exception e
       (log/error "Problem invoking %s on %s" handle object e)))))

; Method Handle methods
(defmulti make-handle
          "Make a method handle for a given Class Member"
          class)
(defmethod make-handle Field [field] (.unreflectGetter (MethodHandles/lookup) field))
(defmethod make-handle Method [method] (.unreflect (MethodHandles/lookup) method))

(defmulti get-member-return-type
          "Get the Java class return type of the field/method"
          class)
(defmethod get-member-return-type Field [field] (.getType field))
(defmethod get-member-return-type Method [method] (.getReturnType method))

(defn parse-return-type
  "Determine the OWLDatatype of the field/method"
  [member]
  (if (pred/hasAnnotation? member Fact)
    (TypeConverter/getDatatypeFromAnnotation (pred/get-annotation member Fact) (get-member-return-type member))
    (TypeConverter/getDatatypeFromJavaClass (get-member-return-type member))))

(defn get-member-language
  "Get the language tag, if one exists"
  [member defaultLang]
  (if (pred/hasAnnotation? member Language)
    (.language (pred/get-annotation member Language))
    defaultLang))

(defn build-member
  "Build member from class methods/fields"
  [member df prefix defaultLang]
  (let [iri (m/build-iri member prefix)]
    {
     :name          (pred/filter-member-name member)
     :member-name   (pred/get-member-name member)
     :iri           iri
     :data-property (.getOWLDataProperty df iri)
     :return-type   (parse-return-type member)
     :language      (if (complement (nil? defaultLang))
                      (get-member-language member defaultLang))
     :handle        (make-handle member)
     :type          (pred/member-type member)
     :ignore        (pred/ignore? member)}))

(defmulti build-literal (fn [df value multiLangEnabled
                             defaultLang returnType] (class value)))
(defmethod build-literal String
  [df value multiLangEnabled defaultLang returnType]
  (.getOWLLiteral df value defaultLang))
(defmethod build-literal :default
  [df value multiLangEnabled defaultLang returnType]
  (.getOWLLiteral df (.toString value) returnType))


(defn member-reducer
  "Build the Class Member Map"
  [acc member]
  (let [members (get acc :members)
        temporals (get acc :temporals [])
        type (get member :type)
        ignore (get member :ignore)]
    (match [type, ignore]
           [::pred/identifier, true] (assoc acc :identifier member)
           [::pred/identifier, false] (merge acc {:members (conj members member) :identifier member})
           [::pred/spatial, _] (merge acc {:members (conj members member) :spatial member})
           [::pred/temporal, _] (merge acc {:temporals (conj temporals member)})
           :else (assoc acc :members (conj members member)))))


(defrecord ClojureClassParser [^OWLDataFactory df,
                               ^String reasonerPrefix,
                               ^boolean multiLangEnabled,
                               ^String defaultLanguageCode
                               classRegistry]
  IClassParser
  (isMultiLangEnabled [this] (multiLangEnabled))
  (getDefaultLanguageCode [this] (if (= defaultLanguageCode "") nil defaultLanguageCode))
  (^OWLClass getObjectClass [this ^Class clazz] (. df getOWLClass
                                                   (IRI/create reasonerPrefix
                                                               (if (.isAnnotationPresent clazz DatasetClass)
                                                                 (.name (.getDeclaredAnnotation clazz DatasetClass))
                                                                 (.getName clazz)))))
  (^OWLClass getObjectClass [this ^Object inputObject] (.getObjectClass this (.getClass inputObject)))
  (parseClass ^Object [this ^Class clazz]
    ; Check to see if we have the class in the registry
    (if (contains? @classRegistry clazz)
      (log/spyf :debug "Getting from cache"
                ; Get the class from the registry atom
                (get @classRegistry clazz))
      (log/spyf :debug "Parsing test class"
                ; Get the class from the newly updated atom
                (get
                  ; Update the atom with the new class record
                  (swap! classRegistry
                         assoc
                         clazz
                         ; Build the class record
                         (->> (concat (.getDeclaredFields clazz) (.getDeclaredMethods clazz))
                              ; Filter out non-necessary members
                              (r/filter pred/filter-member)
                              ; Build members
                              (r/map #(build-member % df reasonerPrefix (if (true? multiLangEnabled) defaultLanguageCode nil)))
                              ; Combine everything into a map
                              (r/reduce member-reducer {})))
                  clazz))))

  (^OWLNamedIndividual getIndividual [this ^Object inputObject]
    (let [parsedClass (.parseClass this (.getClass inputObject))]
      (.getOWLNamedIndividual df
                              (IRI/create reasonerPrefix
                                          (m/normalize-id (invoker (get
                                                                     (get parsedClass :identifier)
                                                                     :handle)
                                                                   inputObject))))))


  (getFacts ^Optional ^List ^OWLDataPropertyAssertionAxiom [this inputObject filterSpatial]
    (let [parsedClass (.parseClass this (.getClass inputObject))
          individual (.getIndividual this inputObject)]
      (Optional/of
        (->> (get parsedClass :members)
             ; Filter spatial
             (r/filter (fn [member]
                         (if (true? filterSpatial)
                           (complement (= (get member :type) ::pred/spatial))
                           true)))
             ; Build the assertion axiom
             (r/map (fn [member]
                      (.getOWLDataPropertyAssertionAxiom df
                                                         (get member :data-property)
                                                         individual
                                                         (build-literal df
                                                                        (invoker (get member :handle) inputObject)
                                                                        multiLangEnabled
                                                                        defaultLanguageCode
                                                                        (get member :return-type)))))
             (into ())))))
  (getFacts ^Optional ^List ^OWLDataPropertyAssertionAxiom [this inputObject]
    (.getFacts this inputObject false))

  (matchWithClassMember ^String [this clazz classMember]
    (let [parsedClass (.parseClass this clazz)]
      (->> (get parsedClass :members)
           (r/filter (fn [member]
                       (let [iri (.getShortForm (get member :iri))]
                         (= iri classMember))))
           (r/map (fn [member]
                    (get member :member-name)))))))

(defn make-parser
  "Creates a new ClassParser"
  [df reasonerPrefix multiLangEnabled defaultLanguageCode]
  (->ClojureClassParser df reasonerPrefix multiLangEnabled defaultLanguageCode (atom {})))

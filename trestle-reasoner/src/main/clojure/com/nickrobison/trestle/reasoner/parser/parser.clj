(ns com.nickrobison.trestle.reasoner.parser.parser
  (:import [IClassParser]
           (com.nickrobison.trestle.reasoner.parser IClassParser TypeConverter StringParser ClassBuilder)
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

(defn owl-return-type
  "Determine the OWLDatatype of the field/method"
  [member rtype]
  (log/warn "Getting return type")
  (if (pred/hasAnnotation? member Fact)
    (TypeConverter/getDatatypeFromAnnotation (pred/get-annotation member Fact) rtype)
    (TypeConverter/getDatatypeFromJavaClass rtype)))

(defn get-member-language
  "Get the language tag, if one exists"
  [member defaultLang]
  (if (pred/hasAnnotation? member Language)
    (.language (pred/get-annotation member Language))
    defaultLang))

(defn build-member-return-type
  [acc member df]
  (let [rtype (get-member-return-type member)]
    (merge acc {
                :return-type  rtype
                :owl-datatype (owl-return-type member rtype)
                })))

(defn build-member-map
  [member fns]
  (reduce (fn [acc f] (f acc member)) {} fns))

(defn default-member-keys
  [acc member]
  (merge acc {
              :name        (pred/filter-member-name member)
              :member-name (pred/get-member-name member)
              :handle      (make-handle member)
              :type        (pred/member-type member)
              }))

(defn ignore-fact
  [acc member]
  (let [ignore (pred/ignore? member)]
    (if (true? ignore) (merge acc {:ignore true})
                       acc)))

(defn build-multi-lang
  "If the return type is a string, check for multi-lang"
  [acc member lang]
  (let [rtype (get acc :return-type
                   (get-member-return-type member))]
    (log/debugf "Called with lang type %s" rtype)
    (if (and (complement (nil? lang)) (= String rtype))
      (merge acc {:language (get-member-language member lang)})
      acc)))

(defn build-temporal
  "If we're a temporal, get the necessary properties"
  [acc member]
  (let [type (get acc :type)]
    (if (= type ::pred/temporal)
      ; If we're a temporal do things
      (merge acc {
                  ; Set the name, since it's on a different annotation
                  :temporal-type (pred/get-temporal-type member)
                  ;:name          (pred/get-temporal-name member)
                  :position (pred/get-temporal-position member)
                  })
      acc)))

(defn build-member
  "Build member from class methods/fields"
  [member df prefix defaultLang]
  (let [iri (m/build-iri member prefix)]
    (merge (build-member-map member
                             [default-member-keys
                              (fn [acc member]
                                (build-member-return-type acc member df))
                              ignore-fact
                              (fn [acc member]
                                (build-multi-lang acc member defaultLang))
                              build-temporal])
           {
            :iri           iri
            :data-property (.getOWLDataProperty df iri)
            })))





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
        ignore (get member :ignore false)]
    (match [type, ignore]
           [::pred/identifier, true] (assoc acc :identifier member)
           [::pred/identifier, false] (merge acc {:members (conj members member) :identifier member})
           [::pred/spatial, _] (merge acc {:members (conj members member) :spatial member})
           [::pred/temporal, _] (merge acc {:temporals (conj temporals member)})
           :else (assoc acc :members (conj members member)))))

(defn build-assertion-axiom
  "Build OWLDataPropertyAssertionAxiom from member"
  [df individual member inputObject]
  (.getOWLDataPropertyAssertionAxiom df
                                     (get member :data-property)
                                     individual
                                     (build-literal df
                                                    (invoker (get member :handle) inputObject)
                                                    true
                                                    (get member :language)
                                                    (get member :owl-datatype))))

(defmulti member-matches?
          "Match the string IRI name with a member in the given set"
          (fn [member languageCode classMember]
            (:type member)))
(defmethod member-matches? ::pred/temporal
  [member languageCode classMember]
  (let [iri (.getShortForm (get member :iri))
        position (get member :position)]
    (log/warnf "Matching %s against temporals" classMember)
    (or
      ; Can we match directly against the class member?
      (= classMember (get member :name))
      ; If not, try to match against a default name and the position type
      (if (= classMember "intervalStart")
        (= position ::pred/start)
        (if (= classMember "intervalEnd")
          (= position ::pred/end)
          (= position ::pred/at))))))
(defmethod member-matches? ::pred/language
  [member languageCode classMember]
  (let [iri (.getShortForm (get member :iri))]
    (log/debugf "Matching member %s against language %s"
                classMember languageCode)
    ; Match against IRI and language code
    (and (= iri classMember) (= languageCode (get member :language)))))
(defmethod member-matches? :default
  [member languageCode classMember]
  (log/debugf "Matching %s against defaults" classMember)
  (let [iri (.getShortForm (get member :iri))]
    (= iri classMember)))


(defn match-class-member
  "Match the string IRI name with a member in the given set"
  [members languageCode classMember]
  (->> members
       (filter (fn [member]
                 (log/debug "Matching")
                 (member-matches? member languageCode classMember)))
       (map (fn [member]
              (get member :name)))
       (first)))


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
                      (build-assertion-axiom df individual member inputObject)))
             (into ())))))
  (getFacts ^Optional ^List ^OWLDataPropertyAssertionAxiom [this inputObject]
    (.getFacts this inputObject false))

  (getSpatialFact ^Optional ^OWLDataPropertyAssertionAxiom [this inputObject]
    (let [parsedClass (.parseClass this (.getClass inputObject))
          individual (.getIndividual this inputObject)]
      (if-let [spatial (get parsedClass :spatial)]
        (Optional/of (build-assertion-axiom df individual spatial inputObject))
        (Optional/empty))))

  (matchWithClassMember ^String [this clazz classMember]
    (.matchWithClassMember this clazz classMember nil))
  (matchWithClassMember ^String [this clazz classMember languageCode]
    (let [parsedClass (.parseClass this clazz)]
      ; Figure out if we directly match against a constructor argument
      (if (ClassBuilder/isConstructorArgument clazz classMember nil)
        classMember
        (if-some
          ; Try for a classMember first, if it doesn't match, go for temporals
          [classMember (match-class-member (get parsedClass :members)
                                           languageCode classMember)]
          classMember
          ; Lookup temporals
          (match-class-member (get parsedClass :temporals)
                              languageCode classMember)))))

  (getFactDatatype ^Optional [this clazz factName]
    (let [parsedClass (.parseClass this clazz)]
      (Optional/of
        (->> (get parsedClass :members)
             (filter (fn [member]
                       (let [iriString (.toString (get member :iri))]
                         (= iriString factName))))
             (map (fn [member]
                    (get member :return-type)))
             (first))))))

(defn make-parser
  "Creates a new ClassParser"
  [df reasonerPrefix multiLangEnabled defaultLanguageCode]
  (->ClojureClassParser df reasonerPrefix multiLangEnabled defaultLanguageCode (atom {})))

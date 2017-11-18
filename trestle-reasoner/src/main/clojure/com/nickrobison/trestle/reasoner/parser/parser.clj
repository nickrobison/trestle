(ns com.nickrobison.trestle.reasoner.parser.parser
  (:import [IClassParser]
           (com.nickrobison.trestle.reasoner.parser IClassParser TypeConverter StringParser ClassBuilder SpatialParser IClassBuilder IClassRegister)
           (org.semanticweb.owlapi.model IRI OWLClass OWLDataFactory OWLNamedIndividual OWLDataPropertyAssertionAxiom OWLDataProperty OWLLiteral OWLDatatype)
           (java.lang.annotation Annotation)
           (java.lang.reflect InvocationTargetException Field Method Modifier Constructor Parameter)
           (java.lang.invoke MethodHandles MethodHandle)
           (com.nickrobison.trestle.reasoner.annotations IndividualIdentifier DatasetClass Fact NoMultiLanguage Language)
           (java.util Optional List)
           (com.nickrobison.trestle.common StaticIRI)
           (com.nickrobison.trestle.reasoner.exceptions MissingConstructorException InvalidClassException InvalidClassException$State UnregisteredClassException))
  (:require [clojure.core.match :refer [match]]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.set :as set]
            [com.nickrobison.trestle.reasoner.parser.utils.predicates :as pred]
            [com.nickrobison.trestle.reasoner.parser.utils.members :as m])
  (:use clj-fuzzy.metrics))

; Class related helpers
(defn find-matching-constructor
  "Find first constructor that matches the given predicate"
  [^Class clazz predicate]
  (->> (.getDeclaredConstructors clazz)
       (filter predicate)
       (first)))

(defn get-constructor
  "Get the TrestleConstructor for the provided class"
  [clazz]
  ; Look for one that's annotated with TrestleCreator
  (if-let [constructor (find-matching-constructor
                         clazz
                         pred/trestle-creator?)]
    constructor
    ; If we don't have one, look for the first multi-arg constructor
    (if-let [no-arg-constructor (find-matching-constructor clazz
                                                    pred/multi-arg-constructor?)]
      no-arg-constructor
      ; Throw an exception if we can't find the correct constructor
      (throw (MissingConstructorException. "Cannot find TrestleCreator or multi-arg constructor")))))

(defn build-parameter
  [^Parameter parameter]
  {
   :name (.getName parameter)
   :type (.getType parameter)
   })

(defn build-constructor-args
  "Build a list of constructor params and types"
  [^Constructor constructor]
  (->> (.getParameters constructor)
       (map build-parameter)
       (into ())))

(defn build-constructor
  "Build constructor map"
  [clazz]
  (let [constructor (get-constructor clazz)]
    {
     :handle    (m/make-handle constructor)
     :arguments (build-constructor-args constructor)
     }))

(defn fuzzy-match-args
  "If we can't match any of the constructor arguments,
  try to find a close match"
  [parameter arguments]
  (throw (IllegalArgumentException.
           (format "Cannot match %s against Constructor params, perhaps you meant %s"
                   (->> arguments
                        (map #(levenshtein parameter %))
                        (sort-by #(compare %2 %1)))))))

(defn get-class-name
  "Get the OWL Class name"
  [^Class clazz ^OWLDataFactory df reasonerPrefix]
  (.getOWLClass df
     (IRI/create reasonerPrefix
                 (if (.isAnnotationPresent clazz DatasetClass)
                   (.name ^DatasetClass (.getDeclaredAnnotation clazz DatasetClass))
                   (throw (InvalidClassException.
                            (.toString (class DatasetClass))
                            InvalidClassException$State/MISSING))))))

(defn invoker
  "Invoke method handle"
  ; We need to use invokeWithArguments to work around IDEA-154967
  ([^MethodHandle handle object]
   (try
     (log/debugf "Invoking method handle %s on %s" handle object)
     (.invokeWithArguments handle (object-array [object]))
     (catch Exception e
       (log/error "Problem invoking" e))))
  ([^MethodHandle handle object & args]
   (try
     (log/debugf "Invoking method handle %s on %s with args %s"
                 handle
                 object
                 args)
     (.invokeWithArguments handle (object-array [object args]))
     (catch Exception e
       (log/error "Problem invoking %s on %s" handle object e)))))

(defn invoke-constructor
  "Invoke Constructor Method Handle"
  ; We need to use invokeWithArguments to work around IDEA-154967
  ([^MethodHandle handle]
   (try
     (log/debugf "Invoking constructor %s" handle)
     (.invokeWithArguments handle (object-array []))
     (catch Exception e
       (log/error "Problem invoking" e))))
  ([^MethodHandle handle & args]
   (try
     (log/debugf "Invoking constructor %s with args %s" handle args)
     ; I honestly have no idea why we need to do first, but that's how it is
     (.invokeWithArguments handle (object-array (first args)))
     (catch Exception e
       (log/error "Problem invoking" e))))
  )

(defn owl-return-type
  "Determine the OWLDatatype of the field/method"
  [member rtype]
  (if (pred/hasAnnotation? member Fact)
    (TypeConverter/getDatatypeFromAnnotation (pred/get-annotation member Fact) rtype)
    (TypeConverter/getDatatypeFromJavaClass rtype)))

(defn get-member-language
  "Get the language tag, if one exists"
  [member defaultLang]
  (if (pred/hasAnnotation? member Language)
    (.language ^Language (pred/get-annotation member Language))
    defaultLang))

(defmulti build-member-return-type
          "Specialized method to build return type for member"
          (fn [acc member df] (:type acc)))
(defmethod build-member-return-type ::pred/spatial
  [acc member ^OWLDataFactory df]
  (let [rtype (pred/get-member-return-type member)]
    (merge acc {
                :return-type  rtype
                :owl-datatype (.getOWLDatatype df (StaticIRI/WKTDatatypeIRI))
                })))
(defmethod build-member-return-type :default
  [acc member df]
  (let [rtype (pred/get-member-return-type member)]
    (merge acc {
                :return-type  rtype
                :owl-datatype (owl-return-type member rtype)
                })))

(defn build-member-map
  [member fns]
  (reduce (fn [acc f] (f acc member)) {} fns))

(defn default-member-keys
  [acc member defaultLanguageCode]
  (merge acc {
              :name        (pred/filter-member-name member)
              :member-name (pred/get-member-name member)
              :handle      (m/make-handle member)
              :type        (pred/member-type member defaultLanguageCode)
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
                   (pred/get-member-return-type member))]
    (log/debugf "Called with lang type %s" rtype)
    (if (and (complement (nil? lang)) (= String rtype))
      (merge acc {:language (get-member-language member lang)})
      acc)))

(defn build-temporal
  "If we're a temporal, get the necessary properties"
  [acc member]
  (let [type (get acc :type)]
    (if (= type ::pred/temporal)
      (merge acc {
                  :temporal-type (pred/get-temporal-type member)
                  :position      (pred/get-temporal-position member)
                  })
      acc)))

(defn build-member
  "Build member from class methods/fields"
  [member ^OWLDataFactory df prefix defaultLang]
  (let [iri (m/build-iri member prefix)]
    (merge (build-member-map member
                             ; Apply all these transformations to build up the member map
                             [(fn [acc member]
                                (default-member-keys acc member defaultLang))
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

(defn ^OWLLiteral build-literal
  ([^OWLDataFactory df ^String value returnType ^String defaultLang]
   (.getOWLLiteral df value defaultLang))
  ([^OWLDataFactory df ^Object value returnType]
   (.getOWLLiteral df (.toString value) ^OWLDatatype returnType)))

; Validators
(defn validate-member
  "Validate member to make sure it matches everything"
  [member context]
  (build-member-map member
                    ; Individual IRI doesn't have spaces
                    [(fn [acc member]
                       )
                     ])
  )

;(defn validate-class-name
;  [clazz]
;  (if (pred/hasAnnotation? clazz DatasetClass)
;    )
;  )

(defn member-reducer
  "Build the Class Member Map"
  [acc member]
  ; Validate member
  (log/debug "validating")
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

(defmulti build-assertion-axiom
          "Build OWLDataPropertyAssertionAxiom from member"
          (fn [^OWLDataFactory df individual member inputObject] (:type member)))
(defmethod build-assertion-axiom ::pred/spatial
  [^OWLDataFactory df ^OWLNamedIndividual individual member inputObject]
  (let [wktOptional (SpatialParser/parseWKTFromGeom
                      (invoker (get member :handle) inputObject))]
    (if (.isPresent wktOptional)
      (.getOWLDataPropertyAssertionAxiom df
                                         ^OWLDataProperty (get member :data-property)
                                         individual
                                         ^OWLLiteral (.get wktOptional)))))
(defmethod build-assertion-axiom ::pred/language
  [^OWLDataFactory df ^OWLNamedIndividual individual member inputObject]
  (.getOWLDataPropertyAssertionAxiom df
                                     ^OWLDataProperty (get member :data-property)
                                     individual
                                     (build-literal df
                                                    (invoker (get member :handle) inputObject)
                                                    (get member :owl-datatype)
                                                    (get member :language))))
(defmethod build-assertion-axiom :default
  [^OWLDataFactory df ^OWLNamedIndividual individual member inputObject]
  (.getOWLDataPropertyAssertionAxiom df
                                     ^OWLDataProperty (get member :data-property)
                                     individual
                                     (build-literal df
                                                    (invoker (get member :handle) inputObject)
                                                    (get member :owl-datatype))))

(defmulti member-matches?
          "Match the string IRI name with a member in the given set"
          (fn [member languageCode classMember]
            (:type member)))
(defmethod member-matches? ::pred/temporal
  [member languageCode classMember]
  (let [iri (.getShortForm ^IRI (get member :iri))
        position (get member :position)
        ttype (get member :temporal-type)
        name (get member :name)]
    (log/warnf "Matching against temporal %s" classMember iri)
    (condp = classMember
      ; Can we match directly against the class member?
      name true
      ; If not, try to match against a default name and the position type
      "intervalStart" (= position ::pred/start)
      "intervalEnd" (= position ::pred/end)
      "pointTime" (= position ::pred/at)
      false)))
(defmethod member-matches? ::pred/language
  [member languageCode classMember]
  (let [iri (.getShortForm ^IRI (get member :iri))]
    (log/debugf "Matching against %s with language %s" iri (get member :language)
                classMember languageCode)
    ; Match against IRI and language code (ignoring case)
    (log/spyf "Matches? %s" (and (= iri classMember) (.equalsIgnoreCase ^String languageCode (get member :language))))))
(defmethod member-matches? :default
  [member languageCode classMember]
  (log/debugf "Matching %s against defaults" classMember)
  (let [iri (.getShortForm ^IRI (get member :iri))]
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

;
; ClassParser/Builder
;

(defrecord ClojureClassParser [^OWLDataFactory df,
                               ^String reasonerPrefix,
                               ^boolean multiLangEnabled,
                               ^String defaultLanguageCode
                               classRegistry]
  IClassParser
  (isMultiLangEnabled [this] (multiLangEnabled))
  (getDefaultLanguageCode [this] (if (= defaultLanguageCode "") nil defaultLanguageCode))
  (^OWLClass getObjectClass [this ^Class clazz]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (:class-name parsedClass)))
  (^OWLClass getObjectClass [this ^Object inputObject] (.getObjectClass this (.getClass inputObject)))
  (parseClass ^Object [this ^Class clazz]
    ; Parse input class
    ;(log/debugf "Parsing %s" clazz)
    (->> (concat (.getDeclaredFields clazz) (.getDeclaredMethods clazz))
         ; Filter out non-necessary members
         (r/filter pred/filter-member)
         ; Build members
         (r/map #(build-member % df reasonerPrefix (if (true? multiLangEnabled) defaultLanguageCode nil)))
         ; Combine everything into a map
         (r/reduce member-reducer {
                                   :class-name  (get-class-name
                                                  clazz df reasonerPrefix)
                                   :constructor (build-constructor clazz)
                                   }))
    ;
    ;; Check to see if we have the class in the registry
    ;(if (contains? @classRegistry clazz)
    ;  (log/spyf :debug "Getting from cache"
    ;            ; Get the class from the registry atom
    ;            (get @classRegistry clazz))
    ;  (log/spyf :debug "Parsing test class"
    ;            ; Get the class from the newly updated atom
    ;            (get
    ;              ; Update the atom with the new class record
    ;              (swap! classRegistry
    ;                     assoc
    ;                     clazz
    ;                     ; Build the class record
    ;                     (->> (concat (.getDeclaredFields clazz) (.getDeclaredMethods clazz))
    ;                          ; Filter out non-necessary members
    ;                          (r/filter pred/filter-member)
    ;                          ; Build members
    ;                          (r/map #(build-member % df reasonerPrefix (if (true? multiLangEnabled) defaultLanguageCode nil)))
    ;                          ; Combine everything into a map
    ;                          (r/reduce member-reducer {
    ;                                                    :class-name  (get-class-name
    ;                                                                   clazz df reasonerPrefix)
    ;                                                    :constructor (build-constructor clazz)
    ;                                                    })))
    ;              clazz)))
    )

  (^OWLNamedIndividual getIndividual [this ^Object inputObject]
    (let [parsedClass (.getRegisteredClass this (.getClass inputObject))]
      (.getOWLNamedIndividual df
                              (IRI/create reasonerPrefix
                                          (m/normalize-id (invoker (get
                                                                     (get parsedClass :identifier)
                                                                     :handle)
                                                                   inputObject))))))


  (getFacts ^Optional ^List ^OWLDataPropertyAssertionAxiom [this inputObject filterSpatial]
    (let [parsedClass (.getRegisteredClass this (.getClass inputObject))
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
    (let [parsedClass (.getRegisteredClass this (.getClass inputObject))
          individual (.getIndividual this inputObject)]
      (if-let [spatial (get parsedClass :spatial)]
        (Optional/of (build-assertion-axiom df individual spatial inputObject))
        (Optional/empty))))

  (matchWithClassMember ^String [this clazz classMember]
    (.matchWithClassMember this clazz classMember nil))
  (matchWithClassMember ^String [this clazz classMember languageCode]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (log/debugf "Trying to match %s with language %s" classMember languageCode)
      ; Figure out if we directly match against a constructor argument
      ; But only if we don't have a provided language code
      (if
        (and (nil? languageCode) (ClassBuilder/isConstructorArgument clazz classMember nil))
        classMember
        (if-let
          ; Try for a classMember first, if it doesn't match, go for temporals
          [classMember (match-class-member (get parsedClass :members)
                                           languageCode classMember)]
          classMember
          ; Lookup temporals
          (match-class-member (get parsedClass :temporals)
                              languageCode classMember)))))

  (getFactDatatype ^Optional [this clazz factName]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (Optional/ofNullable (m/filter-and-get
                             (:members parsedClass)
                             (fn [member]
                               (m/filter-member-name-iri
                                 factName
                                 member))
                             :return-type))))

  (getFactIRI ^Optional [this clazz factName]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (Optional/ofNullable (m/filter-and-get
                             (:members parsedClass)
                             (fn [member]
                               (m/filter-member-name-iri
                                 factName
                                 member))
                             :iri))))

  ; IClassBuilder methods
  IClassBuilder
  (getPropertyMembers ^Optional [this clazz filterSpatial]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (Optional/ofNullable (->> (:members parsedClass)
                                (filter #(if filterSpatial
                                           (complement
                                             (= (:type %) ::pred/spatial))
                                           true))
                                (map #(.getOWLDataProperty df ^IRI (:iri %)))))))
  (getPropertyMembers ^Optional [this clazz]
    (.getPropertyMembers this clazz false))
  (constructObject ^Object [this clazz arguments]
    (let [parsedClass (.getRegisteredClass this clazz)
          constructor (:constructor parsedClass)
          parameterNames (m/get-from-array :name (:arguments constructor))
          sortedTypes (.getSortedTypes arguments ^List parameterNames)
          sortedValues (.getSortedValues arguments ^List parameterNames)
          missingParams (set/difference (set parameterNames) (.getNames arguments))]
      ; Are we missing parameters?
      (if (empty? missingParams)
        ; If missingParams is empty, we have everything we need, so build the object
        (invoke-constructor (:handle constructor) sortedValues)
        ((log/errorf "Missing constructor arguments needs %s\n%s\n%s" missingParams parameterNames sortedValues)
          (throw (MissingConstructorException. "Missing parameters required for constructor generation"))))))

  ; ClassRegister methods
  IClassRegister
  (registerClass [this clazz]
    (let [parsedClass (.parseClass this clazz)]
      (if (contains? @classRegistry clazz)
        ; Overwrite the class, if we already have it
        ((log/debugf "Already registered %s, rebuilding" (.getName clazz))
          (swap! classRegistry
                 assoc clazz parsedClass))
        ; Otherwise, just create it
        (swap! classRegistry assoc clazz parsedClass))))
  (getRegisteredClass ^Object [this ^Class clazz]
    (if-let [parsedClass (get @classRegistry clazz)]
      parsedClass
      (throw (UnregisteredClassException. clazz))))
  (deregisterClass [this clazz]
    (log/debugf "Deregistering %s" clazz)
    (swap! @classRegistry dissoc clazz)))

(defn make-parser
  "Creates a new ClassParser"
  [df reasonerPrefix multiLangEnabled defaultLanguageCode]
  (->ClojureClassParser df reasonerPrefix multiLangEnabled defaultLanguageCode (atom {})))

(ns com.nickrobison.trestle.reasoner.parser.parser
  (:import
    (com.nickrobison.trestle.reasoner.parser IClassParser IClassBuilder IClassRegister ClassBuilder ITypeConverter)
    (org.semanticweb.owlapi.model IRI OWLClass OWLDataFactory OWLNamedIndividual OWLDataPropertyAssertionAxiom OWLDataProperty OWLLiteral OWLDatatype OWLObjectPropertyAssertionAxiom)
    (java.lang.reflect Constructor Parameter)
    (com.nickrobison.trestle.reasoner.annotations DatasetClass Fact Language)
    (java.util Optional List)
    (com.nickrobison.trestle.common StaticIRI LanguageUtils)
    (com.nickrobison.trestle.reasoner.exceptions MissingConstructorException InvalidClassException InvalidClassException$State UnregisteredClassException)
    (java.io Serializable)
    )
  (:require [clojure.core.match :refer [match]]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :as log]
            [clojure.set :as set]
            [else-let.core :refer :all]
            [com.nickrobison.trestle.reasoner.parser.utils.predicates :as pred]
            [com.nickrobison.trestle.reasoner.parser.utils.members :as m]
    ; We have to require the methods we're extending, as well as namespaces where we did the extension
            [com.nickrobison.trestle.reasoner.parser.spatial :refer [wkt-from-geom]]
            [com.nickrobison.trestle.reasoner.parser.types.spatial.esri]
            [com.nickrobison.trestle.reasoner.parser.types.spatial.jts]
            [com.nickrobison.trestle.reasoner.parser.spatial :as spatial])
  (:use clj-fuzzy.metrics)
  )

; Class related helpers
(defn find-matching-constructors
  "Find constructors that match the given predicate
  If no constructors are found, returns nil"
  [^Class clazz predicate]
  (let [constructors (->> (.getDeclaredConstructors clazz)
                          (filter predicate))]
    ;If we don't have any constructors, return nil
    (if (= (count constructors) 0)
      nil
      constructors)))

(defn get-constructor
  "Get the TrestleConstructor for the provided class"
  [clazz]
  ; Look for one that's annotated with TrestleCreator
  (if-let [constructor (find-matching-constructors
                         clazz
                         pred/trestle-creator?)]
    ; If we don't have 0 or 1 constructors, throw an exception
    (if (not= (count constructor) 1)
      (throw (InvalidClassException. ^Class clazz InvalidClassException$State/EXCESS "Constructor"))
      (first constructor))
    ; If we don't have one, look for the first multi-arg constructor
    (if-let [no-arg-constructor (find-matching-constructors clazz
                                                            pred/multi-arg-constructor?)]
      ; If we don't have 0 or 1 constructors, throw an exception
      (if (not= (count no-arg-constructor) 1)
        (throw (InvalidClassException. ^Class clazz InvalidClassException$State/EXCESS "Constructor"))
        (first no-arg-constructor))
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
    ; If the constructor is not public, throw an exception
    (if (pred/public? constructor)
      {
       :handle    (m/make-handle constructor)
       :arguments (build-constructor-args constructor)
       }
      (throw (InvalidClassException. ^Class clazz "Constructor" "Must be public")))))

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
                                       clazz
                                       InvalidClassException$State/MISSING
                                       "OWL Class"))))))

(defn owl-return-type
  "Determine the OWLDatatype of the field/method"
  [member rtype ^ITypeConverter typeConverter]
  (if (pred/hasAnnotation? member Fact)
    (.getDatatypeFromAnnotation typeConverter (pred/get-annotation member Fact) rtype)
    (.getDatatypeFromJavaClass typeConverter rtype)))

(defn get-member-language
  "Get the language tag, if one exists"
  [member defaultLang]
  (if (pred/hasAnnotation? member Language)
    (let [lang (.language ^Language (pred/get-annotation member Language))]
      (if (LanguageUtils/checkLanguageCodeIsValid lang)
        lang
        (throw (InvalidClassException.
                 ; I don't really know how to get the class name here
                 (class Class)
                 InvalidClassException$State/INVALID
                 (str lang " is not a valid language code")))))

    defaultLang))

(defmulti build-member-return-type
          "Specialized method to build return type for member"
          (fn [acc _member _df _typeConverter] (:type acc)))
(defmethod build-member-return-type ::pred/spatial
  [acc member ^OWLDataFactory df _]
  (let [rtype (pred/get-member-return-type member)]
    (merge acc {
                :return-type  rtype
                :owl-datatype (.getOWLDatatype df (StaticIRI/WKTDatatypeIRI))
                })))
(defmethod build-member-return-type :default
  [acc member _ typeConverter]
  (let [rtype (pred/get-member-return-type member)]
    (merge acc {
                :return-type  rtype
                :owl-datatype (owl-return-type member rtype typeConverter)
                })))

(defn build-member-map
  [member fns]
  (reduce (fn [acc f] (f acc member)) {} fns))

(defn default-member-keys
  [acc member defaultLanguageCode]
  (let [type (pred/member-type member defaultLanguageCode)]
    (merge acc {
                :name        (m/filter-constructor-name member type)
                :member-name (pred/get-member-name member)
                :handle      (m/make-handle member)
                :related     (pred/related? member)
                :type        type
                })))

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
    (log/tracef "Called with lang type %s" rtype)
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

; Process the spatial member, if it actually is one
(defn build-spatial
  "If we're a spatial member, get the projection"
  [acc member defaultProjection]
  (let [type (:type acc)]
    (if (= type ::pred/spatial)
      (let [projection (m/get-projection member defaultProjection)]
        (merge acc {
                    :projection projection
                    :crs-uri    (spatial/projection-to-uri projection)
                    }))
      ; Return the member if we're not spatial
      acc))
  )

(defn build-member
  "Build member from class methods/fields"
  [member ^OWLDataFactory df prefix defaultLang defaultProjection typeConverter]
  (let [iri (m/build-iri member prefix)]
    (merge (build-member-map member
                             ; Apply all these transformations to build up the member map
                             [(fn [acc member]
                                (default-member-keys acc member defaultLang))
                              (fn [acc member]
                                (build-member-return-type acc member df typeConverter))
                              ignore-fact
                              (fn [acc member]
                                (build-multi-lang acc member defaultLang))
                              build-temporal
                              (fn [acc member]
                                (build-spatial acc member defaultProjection))])
           {
            :iri           iri
            :data-property (.getOWLDataProperty df iri)
            })))

(defn ^OWLLiteral build-literal
  ([^OWLDataFactory df ^String value _returnType ^String defaultLang]
   (.getOWLLiteral df value defaultLang))
  ([^OWLDataFactory df ^Object value returnType]
   (.getOWLLiteral df (.toString value) ^OWLDatatype returnType)))

; Validators
; Temporal validators
(defn validate-interval-temporal
  "Validate interval temporal to ensure it matches spec"
  [interval-temporal type members class-name]
  (if (= type ::pred/start)
    ;Validate for start temporals
    ;Ensure we have no more than 1 start temporal
    (m/ensure-temporal-count = 0 ::pred/start
                             ; if we have more than one ending temporal, throw an exception
                             (m/ensure-temporal-count <= 1 ::pred/end
                                                      interval-temporal :position members
                                                      class-name InvalidClassException$State/EXCESS "End temporal")
                             :position members class-name InvalidClassException$State/EXCESS "Start temporal")
    ; Validate for end temporals
    ; Ensure we have no more than 1 start temporal
    (m/ensure-temporal-count <= 1 ::pred/start
                             ; Ensure we have no more than 1 end temporal
                             (m/ensure-temporal-count < 1 ::pred/end
                                                      interval-temporal :position members
                                                      class-name InvalidClassException$State/EXCESS "End Temporal")
                             :position members class-name InvalidClassException$State/EXCESS "Start Temporal")))

(defn validate-point-temporal
  [point-temporal members class-name]
  ; Ensure we only have 1 at temporal (since we may not actually have any members yet, this has to be a lt operator
  (m/ensure-temporal-count = 0 ::pred/point
                           ; Ensure we don't have any interval temporals
                           (m/ensure-temporal-count = 0 ::pred/interval
                                                    point-temporal :temporal-type members class-name InvalidClassException$State/EXCESS "Start Temporal")
                           :temporal-type members class-name InvalidClassException$State/EXCESS "At Temporal"))

(defn validate-temporal
  "Ensure the temporal properties meet the required specifications"
  [member members class-name]
  (let [type (:temporal-type member)
        position (:position member)]
    (match [type]
           [::pred/interval] (validate-interval-temporal member position members class-name)
           [::pred/point] (validate-point-temporal member members class-name)
           :else (throw (IllegalStateException. (str "Don't know what to match on" type)))))
  )

(defn member-reducer
  "Build the Class Member Map"
  [acc member]
  ; Validate member
  (log/debugf "validating %s" member)
  (let [members (get acc :members)
        temporals (get acc :temporals [])
        properties (get acc :properties [])
        type (get member :type)
        ignore (get member :ignore false)]
    (match [type ignore]
           ; If we already have an identifier, throw an exception
           [::pred/identifier true] (if (contains? acc :identifier)
                                      (throw (InvalidClassException.
                                               "TestClass" InvalidClassException$State/EXCESS "Identifier"))
                                      (assoc acc :identifier member))
           [::pred/identifier false] (if (contains? acc :identifier)
                                       (throw (InvalidClassException.
                                                "TestClass" InvalidClassException$State/EXCESS "Identifier"))
                                       (merge acc {:members (conj members member) :identifier member}))
           [::pred/spatial _] (if (contains? acc :spatial)
                                (throw (InvalidClassException.
                                         "TestClass" InvalidClassException$State/EXCESS "Spatial"))
                                (merge acc {:members (conj members member) :spatial member}))
           [::pred/temporal _] (merge acc {:temporals (conj temporals (validate-temporal member temporals (:java-class acc)))})
           [::pred/property _] (merge acc {:properties (conj properties member)})
           :else (assoc acc :members (conj members member)))))

(defn build-object-assertion-axiom
  "Build OWLObjectPropertyAssertionAxiom from member"
  [parser df member individual inputObject]
  (let [relatedObject (m/invoker (get member :handle) inputObject)
        related (.getIndividual parser relatedObject)
        axiom (.getOWLObjectProperty df ^IRI (:iri member))]
    (.getOWLObjectPropertyAssertionAxiom df axiom individual related)))

(defmulti build-data-assertion-axiom
          "Build OWLDataPropertyAssertionAxiom from member"
          (fn [^OWLDataFactory _df _individual member _inputObject] (:type member)))
(defmethod build-data-assertion-axiom ::pred/spatial
  [^OWLDataFactory df ^OWLNamedIndividual individual member inputObject]
  (if-let [literal (.getOWLLiteral df (spatial/build-projected-wkt
                                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84"
                                        ;(:crs-uri member)
                                        (wkt-from-geom
                                          (m/invoker (get member :handle) inputObject)
                                          (:projection member)))
                                   (.getOWLDatatype df StaticIRI/WKTDatatypeIRI))]
    (.getOWLDataPropertyAssertionAxiom df
                                       ^OWLDataProperty (get member :data-property)
                                       individual
                                       literal)
    nil))
(defmethod build-data-assertion-axiom ::pred/language
  [^OWLDataFactory df ^OWLNamedIndividual individual member inputObject]
  (.getOWLDataPropertyAssertionAxiom df
                                     ^OWLDataProperty (get member :data-property)
                                     individual
                                     (build-literal df
                                                    (m/invoker (get member :handle) inputObject)
                                                    (get member :owl-datatype)
                                                    (get member :language))))
(defmethod build-data-assertion-axiom :default
  [^OWLDataFactory df ^OWLNamedIndividual individual member inputObject]
  (.getOWLDataPropertyAssertionAxiom df
                                     ^OWLDataProperty (get member :data-property)
                                     individual
                                     (build-literal df
                                                    (m/invoker (get member :handle) inputObject)
                                                    (get member :owl-datatype))))

(defmulti member-matches?
          "Predicate for determining if the given classmember matches the string IRI name of a member in the given set
          Specializes on the member type in order to handle special casing of temporals and multi-lang strings"
          (fn [member _languageCode _classMember]
            (:type member)))
(defmethod member-matches? ::pred/temporal
  [member _languageCode classMember]
  (let [iri (.getShortForm ^IRI (get member :iri))
        position (get member :position)
        name (get member :name)]
    (log/tracef "Matching %s against temporal %s" classMember iri)
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
    (log/tracef "Matching %s against %s with language %s" classMember iri (get member :language))
    ; Match against IRI and language code (ignoring case)
    (log/spyf "Matches? %s" (and (= iri classMember) (.equalsIgnoreCase ^String languageCode (get member :language))))))
(defmethod member-matches? :default
  [member _languageCode classMember]
  (log/tracef "Matching %s against defaults" classMember)
  (let [iri (.getShortForm ^IRI (get member :iri))]
    (= iri classMember)))


(defn match-class-member
  "Iterates through the provided member list (which can be members or temporals).
  Try to match the string IRI name with a member in the given set"
  [members languageCode classMember]
  (->> members
       (filter (fn [member]
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
                               ^Integer defaultProjection
                               ^ITypeConverter typeConverter
                               classRegistry
                               owlClassMap]
  IClassParser
  (isMultiLangEnabled [_this] (multiLangEnabled))
  (getDefaultLanguageCode [_this] (if (= defaultLanguageCode "") nil defaultLanguageCode))
  (^OWLClass getObjectClass [_this ^Class clazz]
    (get-class-name clazz df reasonerPrefix))
  (^OWLClass getObjectClass [this ^Object inputObject] (.getObjectClass this (.getClass inputObject)))
  (parseClass ^Object [_this ^Class clazz]
              ; Parse input class
    (log/debugf "Parsing %s" clazz)
              ; If the class is not public, throw an exception
    (if (pred/public? clazz)
      ; Ensure it has an Identifier
      (let [parsedClass (->> (concat (.getDeclaredFields clazz) (.getDeclaredMethods clazz))
                             ; Filter out non-necessary members
                             (r/filter pred/filter-member)
                             ; Build members
                             (r/map #(build-member % df
                                                   reasonerPrefix (if (true? multiLangEnabled)
                                                                    defaultLanguageCode nil)
                                                   defaultProjection
                                                   typeConverter))
                             ; Combine everything into a map
                             (r/reduce member-reducer {
                                                       :class-name   (get-class-name
                                                                       clazz df reasonerPrefix)
                                                       :java-class   (.getSimpleName clazz)
                                                       :constructor  (build-constructor clazz)
                                                       ;Does the class implement Serializable, and is thus cachable?
                                                       :serializable (instance? Serializable clazz)
                                                       }))]
        (if (contains? parsedClass :identifier)
          (if (contains? parsedClass :temporals)
            parsedClass
            (throw (InvalidClassException. clazz InvalidClassException$State/MISSING "Temporals")))
          (throw (InvalidClassException. clazz InvalidClassException$State/MISSING "Identifier"))))

      (throw (InvalidClassException. clazz "Class" "Must be public"))))

  (^OWLNamedIndividual getIndividual [this ^Object inputObject]
    (let [parsedClass (.getRegisteredClass this (.getClass inputObject))]
      (.getOWLNamedIndividual df
                              (IRI/create reasonerPrefix
                                          (m/normalize-id (m/invoker (get
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
                           (not= (get member :type) ::pred/spatial)
                           true)))
             ; Build the assertion axiom
             (r/map (fn [member]
                      (build-data-assertion-axiom df individual member inputObject)))
             (into ())))))
  (getFacts ^Optional ^List ^OWLDataPropertyAssertionAxiom [this inputObject]
    (.getFacts this inputObject false))

  (getSpatialFact ^Optional ^OWLDataPropertyAssertionAxiom [this inputObject]
    (let [parsedClass (.getRegisteredClass this (.getClass inputObject))
          individual (.getIndividual this inputObject)]
      (if-let [spatial (get parsedClass :spatial)]
        (Optional/of (build-data-assertion-axiom df individual spatial inputObject))
        (Optional/empty))))

  (getObjectProperties ^List ^OWLObjectPropertyAssertionAxiom [this inputObject]
    (let [parsedClass (.getRegisteredClass this (.getClass inputObject))
          individual (.getIndividual this inputObject)]
      (->> (get parsedClass :properties)
           (r/map (fn [member]
                    (build-object-assertion-axiom this df member individual inputObject)))
           (into ()))))

  (getAssociatedObjects ^List ^Object [this inputObject]
    (let [parsedClass (.getRegisteredClass this (.getClass inputObject))]
      (->> (:properties parsedClass)
           (r/map (fn [member]
                    (m/invoker (:handle member) inputObject)))
           (into ()))))

  (matchWithClassMember ^String [this clazz classMember]
    (.matchWithClassMember this clazz classMember defaultLanguageCode))
  (matchWithClassMember ^String [this clazz classMember languageCode]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (log/tracef "Trying to match %s with language %s" classMember languageCode)
      ; Figure out if we directly match against a constructor argument
      ; But only if we don't have a provided language code
      (if
        (and (nil? languageCode) (ClassBuilder/isConstructorArgument clazz classMember nil))
        classMember
        (if-let
          ; Try to match against the members lists first, if it doesn't match, go for temporals
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
      (Optional/ofNullable (m/member-field-get parsedClass (fn [member]
                                                             (m/filter-member-name-iri
                                                               factName
                                                               member)) :iri))))

  (isFactRelated [this clazz factName]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (else-let [related (m/filter-and-get
                      (:members parsedClass)
                      (fn [member]
                        (m/filter-member-name-iri
                          factName
                          member))
                      :related) false]
                related)))

  (getClassProjection ^Long [this clazz]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (if-let [projection (get-in parsedClass [:spatial :projection])]
        projection
        (Integer/valueOf 0))))

  ; IClassBuilder methods
  IClassBuilder
  (getPropertyMembers ^Optional [this clazz filterSpatial]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (Optional/ofNullable (->> (:members parsedClass)
                                (filter #(if filterSpatial
                                           (not= (:type %) ::pred/spatial)
                                           true))
                                (map #(.getOWLDataProperty df ^IRI (:iri %)))))))
  (getPropertyMembers ^Optional [this clazz]
    (.getPropertyMembers this clazz false))
  (getObjectPropertyMembers [this clazz]
    (let [parsedClass (.getRegisteredClass this clazz)]
      (->> (:properties  parsedClass)
           (map #(.getOWLObjectProperty df ^IRI (:iri %))))))
  (constructObject ^Object [this clazz arguments]
    (let [parsedClass (.getRegisteredClass this clazz)
          constructor (:constructor parsedClass)
          parameterNames (m/get-from-array :name (:arguments constructor))
          _sortedTypes (.getSortedTypes arguments ^List parameterNames)
          sortedValues (.getSortedValues arguments ^List parameterNames)
          missingParams (set/difference (set parameterNames) (.getNames arguments))]
      ; Are we missing parameters?
      (if (empty? missingParams)
        ; If missingParams is empty, we have everything we need, so build the object
        (m/invoke-constructor (:handle constructor) sortedValues)
        ((log/errorf "Missing constructor arguments needs %s\n%s\n%s" missingParams parameterNames sortedValues)
         (throw (MissingConstructorException. "Missing parameters required for constructor generation"))))))
  (getProjectedWKT ^OWLLiteral [this clazz spatialObject srid]
    (let [parsedClass (.getRegisteredClass this clazz)]
      ; If the SRID is nil, use the one from the class definition
      (if (nil? srid)
        (.getOWLLiteral df
                        (spatial/object-to-projected-wkt
                          spatialObject
                          (get-in parsedClass [:spatial :projection]))
                        (.getOWLDatatype df StaticIRI/WKTDatatypeIRI))
        (.getOWLLiteral df
                        (spatial/object-to-projected-wkt
                          spatialObject
                          srid)
                        (.getOWLDatatype df StaticIRI/WKTDatatypeIRI)))))

  ; ClassRegister methods
  IClassRegister
  (registerClass [this owlClass clazz]
    (let [parsedClass (.parseClass this clazz)]
      (if (contains? @classRegistry clazz)
        ; Overwrite the class, if we already have it
        ((log/debugf "Already registered %s, rebuilding" (.getName clazz))
         (swap! classRegistry
                assoc clazz parsedClass)
         (swap! owlClassMap assoc owlClass clazz))
        ; Otherwise, just create it
        ((swap! classRegistry assoc clazz parsedClass)
         (swap! owlClassMap assoc owlClass clazz)))))
  (getRegisteredClass ^Object [_this ^Class clazz]
    (if-let [parsedClass (get @classRegistry clazz)]
      parsedClass
      (throw (UnregisteredClassException. clazz))))
  (getRegisteredOWLClasses [_this]
    (set (keys @owlClassMap)))
  (deregisterClass [_this clazz]
    (log/debugf "Deregistering %s" clazz)
    (swap! classRegistry dissoc clazz)
    (swap! owlClassMap dissoc clazz))
  (isCacheable [_this clazz]
    (if-let [parsedClass (get @classRegistry clazz)]
      (:serializable parsedClass)
      (throw (UnregisteredClassException. clazz))))
  (isRegistered [_this clazz]
    (contains? @classRegistry clazz))
  (lookupClass [_this owlClass]
    (if-let [clazz (get @owlClassMap owlClass)]
      clazz
      (throw (UnregisteredClassException. owlClass)))))


(defn make-parser
  "Creates a new ClassParser"
  [df reasonerPrefix multiLangEnabled defaultLanguageCode defaultProjection typeConverter]
  (->ClojureClassParser df reasonerPrefix
                        multiLangEnabled
                        defaultLanguageCode defaultProjection
                        typeConverter
                        (atom {})
                        (atom {})))

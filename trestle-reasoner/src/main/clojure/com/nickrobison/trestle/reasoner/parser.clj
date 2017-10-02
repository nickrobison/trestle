(ns com.nickrobison.trestle.reasoner.parser
  (:import [IClassParser]
           (com.nickrobison.trestle.reasoner.parser IClassParser)))

(defrecord ClojureClassParser []
  IClassParser
  (sayHello [this name] (println (str "Hello, " name)))
  (addSomething [this x y] (+ x (+ y 3))))
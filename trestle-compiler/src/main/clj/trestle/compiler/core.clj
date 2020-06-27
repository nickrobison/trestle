(ns trestle.compiler.core)

(defn hello [name]
  (str "hello" " " name))

(defn printer [f & args]
  (print (f args)))
(ns com.nickrobison.trestle.reasoner.debug.repl
  (:use [clojure.tools.nrepl.server :only (start-server stop-server)])
  (:import (com.nickrobison.trestle.reasoner.debug IDebugREPL)))

(defrecord ClojureDebugREPL
  [state]
  IDebugREPL
  (start
    [this]
    (let [port (:server-port @state)]
      (swap! state assoc :server (start-server :port port))
      )
    )

  (stop
    [this]
    (stop-server (:server @state))

    ))

(defn make-clojure-repl
  [port]
  (->ClojureDebugREPL
    (atom {
           :server-port port
           })))
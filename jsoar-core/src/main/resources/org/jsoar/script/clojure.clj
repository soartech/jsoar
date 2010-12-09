(ns org.jsoar.script.clojure
  (:import (org.jsoar.util.events SoarEventListener)
           (org.jsoar.kernel.events InputEvent OutputEvent BeforeInitSoarEvent)
           (org.jsoar.kernel.symbols Symbols)
           (org.jsoar.kernel.io InputWmes)))

; user/_soar is set in org.jsoar.script.ScriptEngineState
(def context user/_soar)

(def events (.. context getAgent getEvents))
(def printer (.. context getAgent getPrinter))

(def output-handlers (ref {}))
(def disposers (ref []))

(defn echo [s] (.. printer (print (str s))))

(defn- event-listener 
  [f]
  (reify SoarEventListener 
    (onEvent [this e] (f e))))

(defn on-event 
  "Register function f to listen for events of a particular class.
   f will be called passed an event object as its only argument.
   Returns a function which when called, unregisters the event
   handler."
  [class f]
  (let [handler (event-listener f)]
    (do
      (.addListener events class handler)
      #(.removeListener events class handler))))

(defn for-one-event
  "Register a handler that is automatically unregistered after the first time
   it is called, e.g.
  
      (for-one-event on-input (fn [e] ...))
  "
  [reg f]
  (let [cleanup (ref nil) 
        wrapper (fn [e] (try (f e) (finally (@cleanup))))]
    (dosync (ref-set cleanup (reg wrapper)))))

(defn on-init-soar
  "Register a function for org.jsoar.kernel.events.BeforeInitSoarEvent"
  [f]
  (on-event BeforeInitSoarEvent f))

(defn on-input
  "Register a function for org.jsoar.kernel.events.InputEvent."
  [f]
  (on-event InputEvent f))

(defn on-output
  "Register a function for org.jsoar.kernel.events.OutputEvent."
  [f]
  (on-event OutputEvent f))

(defn on-output-command
  [command f]
  (dosync
    (alter output-handlers assoc command f)
    #(dosync (alter output-handlers dissoc command f))))

(defn on-dispose
  "Register a function to be called when the script engine is disposed"
  [f]
  (dosync
    (alter disposers conj f)))

(declare to-clojure) ; forward decl

(defn- to-clojure-reducer
  "to-clojure helper. acc is the result accumulator and wme is a Wme."
  [acc wme]
  (let [attr  (str (.getAttribute wme))
        value (.getValue wme)
        id    (.asIdentifier value)]
    (assoc acc attr 
      (if id
        (to-clojure id)
        (Symbols/valueOf value)))))

(defn to-clojure
  ([root acc]
    (let [wmes (iterator-seq (.getWmes root))]
      (reduce to-clojure-reducer acc wmes)))
  ([root] (to-clojure root {})))


(defn dispose
  "Called by soar_dispose when the engine is disposed. Calls all registered
   disposal functions"
  []
  (doseq [f @disposers] (f)))

; Register the output command handler and make sure it's removed when the
; script engine is disposed.
(on-dispose
  (on-output 
    (fn [e] 
      (let [io (.getInputOutput e)]
        (doseq [c (.. e getInputOutput getPendingCommands)]
          (when-let [f (@output-handlers (str (.getAttribute c)))]
            (InputWmes/add io (.getValue c) "status" (or (f) "complete"))))))))



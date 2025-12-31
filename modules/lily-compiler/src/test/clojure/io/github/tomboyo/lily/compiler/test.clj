(ns io.github.tomboyo.lily.compiler.test
  (:import (io.github.tomboyo.lily.compiler CompilerSupport)))

(def ^:dynamic *state*)

(def serial (ref 0))

(defn package-name
  "Get a unique package name"
  [] (dosync (str "gen" (alter serial inc))))

(defn generate [spec]
  "Generates source code from the spec into a unique package."
  (dosync
    (let [p (:package @*state*)
          d (:dirty @*state*)]
      (when d (throw (IllegalStateException. "Attempted to generate code in the same scope more than once")))
      (CompilerSupport/compileOas p spec)
      (alter *state* assoc :dirty true))))

(defn resolve-name [s]
  "Returns a symbol formed by resolving the relative name s against the
  generated source code package name."
  (symbol (str (:package @*state*) "." s)))

(defn fixture [f]
  "Ensures code is generated into unique packages and cleaned up per-test."
  (let [p (package-name)]
    (binding [*state* (ref {:package p})]
      (try
        (f)
        (finally (CompilerSupport/clearPackageFiles p))))))

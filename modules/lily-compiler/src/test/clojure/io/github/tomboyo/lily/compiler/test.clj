(ns io.github.tomboyo.lily.compiler.test
  (:require [clojure.string :as str])
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

(defn relative-class-name
  "Returns the class name of x relative to the generated source code package. If
  the generated package is GEN and x is an instance of GEN.foo.Bar, the result
  is foo.Bar with no leading dot."
  ([x]
   (str/replace-first (.getName (class x))
                      (re-pattern (str "^" (:package @*state*) "."))
                      "")))

(defn fixture [f]
  "Ensures code is generated into unique packages and cleaned up per-test."
  (let [p (package-name)]
    (binding [*state* (ref {:package p})]
      (try
        (f)
        (finally (CompilerSupport/clearPackageFiles p))))))
